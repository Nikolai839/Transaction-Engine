package dk.superawesome.core;

public enum SortingMethod {
    BY_TIME("tidspunkt"),
    BY_AMOUNT("beløb")
    ;

    private final String name;

    SortingMethod(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public record Linked<N extends TransactionNode, T extends TransactionNode>(SortingMethod method, PostQueryTransformer<N, T> transformer) {

    }
}
