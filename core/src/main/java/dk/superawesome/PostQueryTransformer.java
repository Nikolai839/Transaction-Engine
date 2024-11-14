package dk.superawesome;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;

public interface PostQueryTransformer<N, T> {

    T transform(List<N> nodes);

    class SortBy<N extends Node> implements PostQueryTransformer<N, List<N>> {

        public static <N extends Node, T> SortBy<N> sortBy(Function<N, T> func, Comparator<T> comparator) {
            return new SortBy<>((o1, o2) -> comparator.compare(func.apply(o1), func.apply(o2)));
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
    }

    class GroupBy<N extends Node, GN extends GroupedNode> implements PostQueryTransformer<N, List<GN>> {

        public interface GroupOperator<N extends Node> {

            static <N extends Node> GroupOperator<N> max(int limit) {
                return (l, __) -> l.size() <= limit;
            }

            static <N extends Node> GroupOperator<N> maxBetween(Function<N, Date> timeSupplier,  int amount, TimeUnit unit) {
                return (l, n) -> {
                    List<N> testGroup = new ArrayList<>(l);
                    testGroup.add(n);

                    List<Date> postTest = testGroup.stream().map(timeSupplier).sorted(Date::compareTo).toList();
                    long diff = postTest.get(postTest.size() - 1).getTime() - postTest.get(0).getTime();

                    return diff <= unit.toMillis(amount);
                };
            }

            boolean checkGroup(List<N> nodes, N node);
        }

        public interface GroupCollector<N extends Node, GN extends GroupedNode> {

            GN collect(List<N> nodes);
        }

        public static <N extends Node, GN extends GroupedNode> GroupBy<N, GN> groupBy(Function<N, Object> func, BiPredicate<Object, Object> groupBy, GroupCollector<N, GN> collector) {
            return groupBy(func, func, groupBy, null, collector);
        }

        public static <N extends Node, GN extends GroupedNode> GroupBy<N, GN> groupBy(Function<N, Object> func, BiPredicate<Object, Object> groupBy, GroupOperator<N> operator, GroupCollector<N, GN> collector) {
            return groupBy(func, func, groupBy, operator, collector);
        }

        public static <N extends Node, GN extends GroupedNode> GroupBy<N, GN> groupBy(Function<N, Object> func1, Function<N, Object> func2, BiPredicate<Object, Object> groupBy, GroupCollector<N, GN> collector) {
            return groupBy(func1, func2, groupBy, null, collector);
        }

        public static <N extends Node, GN extends GroupedNode> GroupBy<N, GN> groupBy(Function<N, Object> func1, Function<N, Object> func2, BiPredicate<Object, Object> groupBy, GroupOperator<N> operator, GroupCollector<N, GN> collector) {
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
