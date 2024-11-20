package dk.superawesome.core;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

public interface PostQueryTransformer<N extends Node, T extends Node> {

    static <N extends Node> PostQueryTransformer<N, N> NO_ACTION() {
        return nodes -> nodes;
    }

    List<T> transform(List<N> nodes);

    static <N extends Node> PostQueryTransformer<N, N> reversed() {
        return nodes -> {
            Collections.reverse(nodes);
            return nodes;
        };
    }

    class SortBy<N extends Node> implements PostQueryTransformer<N, N> {

        @SuppressWarnings("unchecked")
        public static <N extends Node> SortVisitor<N> getVisitor(Node.Collection collection) {
            return switch (collection) {
                case SINGLE -> (SortVisitor<N>) new SingleTransactionNode.Visitor();
                case GROUPED -> (SortVisitor<N>) new TransactionNode.GroupedTransactionNode.Visitor();
            };

        }

        public static <N extends Node, T> SortBy<N> sortBy(Function<N, T> func, Comparator<T> comparator) {
            return new SortBy<>((o1, o2) -> comparator.compare(func.apply(o1), func.apply(o2)));
        }

        public static <GN extends GroupedNode<N>, N extends Node, T> SortBy<GN> sortByGroup(Function<GN, List<N>> nodes, Function<N, T> func, Comparator<T> comparator) {
            return new SortBy<>((o1, o2) -> comparator.compare(
                    func.apply(nodes.apply(o1).stream().max((n1, n2) -> comparator.compare(func.apply(n1), func.apply(n2))).orElse(null)),
                    func.apply(nodes.apply(o2).stream().max((n1, n2) -> comparator.compare(func.apply(n1), func.apply(n2))).orElse(null))
            ));
        }

        public static <GN extends GroupedNode<N>, N extends Node, T> SortBy<GN> sortByGroup(Function<GN, List<N>> nodes, Function<N, T> func, BinaryOperator<T> collector, Comparator<T> comparator) {
            return new SortBy<>((o1, o2) -> comparator.compare(
                    nodes.apply(o1).stream().map(func).reduce(collector).orElse(null),
                    nodes.apply(o2).stream().map(func).reduce(collector).orElse(null)
            ));
        }

        private final Comparator<N> comparator;

        public SortBy(Comparator<N> comparator) {
            this.comparator = comparator;
        }

        @Override
        public List<N> transform(List<N> nodes) {
            nodes.sort(this.comparator);
            return nodes;
        }

        public interface SortVisitor<N extends Node> {

            SortBy<N> sortByTime();

            SortBy<N> sortByAmount();
        }

        public interface SortVisitable<N extends Node, V extends SortVisitor<N>> {

            PostQueryTransformer<N, N> visit(SortingMethod method, V visitor);
        }
    }

    class GroupBy<N extends Node, GN extends GroupedNode<N>, T> implements PostQueryTransformer<N, GN> {

        public interface GroupOperator<N extends Node> {

            static <N extends Node> GroupOperator<N> mix(List<GroupOperator<N>> operators) {
                if (operators.isEmpty()) {
                    return (nodes, node) -> true;
                }

                if (operators.size() == 1) {
                    return operators.get(0);
                }

                return (nodes, node) -> {
                    for (GroupOperator<N> operator : operators) {
                        if (operator.checkGroup(nodes, node)) {
                            return false;
                        }
                    }

                    return true;
                };
            }

            static <N extends Node> GroupOperator<N> max(int limit) {
                return (l, __) -> l.size() <= limit;
            }

