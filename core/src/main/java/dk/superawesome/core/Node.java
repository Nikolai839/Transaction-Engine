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
        SINGLE(SingleNodeComparator::new), GROUPED(GroupedNodeComparator::new);

        private final Supplier<NodeComparator<? extends Node, ? extends PostQueryTransformer.SortBy.SortVisitor<? extends Node>>> comparatorSupplier;

        Collection(Supplier<NodeComparator<? extends Node, ? extends PostQueryTransformer.SortBy.SortVisitor<? extends Node>>> comparatorSupplier) {
            this.comparatorSupplier = comparatorSupplier;
        }

        @SuppressWarnings("unchecked")
        public <N extends Node, V extends PostQueryTransformer.SortBy.SortVisitor<N>> NodeComparator<N, V> getComparator() {
            return (NodeComparator<N, V>) this.comparatorSupplier.get();
        }

        static abstract private class RegistryNodeComparator<N extends Node, V extends PostQueryTransformer.SortBy.SortVisitor<N>> implements NodeComparator<N, V> {

            private final EnumMap<SortingMethod, Function<V, PostQueryTransformer<N, N>>> registry;

            private RegistryNodeComparator(EnumMap<SortingMethod, Function<V, PostQueryTransformer<N, N>>> registry) {
                this.registry = registry;
            }

            @Override
            public PostQueryTransformer<N, N> visit(SortingMethod method, V visitor) {
                return Optional.ofNullable(registry.get(method)).map(f -> f.apply(visitor)).orElse(PostQueryTransformer.NO_ACTION());
            }
        }

        static class SingleNodeComparator extends RegistryNodeComparator<SingleTransactionNode, SingleTransactionNode.Visitor> {

            private SingleNodeComparator() {
                super(SingleTransactionNode.Visitor.SORTINGS);
            }
        }

        static class GroupedNodeComparator extends RegistryNodeComparator<TransactionNode.GroupedTransactionNode, TransactionNode.GroupedTransactionNode.Visitor> {

            private GroupedNodeComparator() {
                super(TransactionNode.GroupedTransactionNode.Visitor.SORTINGS);
            }
        }
    }
}
