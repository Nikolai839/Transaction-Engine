package dk.superawesome.core;

import java.time.ZonedDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;

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
    }

    class GroupBy<N extends Node, GN extends GroupedNode<N>> implements PostQueryTransformer<N, GN> {

        public interface GroupOperator<N extends Node> {

            static <N extends Node> GroupOperator<N> mix(List<GroupOperator<N>> operators) {
                return (nodes, node) -> operators.stream().allMatch(o -> o.checkGroup(nodes, node));
            }

            static <N extends Node> GroupOperator<N> max(int limit) {
                return (l, __) -> l.size() <= limit;
            }

            static <N extends Node> GroupOperator<N> maxBetween(Function<N, ZonedDateTime> timeSupplier, int amount, TimeUnit unit) {
                return (l, n) -> {
                    List<N> testGroup = new ArrayList<>(l);
                    testGroup.add(n);

                    List<ZonedDateTime> postTest = testGroup.stream().map(timeSupplier).sorted(ChronoZonedDateTime::compareTo).toList();
                    long diff = postTest.get(postTest.size() - 1).toInstant().toEpochMilli() - postTest.get(0).toInstant().toEpochMilli();

                    return diff <= unit.toMillis(amount);
                };
            }

            boolean checkGroup(List<N> nodes, N node);
        }

        public interface GroupCollector<N extends Node, GN extends GroupedNode<N>> {

            GN collect(List<N> nodes);
        }

        public static <N extends Node, GN extends GroupedNode<N>> GroupBy<N, GN> groupBy(Function<N, Object> func, BiPredicate<Object, Object> groupBy, GroupCollector<N, GN> collector) {
            return groupBy(func, func, groupBy, null, collector);
        }

        public static <N extends Node, GN extends GroupedNode<N>> GroupBy<N, GN> groupBy(Function<N, Object> func, BiPredicate<Object, Object> groupBy, GroupOperator<N> operator, GroupCollector<N, GN> collector) {
            return groupBy(func, func, groupBy, operator, collector);
        }

        public static <N extends Node, GN extends GroupedNode<N>> GroupBy<N, GN> groupBy(Function<N, Object> func1, Function<N, Object> func2, BiPredicate<Object, Object> groupBy, GroupCollector<N, GN> collector) {
            return groupBy(func1, func2, groupBy, null, collector);
        }

        public static <N extends Node, GN extends GroupedNode<N>> GroupBy<N, GN> groupBy(Function<N, Object> func1, Function<N, Object> func2, BiPredicate<Object, Object> groupBy, GroupOperator<N> operator, GroupCollector<N, GN> collector) {
            return new GroupBy<>((n1, n2) -> groupBy.test(func1.apply(n1), func2.apply(n2)), operator, collector);
        }

        private final BiPredicate<N, N> groupBy;
        private final GroupOperator<N> operator;
        private final GroupCollector<N, GN> collector;

        public GroupBy(BiPredicate<N, N> groupBy, GroupOperator<N> operator, GroupCollector<N, GN> collector) {
            this.groupBy = groupBy;
            this.operator = operator;
            this.collector = collector;
        }

        @Override
        public List<GN> transform(List<N> nodes) {
            List<List<N>> groups = new ArrayList<>();
            for (N node : nodes) {
                check_node: {
                    check_group: {
                        for (List<N> group : groups) {
                            if (this.operator != null && !this.operator.checkGroup(group, node)) {
                                continue;
                            }

                            for (N n : group) {
                                if (!this.groupBy.test(node, n)) {
                                    break check_group;
                                }
                            }
                            group.add(node);
                            break check_node;
                        }
                    }
                    List<N> newGroup = new ArrayList<>();
                    newGroup.add(node);
                    groups.add(newGroup);
                }
            }

            List<GN> newNodes = new ArrayList<>();
            for (List<N> group : groups) {
                newNodes.add(this.collector.collect(group));
            }
            return newNodes;
        }
    }
}
