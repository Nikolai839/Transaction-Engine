package dk.superawesome.core;

public interface NodeComparator<N extends Node, V extends PostQueryTransformer.SortBy.SortVisitor<N>> {

    PostQueryTransformer<N, N> visit(SortingMethod method, V visitor);
}
