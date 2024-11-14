package dk.superawesome;

import java.util.Date;
import java.util.List;

public record SimpleTransactionNode(Date time, double amount, String fromUserName, String toUserName) implements TransactionNode {

    public static final TransactionGroupCollector COLLECTOR = new TransactionGroupCollector();

    public static class TransactionGroupCollector implements PostQueryTransformer.GroupBy.GroupCollector<SimpleTransactionNode, GroupedTransactionNode> {

        @Override
        public GroupedTransactionNode collect(List<SimpleTransactionNode> nodes) {
            return new GroupedTransactionNode(nodes);
        }
    }

    public record GroupedTransactionNode(List<SimpleTransactionNode> nodes) implements TransactionNode, GroupedNode {

    }
}