            static <N extends Node> GroupOperator<N> maxBetween(Function<N, ZonedDateTime> timeSupplier, int amount, TimeUnit unit) {
                return new GroupOperator<>() {

                    final long limit = unit.toSeconds(amount);
                    boolean canExpand = true;
                    ZonedDateTime max;
                    ZonedDateTime min;

                    @Override
                    public boolean checkGroup(List<N> nodes, N node) {
                        ZonedDateTime time = timeSupplier.apply(node);
                        ZonedDateTime prevMax = max;
                        if (max == null || canExpand && time.isAfter(max)) {
                            max = time;
                        }
                        ZonedDateTime prevMin = min;
                        if (min == null || canExpand && time.isBefore(min)) {
                            min = time;
                        }

                        long diff = Math.abs(max.toEpochSecond() - min.toEpochSecond());
                        if (diff > limit) {
                            if (max == prevMax && min == prevMin) {
                                canExpand = false;
                            } else if (prevMax != null && prevMin != null) {
                                long diffRe = Math.abs(prevMax.toEpochSecond() - prevMin.toEpochSecond());
                                if (diffRe > limit) {
                                    canExpand = false;
                                }
                            }

                            max = prevMax;
                            min = prevMin;
                            return true;
                        }

                        return false;
                    }
                };
            }

            boolean checkGroup(List<N> nodes, N node);
        }

        public interface GroupCollector<N extends Node, GN extends GroupedNode<N>, T> {

            GN collect(List<N> nodes);

            T getKey(N node);
        }

        public static <N extends Node, GN extends GroupedNode<N>, T> GroupBy<N, GN, T> groupBy(Function<N, T> func, BiPredicate<T, T> groupBy, GroupCollector<N, GN, T> collector) {
            return groupBy(func, func, groupBy, null, collector);
        }

        public static <N extends Node, GN extends GroupedNode<N>, T> GroupBy<N, GN, T> groupBy(Function<N, T> func, BiPredicate<T, T> groupBy, Supplier<GroupOperator<N>> operatorSupplier, GroupCollector<N, GN, T> collector) {
            return groupBy(func, func, groupBy, operatorSupplier, collector);
        }

        public static <N extends Node, GN extends GroupedNode<N>, T> GroupBy<N, GN, T> groupBy(Function<N, T> func1, Function<N, T> func2, BiPredicate<T, T> groupBy, GroupCollector<N, GN, T> collector) {
            return groupBy(func1, func2, groupBy, null, collector);
        }

        public static <N extends Node, GN extends GroupedNode<N>, T> GroupBy<N, GN, T> groupBy(Function<N, T> func1, Function<N, T> func2, BiPredicate<T, T> groupBy, Supplier<GroupOperator<N>> operatorSupplier, GroupCollector<N, GN, T> collector) {
            return new GroupBy<>((n1, n2) -> groupBy.test(func1.apply(n1), func2.apply(n2)), operatorSupplier, collector);
        }

        private final BiPredicate<N, N> groupBy;
        private final Supplier<GroupOperator<N>> operatorSupplier;
        private final GroupCollector<N, GN, T> collector;

        public GroupBy(BiPredicate<N, N> groupBy, Supplier<GroupOperator<N>> operatorSupplier, GroupCollector<N, GN, T> collector) {
            this.groupBy = groupBy;
            this.operatorSupplier = operatorSupplier;
            this.collector = collector;
        }

        record SubGroup<N extends Node>(List<N> nodes, GroupOperator<N> operator) {

        }

        @Override
        public List<GN> transform(List<N> nodes) {
            Map<T, List<SubGroup<N>>> groups = new HashMap<>();

            for (N node : nodes) {
                T key = this.collector.getKey(node);
                if (!groups.containsKey(key)) {
                    groups.put(key, new ArrayList<>());
                }

                groupIteration: {
                    List<SubGroup<N>> subGroups = groups.get(key);
                    for (SubGroup<N> subGroup : subGroups) {
                        if (!subGroup.nodes().isEmpty() && !this.groupBy.test(subGroup.nodes().get(0), node)) {
                            continue;
                        }

                        if (subGroup.operator() != null && subGroup.operator().checkGroup(subGroup.nodes(), node)) {
                            continue;
                        }

                        subGroup.nodes().add(node);
                        break groupIteration;
                    }

                    // no group found
                    List<N> newGroup = new ArrayList<>();
                    newGroup.add(node);

                    GroupOperator<N> operator = null;
                    if (this.operatorSupplier != null) {
                        operator = this.operatorSupplier.get();
                    }

                    subGroups.add(new SubGroup<>(newGroup, operator));
                }
            }

            return groups.values().stream()
                    .flatMap(List::stream)
                    .map(SubGroup::nodes)
                    .map(this.collector::collect)
                    .toList();
        }
    }
}
