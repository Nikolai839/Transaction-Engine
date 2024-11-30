package dk.superawesome.core;

import dk.superawesome.core.transaction.SingleTransactionNode;
import dk.superawesome.core.transaction.SortingMethod;
import dk.superawesome.core.transaction.TransactionNode;

import java.time.ZonedDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BinaryOperator;
import java.util.function.Function;

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
        public static <V extends SortVisitor<N>, N extends Node> V getVisitor(Node.Collection collection) {
            return switch (collection) {
                case SINGLE -> (V) new SingleTransactionNode.Visitor();
                case GROUPED -> (V) new TransactionNode.GroupedTransactionNode.Visitor();
            };

        }

        public static <N extends Node, T> SortBy<N> sortBy(Function<N, T> func, Comparator<T> comparator) {
            return new SortBy<>((o1, o2) -> comparator.compare(func.apply(o1), func.apply(o2)));
        }

        public static <GN extends GroupedNode<N>, N extends Node, T> SortBy<GN> sortByGroup(Function<GN, Collection<N>> nodes, Function<N, T> func, Comparator<T> comparator) {
            return new SortBy<>((o1, o2) -> comparator.compare(
                    func.apply(nodes.apply(o1).stream().max((n1, n2) -> comparator.compare(func.apply(n1), func.apply(n2))).orElse(null)),
                    func.apply(nodes.apply(o2).stream().max((n1, n2) -> comparator.compare(func.apply(n1), func.apply(n2))).orElse(null))
            ));
        }

        public static <GN extends GroupedNode<N>, N extends Node, T> SortBy<GN> sortByGroup(Function<GN, Collection<N>> nodes, Function<N, T> func, BinaryOperator<T> collector, Comparator<T> comparator) {
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

            default SortBy<N> sortByGroupedAmount() {
                return null;
            }
        }

        public interface SortVisitable<N extends Node, V extends SortVisitor<N>> {

            PostQueryTransformer<N, N> visit(SortingMethod method, V visitor);
        }
    }

    class GroupBy<N extends Node, GN extends GroupedNode<N>, T> implements PostQueryTransformer<N, GN> {

        public interface GroupOperator<N extends Node> {

            static <N extends Node> GroupOperator<N> mix(List<GroupOperator<N>> operators) {
                if (operators.isEmpty()) {
                    return (__, ___) -> true;
                }

                if (operators.size() == 1) {
                    return operators.get(0);
                }

                return (nodes, node) -> {
                    for (GroupOperator<N> operator : operators) {
                        if (!operator.checkGroup(nodes, node)) {
                            return false;
                        }
                    }

                    return false;
                };
            }

            static <N extends Node> GroupOperator<N> max(int max) {
                return (group, node) -> group.size() < max;
            }

            static <N extends Node> GroupOperator<N> maxBetween(Function<N, ZonedDateTime> func, int amount, TimeUnit unit) {
                return new GroupOperator<>() {

                    @Override
                    public void sort(List<N> group) {
                        group.sort(Comparator.comparing(func, ChronoZonedDateTime::compareTo));
                    }

                    @Override
                    public boolean checkGroup(List<N> group, N node) {
                        if (group.isEmpty()) {
                            return true;
                        }

                        long diff = Math.abs(func.apply(group.get(0)).toEpochSecond() - func.apply(node).toEpochSecond());
                        return diff < unit.toSeconds(amount);
                    }
                };
            }

            default void sort(List<N> group) {

            }

            boolean checkGroup(List<N> group, N node);
        }

        public interface GroupCollector<N extends Node, GN extends GroupedNode<N>, T> {

            GN collect(Collection<N> nodes);

            T getKey(N node);
        }

        public static <N extends Node, GN extends GroupedNode<N>, T> GroupBy<N, GN, T> groupBy(GroupCollector<N, GN, T> collector) {
            return groupBy(null, collector);
        }

        public static <N extends Node, GN extends GroupedNode<N>, T> GroupBy<N, GN, T> groupBy(GroupOperator<N> operator, GroupCollector<N, GN, T> collector) {
            return new GroupBy<>(operator, collector);
        }

        private final GroupOperator<N> operator;
        private final GroupCollector<N, GN, T> collector;

        public GroupBy(GroupOperator<N> operator, GroupCollector<N, GN, T> collector) {
            this.operator = operator;
            this.collector = collector;
        }

        @Override
        public List<GN> transform(List<N> nodes) {
            Map<T, List<N>> groups = new HashMap<>();

            for (N node : nodes) {
                T key = this.collector.getKey(node);
                if (!groups.containsKey(key)) {
                    groups.put(key, new LinkedList<>());
                }

                Collection<N> group = groups.get(key);
                group.add(node);
            }

            if (this.operator != null) {
                Collection<List<N>> subGroupsOverview = new LinkedList<>();
                for (List<N> group : groups.values()) {
                    this.operator.sort(group);

                    List<N> subGroup = new LinkedList<>();
                    subGroupsOverview.add(subGroup);
                    for (N node : group) {
                        if (!this.operator.checkGroup(subGroup, node)) {
                            subGroup = new LinkedList<>();
                            subGroupsOverview.add(subGroup);
                        }

                        subGroup.add(node);
                    }
                }

                return subGroupsOverview.stream()
                        .map(this.collector::collect)
                        .toList();
            }

            return groups.values().stream()
                    .map(this.collector::collect)
                    .toList();
        }
    }
}
