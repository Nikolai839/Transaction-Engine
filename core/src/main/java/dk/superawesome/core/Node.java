package dk.superawesome.core;

import java.util.EnumMap;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public interface Node {

    default boolean isGrouped() {
        return false;
    }

    enum Collection {
        SINGLE(SingleVisitable::new), GROUPED(GroupedVisitable::new);

        public static Collection from(Node node) {
            if (node.isGrouped()) {
                return GROUPED;
            } else {
                return SINGLE;
            }
        }

        private final Supplier<PostQueryTransformer.SortBy.SortVisitable<? extends Node, ? extends PostQueryTransformer.SortBy.SortVisitor<? extends Node>>> comparatorSupplier;

        Collection(Supplier<PostQueryTransformer.SortBy.SortVisitable<? extends Node, ? extends PostQueryTransformer.SortBy.SortVisitor<? extends Node>>> comparatorSupplier) {
            this.comparatorSupplier = comparatorSupplier;
        }

        @SuppressWarnings("unchecked")
        public <N extends Node, V extends PostQueryTransformer.SortBy.SortVisitor<N>> PostQueryTransformer.SortBy.SortVisitable<N, V> getVisitable() {
            return (PostQueryTransformer.SortBy.SortVisitable<N, V>) this.comparatorSupplier.get();
        }

        static abstract private class RegistryNodeVisitable<N extends Node, V extends PostQueryTransformer.SortBy.SortVisitor<N>> implements PostQueryTransformer.SortBy.SortVisitable<N, V> {

            private final EnumMap<SortingMethod, Function<V, PostQueryTransformer<N, N>>> registry;

            private RegistryNodeVisitable(EnumMap<SortingMethod, Function<V, PostQueryTransformer<N, N>>> registry) {
                this.registry = registry;
            }

            @Override
            public PostQueryTransformer<N, N> visit(SortingMethod method, V visitor) {
                return Optional.ofNullable(registry.get(method)).map(f -> f.apply(visitor)).orElse(PostQueryTransformer.NO_ACTION());
            }
        }

        static class SingleVisitable extends RegistryNodeVisitable<SingleTransactionNode, SingleTransactionNode.Visitor> {

            private SingleVisitable() {
                super(SingleTransactionNode.Visitor.SORTINGS);
            }
        }

        static class GroupedVisitable extends RegistryNodeVisitable<TransactionNode.GroupedTransactionNode, TransactionNode.GroupedTransactionNode.Visitor> {

            private GroupedVisitable() {
                super(TransactionNode.GroupedTransactionNode.Visitor.SORTINGS);
            }
        }
    }
}
