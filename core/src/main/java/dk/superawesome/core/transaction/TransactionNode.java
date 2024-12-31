package dk.superawesome.core.transaction;

import dk.superawesome.core.GroupedNode;
import dk.superawesome.core.Node;
import dk.superawesome.core.PostQueryTransformer;

import java.time.chrono.ChronoZonedDateTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public interface TransactionNode extends Node {

    String CONSOLE = "CONSOLE";

    enum PayType {
        PAY, CHESTSHOP, SERVERSTORE, AFGIFT, GIVE, TAKE, AREASHOP, SERVERMARKET, MECHANICS
    }

    boolean isTraced();

    record GroupedBothWayTransactionNode(String username, java.util.Collection<SingleTransactionNode.Target> from,
                                         java.util.Collection<SingleTransactionNode.Target> to,
                                         boolean traced) implements TransactionNode, GroupedNode<SingleTransactionNode> {

        @Override
        public Collection getCollection() {
            return Collection.GROUP_GROUPED;
        }

        @Override
        public boolean isTraced() {
            return traced;
        }

        @Override
        public java.util.Collection<SingleTransactionNode> nodes() {
            return combine().stream()
                    .map(Node.Linked::node)
                    .toList();
        }

        public List<SingleTransactionNode.Target> combine() {
            return Stream.concat(from.stream(), to.stream()).toList();
        }

        public double getAmount() {
            return nodes().stream()
                    .mapToDouble(SingleTransactionNode::amount)
                    .sum();
        }

        public double getSum() {
            return combine().stream()
                    .mapToDouble(GroupedBothWayTransactionNode::evaluate)
                    .sum();
        }

        public Optional<SingleTransactionNode> getHighestTransaction(GroupedTransactionNode.Bound bound) {
            return combine().stream()
                    .filter(t -> t.bound().equals(bound))
                    .map(Node.Linked::node)
                    .max(Comparator.comparingDouble(SingleTransactionNode::amount));
        }

        public Optional<SingleTransactionNode.Target> getLatestTransaction() {
            return combine().stream().max(Comparator.comparing(d -> d.node().time()));
        }

        public Optional<SingleTransactionNode.Target> getOldestTransaction() {
            return combine().stream().min(Comparator.comparing(d -> d.node().time()));
        }

        public Optional<SingleTransactionNode> getOldestTransaction(GroupedTransactionNode.Bound bound) {
            return combine().stream()
                    .filter(t -> t.bound().equals(bound))
                    .map(Node.Linked::node)
                    .min(Comparator.comparing(SingleTransactionNode::time));
        }

        public static double evaluate(SingleTransactionNode.Target target) {
            double amount = target.node().amount();
            if (target.bound() == GroupedTransactionNode.Bound.FROM) {
                amount = amount * -1;
            }
            return amount;
        }

        public static class Visitor implements PostQueryTransformer.SortBy.SortVisitor<GroupedBothWayTransactionNode> {

            public static final EnumMap<SortingMethod, Function<Visitor, PostQueryTransformer<GroupedBothWayTransactionNode, GroupedBothWayTransactionNode>>> SORTINGS = new EnumMap<>(SortingMethod.class);

            static {
                SORTINGS.put(SortingMethod.BY_TIME, Visitor::sortByTime);
                SORTINGS.put(SortingMethod.BY_AMOUNT, Visitor::sortByAmount);
                SORTINGS.put(SortingMethod.BY_SUM, Visitor::sortBySum);
                SORTINGS.put(SortingMethod.GROUPED_AMOUNT, Visitor::sortByGroupedAmount);
            }

            @Override
            public PostQueryTransformer.SortBy<GroupedBothWayTransactionNode> sortByTime() {
                return PostQueryTransformer.SortBy.sortByGroup(GroupedBothWayTransactionNode::nodes, SingleTransactionNode::time, ChronoZonedDateTime::compareTo);
            }

            @Override
            public PostQueryTransformer.SortBy<GroupedBothWayTransactionNode> sortByGroupedAmount() {
                return PostQueryTransformer.SortBy.sortBy(group -> group.combine().size(), Integer::compare);
            }

            @Override
            public PostQueryTransformer.SortBy<GroupedBothWayTransactionNode> sortByAmount() {
                return PostQueryTransformer.SortBy.sortBy(GroupedBothWayTransactionNode::getAmount, Double::compareTo);
            }

            @Override
            public PostQueryTransformer.SortBy<GroupedBothWayTransactionNode> sortBySum() {
                return PostQueryTransformer.SortBy.sortBy(GroupedBothWayTransactionNode::getSum, Double::compareTo);
            }
        }
    }

    record GroupedTransactionNode(java.util.Collection<SingleTransactionNode> nodes, Bound bound,
                                  boolean traced) implements TransactionNode, GroupedNode<SingleTransactionNode> {

        public enum Bound {
            FROM, TO
        }

        @Override
        public boolean isTraced() {
            return traced;
        }

        public double getAmount() {
            return this.nodes.stream()
                    .map(SingleTransactionNode::amount)
                    .reduce(Double::sum)
                    .orElse(0d);
        }

        public Optional<SingleTransactionNode> getHighestTransaction() {
            return this.nodes.stream().max(Comparator.comparingDouble(SingleTransactionNode::amount));
        }

        public Optional<SingleTransactionNode> getOldestTransaction() {
            return this.nodes.stream().min(Comparator.comparing(SingleTransactionNode::time));
        }

        public Optional<SingleTransactionNode> getLatestTransaction() {
            return this.nodes.stream().max(Comparator.comparing(SingleTransactionNode::time));
        }

        public static class Visitor implements PostQueryTransformer.SortBy.SortVisitor<GroupedTransactionNode> {

            public static final EnumMap<SortingMethod, Function<Visitor, PostQueryTransformer<GroupedTransactionNode, GroupedTransactionNode>>> SORTINGS = new EnumMap<>(SortingMethod.class);

            static {
                SORTINGS.put(SortingMethod.BY_TIME, Visitor::sortByTime);
                SORTINGS.put(SortingMethod.BY_AMOUNT, Visitor::sortByAmount);
                SORTINGS.put(SortingMethod.GROUPED_AMOUNT, Visitor::sortByGroupedAmount);
            }

            @Override
            public PostQueryTransformer.SortBy<GroupedTransactionNode> sortByTime() {
                return PostQueryTransformer.SortBy.sortByGroup(GroupedTransactionNode::nodes, SingleTransactionNode::time, ChronoZonedDateTime::compareTo);
            }

            @Override
            public PostQueryTransformer.SortBy<GroupedTransactionNode> sortByAmount() {
                return PostQueryTransformer.SortBy.sortByGroup(GroupedTransactionNode::nodes, SingleTransactionNode::amount, Double::sum, Double::compareTo);
            }

            @Override
            public PostQueryTransformer.SortBy<GroupedTransactionNode> sortByGroupedAmount() {
                return PostQueryTransformer.SortBy.sortBy(GroupedNode::size, Integer::compare);
            }
        }
    }
}
