package dk.superawesome.core.transaction;

import dk.superawesome.core.Node;
import dk.superawesome.core.PostQueryTransformer;

import java.util.function.Predicate;

public enum SortingMethod {
    BY_TIME("tidspunkt", Holder.ALL),
    BY_AMOUNT("beløb", Holder.ALL),
    BY_SUM("grupperet beløb sum", Predicate.isEqual(Node.Collection.GROUP_GROUPED)),
    GROUPED_AMOUNT("grupperet antal", Node.Collection::isGroup)
    ;

    private static class Holder {
        private static final Predicate<Node.Collection> ALL = t -> true;
    }

    private final String name;
    private final Predicate<Node.Collection> match;

    SortingMethod(String name, Predicate<Node.Collection> match) {
        this.name = name;
        this.match = match;
    }

    public String getName() {
        return this.name;
    }

    public boolean match(Node.Collection collection) {
        return this.match.test(collection);
    }

    public record Linked<N extends TransactionNode, T extends TransactionNode>(SortingMethod method, PostQueryTransformer<N, T> transformer) {

    }
}
