package dk.superawesome.core;

import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Function;

public interface TransactionNode extends Node {

    record GroupedTransactionNode(List<SingleTransactionNode> nodes, Bound bound) implements TransactionNode, GroupedNode<SingleTransactionNode> {

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
            }

            @Override
            public PostQueryTransformer.SortBy<GroupedTransactionNode> sortByTime() {
                return PostQueryTransformer.SortBy.sortByGroup(GroupedTransactionNode::nodes, SingleTransactionNode::time, Date::compareTo);
            }
        }
    }
}
