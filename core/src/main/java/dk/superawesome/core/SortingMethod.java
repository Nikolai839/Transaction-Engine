package dk.superawesome.core;

public enum SortingMethod {
    BY_TIME,
    BY_AMOUNT
    ;

    public record Linked<N extends TransactionNode, T extends TransactionNode>(SortingMethod method, PostQueryTransformer<N, T> transformer) {

    }
}
