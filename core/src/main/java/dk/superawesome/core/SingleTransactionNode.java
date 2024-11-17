package dk.superawesome.core;

import java.util.Date;
import java.util.EnumMap;
import java.util.function.Function;

public record SingleTransactionNode(Date time, double amount, String fromUserName, String toUserName) implements TransactionNode {

    public static class Visitor implements PostQueryTransformer.SortBy.SortVisitor<SingleTransactionNode> {

        public static final EnumMap<SortingMethod, Function<Visitor, PostQueryTransformer<SingleTransactionNode, SingleTransactionNode>>> SORTINGS = new EnumMap<>(SortingMethod.class);
        static {
            SORTINGS.put(SortingMethod.BY_TIME, Visitor::sortByTime);
        }

        @Override
        public PostQueryTransformer.SortBy<SingleTransactionNode> sortByTime() {
            return PostQueryTransformer.SortBy.sortBy(SingleTransactionNode::time, Date::compareTo);
        }
    }
}
