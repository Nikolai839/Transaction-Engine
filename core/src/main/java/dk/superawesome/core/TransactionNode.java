package dk.superawesome.core;

import java.time.chrono.ChronoZonedDateTime;
import java.util.*;
import java.util.function.Function;

public interface TransactionNode extends Node {

    enum PayType {
        PAY, CHESTSHOP, SERVERSTORE, AFGIFT, GIVE, TAKE, AREASHOP, SERVERMARKET
    }

    record GroupedTransactionNode(java.util.Collection<SingleTransactionNode> nodes, Bound bound) implements TransactionNode, GroupedNode<SingleTransactionNode> {

        public enum Bound {
            FROM, TO
        }

        @Override
        public java.util.Collection<SingleTransactionNode> getNodes() {
            return this.nodes;
        }

        public double getAmount() {
            return this.nodes.stream().map(SingleTransactionNode::amount).reduce(Double::sum).orElse(0d);
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
