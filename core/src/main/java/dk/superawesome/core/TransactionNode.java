package dk.superawesome.core;

import java.time.ZonedDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

public interface TransactionNode extends Node {

    ZonedDateTime getMinTime();

    record GroupedTransactionNode(List<SingleTransactionNode> nodes, Bound bound) implements TransactionNode, GroupedNode<SingleTransactionNode> {

        @Override
        public ZonedDateTime getMinTime() {
            return this.nodes.stream().map(SingleTransactionNode::time).min(ChronoZonedDateTime::compareTo).orElseThrow();
        }

        public enum Bound {
            FROM, TO
        }

        @Override
        public List<SingleTransactionNode> getNodes() {
            return this.nodes;
        }

        public static class Visitor implements PostQueryTransformer.SortBy.SortVisitor<GroupedTransactionNode> {

            public static final EnumMap<SortingMethod, Function<Visitor, PostQueryTransformer<GroupedTransactionNode, GroupedTransactionNode>>> SORTINGS = new EnumMap<>(SortingMethod.class);
            static {
                SORTINGS.put(SortingMethod.BY_TIME, Visitor::sortByTime);
                SORTINGS.put(SortingMethod.BY_AMOUNT, Visitor::sortByAmount);
            }

            @Override
            public PostQueryTransformer.SortBy<GroupedTransactionNode> sortByTime() {
                return PostQueryTransformer.SortBy.sortByGroup(GroupedTransactionNode::nodes, SingleTransactionNode::time, ChronoZonedDateTime::compareTo);
            }

            @Override
            public PostQueryTransformer.SortBy<GroupedTransactionNode> sortByAmount() {
                return PostQueryTransformer.SortBy.sortByGroup(GroupedTransactionNode::nodes, SingleTransactionNode::amount, Double::sum, Double::compareTo);
            }
        }
    }
}
