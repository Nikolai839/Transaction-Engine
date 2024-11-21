package dk.superawesome.core;

import dk.superawesome.core.util.LazyInit;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
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
        }

        public interface SortVisitable<N extends Node, V extends SortVisitor<N>> {

            PostQueryTransformer<N, N> visit(SortingMethod method, V visitor);
        }
    }

    class GroupBy<N extends Node, GN extends GroupedNode<N>, T, R> implements PostQueryTransformer<N, GN> {

        public interface GroupOperator<N extends Node, T> {

            @SuppressWarnings("unchecked")
            static <N extends Node, T> GroupOperator<N, T> mix(List<GroupOperator<N, ?>> operators, NodeGroupContext.ContextFactory<N, T> contextFactory) {
                if (operators.isEmpty()) {
                    return (__, ___) -> false;
                }

                if (operators.size() == 1) {
                    return (GroupOperator<N, T>) operators.get(0);
                }

                return (nodes, context) -> {
                    for (GroupOperator<N, ?> operator : operators) {
                        if (((GroupOperator<N, T>)operator).checkGroup(nodes, contextFactory.makeContext(context.node()))) {
                            return false;
                        }
                    }

                    return true;
                };
            }

            static <N extends Node, T> GroupOperator<N, T> max(int limit) {
                return (l, __) -> l.size() <= limit;
            }

            static <N extends Node> GroupOperator<N, Long> maxBetween(Function<N, ZonedDateTime> timeSupplier, int amount, TimeUnit unit) {
                return new GroupOperator<>() {

                    final long limit = unit.toSeconds(amount);
                    long max = -1;
                    long min = -1;

                    @Override
                    public boolean checkGroup(Collection<N> nodes, NodeGroupContext<N, Long> context) {
                        long time = context.reference().getOr(() -> timeSupplier.apply(context.node()).toEpochSecond());
                        if (this.max == -1 || time > this.max) {
                            this.max = time;
                        }
                        if (this.min == -1 || time < this.min) {
                            this.min = time;
                        }

                        return Math.abs(this.max - this.min) > limit;
                    }
                };
            }

            boolean checkGroup(Collection<N> nodes, NodeGroupContext<N, T> node);
        }

        public record NodeGroupContext<N extends Node, T>(N node, LazyInit<T> reference) {

            public interface ContextFactory<N extends Node, T> {

                NodeGroupContext<N, T> makeContext(N node);

                static <N extends Node, T, F> ContextFactory<N, T> of(BiFunction<N, Map<F, NodeGroupContext<N, T>>, NodeGroupContext<N, T>> get) {
                    return new ContextFactory<>() {

                        final Map<F, NodeGroupContext<N, T>> data = new HashMap<>();

                        @Override
                        public NodeGroupContext<N, T> makeContext(N node) {
                            return get.apply(node, data);
                        }
                    };
                }

                static <N extends Node, T, F> ContextFactory<N, T> simple(Function<N, F> func) {
                    return of((n, m) -> m.computeIfAbsent(func.apply(n), __ -> new NodeGroupContext<>(n, new LazyInit<>())));
                }
            }
        }

        public interface GroupCollector<N extends Node, GN extends GroupedNode<N>, T> {

            GN collect(Collection<N> nodes);

            T getKey(N node);
        }

        public static <N extends Node, GN extends GroupedNode<N>, T, F> GroupBy<N, GN, T, F> groupBy(GroupCollector<N, GN, T> collector) {
            return groupBy(null, collector);
        }

        public static <N extends Node, GN extends GroupedNode<N>, T, F> GroupBy<N, GN, T, F> groupBy(Supplier<GroupOperator<N, F>> operatorSupplier, GroupCollector<N, GN, T> collector) {
            return new GroupBy<>(operatorSupplier, collector);
        }

        private final Supplier<GroupOperator<N, R>> operatorSupplier;
        private final GroupCollector<N, GN, T> collector;

        public GroupBy(Supplier<GroupOperator<N, R>> operatorSupplier, GroupCollector<N, GN, T> collector) {
            this.operatorSupplier = operatorSupplier;
            this.collector = collector;
        }

        record SubGroup<N extends Node, T>(Collection<N> nodes, GroupOperator<N, T> operator) {

        }

        @Override
        public List<GN> transform(List<N> nodes) {
            Map<T, Collection<SubGroup<N, R>>> groups = new HashMap<>();

            System.out.println("Start group transformation");
            long start = System.currentTimeMillis();
            for (N node : nodes) {
                T key = this.collector.getKey(node);
                if (!groups.containsKey(key)) {
                    groups.put(key, new LinkedList<>());
                }

                groupIteration: {
                    Collection<SubGroup<N, R>> subGroups = groups.get(key);
                    NodeGroupContext<N, R> context = new NodeGroupContext<>(node, new LazyInit<>());
                    for (SubGroup<N, R> subGroup : subGroups) {
                        if (subGroup.operator() != null && subGroup.operator().checkGroup(subGroup.nodes(), context)) {
                            continue;
                        }

                        subGroup.nodes().add(node);
                        break groupIteration;
                    }

                    // no group found
                    Collection<N> newGroup = new ArrayList<>();
                    newGroup.add(node);

                    GroupOperator<N, R> operator = null;
                    if (this.operatorSupplier != null) {
                        operator = this.operatorSupplier.get();
                    }

                    subGroups.add(new SubGroup<>(newGroup, operator));
                }
            }

            System.out.println("Took " + (System.currentTimeMillis() - start) + " " + groups.size());

            return groups.values().stream()
                    .flatMap(Collection::stream)
                    .map(SubGroup::nodes)
                    .map(this.collector::collect)
                    .toList();
        }
    }
}
