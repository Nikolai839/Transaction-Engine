package dk.superawesome.core;

import java.time.ZonedDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.util.EnumMap;
import java.util.function.Function;

public record SingleTransactionNode(ZonedDateTime time, double amount, String fromUserName, String toUserName, double fromUserPreBalance, double toUserPreBalance, TransactionNode.PayType type, String extra) implements TransactionNode {

    public static class Visitor implements PostQueryTransformer.SortBy.SortVisitor<SingleTransactionNode> {

        public static final EnumMap<SortingMethod, Function<Visitor, PostQueryTransformer<SingleTransactionNode, SingleTransactionNode>>> SORTINGS = new EnumMap<>(SortingMethod.class);
        static {
            SORTINGS.put(SortingMethod.BY_TIME, Visitor::sortByTime);
            SORTINGS.put(SortingMethod.BY_AMOUNT, Visitor::sortByAmount);
        }

        @Override
        public PostQueryTransformer.SortBy<SingleTransactionNode> sortByTime() {
            return PostQueryTransformer.SortBy.sortBy(SingleTransactionNode::time, ChronoZonedDateTime::compareTo);
        }

        @Override
        public PostQueryTransformer.SortBy<SingleTransactionNode> sortByAmount() {
            return PostQueryTransformer.SortBy.sortBy(SingleTransactionNode::amount, Double::compareTo);
        }
    }
}
