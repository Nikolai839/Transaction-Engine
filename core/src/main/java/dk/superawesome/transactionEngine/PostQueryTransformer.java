package dk.superawesome.transactionEngine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
import java.util.function.Function;

public interface PostQueryTransformer<N> {

    void transform(List<N> nodes);

    class SortBy<N extends Node> implements PostQueryTransformer<N> {

        private final Comparator<N> comparator;

        public SortBy(Comparator<N> comparator) {
            this.comparator = comparator;
        }

        @Override
        public void transform(List<N> nodes) {
            nodes.sort(this.comparator);
        }
    }

    class GroupBy<N extends Node> implements PostQueryTransformer<N> {

        public interface GroupOperator<N extends Node> {

            static <N extends Node> GroupOperator<N> max(int limit) {
                return (l, __) -> l.size() <= limit;
            }

            static <N extends Node.Timed> GroupOperator<N> maxBetween(int amount, TimeUnit unit) {
                return (l, n) -> {
                    List<N> testGroup = new ArrayList<>(l);
                    testGroup.add(n);

                    List<Date> postTest = testGroup.stream().map(Node.Timed::time).sorted(Date::compareTo).toList();
                    long diff = postTest.get(postTest.size() - 1).getTime() - postTest.get(0).getTime();

                    return diff <= unit.toMillis(amount);
                };
            }

            boolean checkGroup(List<N> nodes, N node);
        }

        public static <N extends Node> GroupBy<N> groupBy(Function<N, Object> func, BiPredicate<Object, Object> groupBy) {
            return groupBy(func, func, groupBy, null);
        }

        public static <N extends Node> GroupBy<N> groupBy(Function<N, Object> func, BiPredicate<Object, Object> groupBy, GroupOperator<N> operator) {
            return groupBy(func, func, groupBy, operator);
        }

        public static <N extends Node> GroupBy<N> groupBy(Function<N, Object> func1, Function<N, Object> func2, BiPredicate<Object, Object> groupBy) {
            return groupBy(func1, func2, groupBy, null);
        }

        public static <N extends Node> GroupBy<N> groupBy(Function<N, Object> func1, Function<N, Object> func2, BiPredicate<Object, Object> groupBy, GroupOperator<N> operator) {
            return new GroupBy<>((n1, n2) -> groupBy.test(func1.apply(n1), func2.apply(n2)), operator);
        }

        private final BiPredicate<N, N> groupBy;
        private final GroupOperator<N> operator;

        public GroupBy(BiPredicate<N, N> groupBy, GroupOperator<N> operator) {
            this.groupBy = groupBy;
            this.operator = operator;
        }

        @Override
        public void transform(List<N> nodes) {
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
        }
    }
}
