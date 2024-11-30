package dk.superawesome.core.transaction;

import dk.superawesome.core.PostQueryTransformer;

public enum SortingMethod {
    BY_TIME("tidspunkt", false),
    BY_AMOUNT("beløb", false),
    GROUPED_AMOUNT("grupperet antal", true)
    ;

    private final String name;
    private final boolean isGrouped;

    SortingMethod(String name, boolean isGrouped) {
        this.name = name;
        this.isGrouped = isGrouped;
    }

    public String getName() {
        return this.name;
    }

    public boolean isGrouped() {
        return this.isGrouped;
    }

    public record Linked<N extends TransactionNode, T extends TransactionNode>(SortingMethod method, PostQueryTransformer<N, T> transformer) {

    }
}
