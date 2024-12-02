package dk.superawesome.core.transaction;

import dk.superawesome.core.Node;
import dk.superawesome.core.PostQueryTransformer;

import java.time.ZonedDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.util.EnumMap;
import java.util.function.Function;

public interface SingleTransactionNode extends TransactionNode, Node.Linked<SingleTransactionNode>, Comparable<SingleTransactionNode> {

    record Target(GroupedTransactionNode.Bound bound, SingleTransactionNode node) implements TransactionNode, Node.Linked<SingleTransactionNode> {

        @Override
        public boolean isTraced() {
            return false;
        }
    }

    default SingleTransactionNode node() {
        return this;
    }

    default int compareTo(SingleTransactionNode node) {
        return time().compareTo(node.time());
    }

    ZonedDateTime time();

    double amount();

    String fromUserName();

    String toUserName();

    double fromUserPreBalance();

    double toUserPreBalance();

    PayType type();

    String extra();

    record Unit(ZonedDateTime time, double amount, String fromUserName, String toUserName, double fromUserPreBalance, double toUserPreBalance, TransactionNode.PayType type, String extra) implements SingleTransactionNode {

        @Override
        public boolean isTraced() {
            return false;
        }
    }

    record Traced(SingleTransactionNode unit, double fromUserTrace, double toUserTrace) implements SingleTransactionNode {

        @Override
        public boolean isTraced() {
            return true;
        }

        @Override
        public ZonedDateTime time() {
            return this.unit.time();
        }

        @Override
        public double amount() {
            return this.unit.amount();
        }

        @Override
        public String fromUserName() {
            return this.unit.fromUserName();
        }

        @Override
        public String toUserName() {
            return this.unit.toUserName();
        }

        @Override
        public double fromUserPreBalance() {
            return this.unit.fromUserPreBalance();
        }

        @Override
        public double toUserPreBalance() {
            return this.unit.toUserPreBalance();
        }

        @Override
        public PayType type() {
            return this.unit.type();
        }

        @Override
        public String extra() {
            return this.unit.extra();
        }
    }

    class Visitor implements PostQueryTransformer.SortBy.SortVisitor<SingleTransactionNode> {

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
