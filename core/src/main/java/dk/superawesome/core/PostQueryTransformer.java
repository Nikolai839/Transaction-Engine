package dk.superawesome.core;

import dk.superawesome.core.transaction.SingleTransactionNode;
import dk.superawesome.core.transaction.SortingMethod;
import dk.superawesome.core.transaction.TransactionNode;

import java.time.ZonedDateTime;
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
                case GROUP_GROUPED -> (V) new TransactionNode.GroupedBothWayTransactionNode.Visitor();
            };

        }

        public static <N extends Node, T> SortBy<N> sortBy(Function<N, T> func, Comparator<T> comparator) {
            return new SortBy<>((o1, o2) -> comparator.compare(func.apply(o1), func.apply(o2)));
        }

        public static <GN extends GroupedNode<?>, N extends Node, T> SortBy<GN> sortByGroup(Function<GN, Collection<N>> nodes, Function<N, T> func, Comparator<T> comparator) {
            return new SortBy<>((o1, o2) -> comparator.compare(
                    func.apply(nodes.apply(o1).stream().max((n1, n2) -> comparator.compare(func.apply(n1), func.apply(n2))).orElse(null)),
                    func.apply(nodes.apply(o2).stream().max((n1, n2) -> comparator.compare(func.apply(n1), func.apply(n2))).orElse(null))
            ));
        }

        public static <GN extends GroupedNode<?>, N extends Node, T> SortBy<GN> sortByGroup(Function<GN, Collection<N>> nodes, Function<N, T> func, BinaryOperator<T> collector, Comparator<T> comparator) {
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

            default SortBy<N> sortBySum() {
                return null;
            }
        }

        public interface SortVisitable<N extends Node, V extends SortVisitor<N>> {

            PostQueryTransformer<N, N> visit(SortingMethod method, V visitor);
        }
    }

    class GroupBy<N extends Node, TN extends Node.Linked<N>, GN extends GroupedNode<?>, KEY> implements PostQueryTransformer<N, GN> {

        public interface GroupOperator<TN extends Node.Linked<N>, N extends Node> {

            static <TN extends Node.Linked<N>, N extends Node> GroupOperator<TN, N> mix(List<GroupOperator<TN, N>> operators) {
                if (operators.isEmpty()) {
                    return (__, ___) -> true;
                }

                if (operators.size() == 1) {
                    return operators.get(0);
                }

                return (nodes, node) -> {
                    for (GroupOperator<TN, N> operator : operators) {
                        if (!operator.checkGroup(nodes, node)) {
                            return false;
                        }
                    }

                    return false;
                };
            }

            static <TN extends Node.Linked<N>, N extends Node> GroupOperator<TN, N> max(int max) {
                return (group, node) -> group.size() < max;
            }

            static <TN extends Node.Linked<N>, N extends Node> GroupOperator<TN, N> maxBetween(Function<N, ZonedDateTime> func, int amount, TimeUnit unit) {
                return (group, node) -> {
                    if (group.isEmpty()) {
                        return true;
                    }

                    long diff = Math.abs(func.apply(group.get(0).node()).toEpochSecond() - func.apply(node).toEpochSecond());
                    return diff < unit.toSeconds(amount);
                };
            }

            boolean checkGroup(List<TN> group, N node);
        }

        public interface GroupCollector<N extends Node, TN extends Node, GN extends GroupedNode<?>, KEY> {

            default void insert(KEY key, TN node, Map<KEY, List<TN>> groups) {
                if (!groups.containsKey(key)) {
                    groups.put(key, new LinkedList<>());
                }

                Collection<TN> group = groups.get(key);
                group.add(node);
            }

            GN collect(Collection<TN> nodes);

            void applyToGroup(N node, Map<KEY, List<TN>> groups);

            interface Direct<N extends Node, GN extends GroupedNode<?>, KEY> extends GroupCollector<N, N, GN, KEY> {

                default void applyToGroup(N node, Map<KEY, List<N>> groups) {
                    KEY key = getKey(node);
                    insert(key, node, groups);
                }

                KEY getKey(N node);
            }
        }

        public static <N extends Node, TN extends Node.Linked<N>, GN extends GroupedNode<?>, KEY> GroupBy<N, TN, GN, KEY> groupBy(GroupCollector<N, TN, GN, KEY> collector) {
            return groupBy(null, null, collector);
        }

        public static <N extends Node, TN extends Node.Linked<N>, GN extends GroupedNode<?>, KEY> GroupBy<N, TN, GN, KEY> groupBy(GroupOperator<TN, N> operator, GroupCollector<N, TN, GN, KEY> collector) {
            return groupBy(null, Node.Linked::node, collector);
        }

        public static <N extends Node, TN extends Node.Linked<N>, GN extends GroupedNode<?>, KEY> GroupBy<N, TN, GN, KEY> groupBy(GroupOperator<TN, N> operator, Function<TN, N> back, GroupCollector<N, TN, GN, KEY> collector) {
            return new GroupBy<>(operator, back, collector);
        }

        private final Function<TN, N> back;
        private final GroupOperator<TN, N> operator;
        private final GroupCollector<N, TN, GN, KEY> collector;

        public GroupBy(GroupOperator<TN, N> operator, Function<TN, N> back, GroupCollector<N, TN, GN, KEY> collector) {
            this.operator = operator;
            this.back = back;
            this.collector = collector;
        }

        @Override
        public List<GN> transform(List<N> nodes) {
            Map<KEY, List<TN>> groups = new HashMap<>();

            for (N node : nodes) {
                this.collector.applyToGroup(node, groups);
            }

            if (this.operator != null) {
                Collection<List<TN>> subGroupsOverview = new LinkedList<>();
                for (List<TN> group : groups.values()) {
                    List<TN> subGroup = new LinkedList<>();
                    subGroupsOverview.add(subGroup);
                    for (TN node : group) {
                        if (!this.operator.checkGroup(subGroup, back.apply(node))) {
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
