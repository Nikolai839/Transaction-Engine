package dk.superawesome.core;

import dk.superawesome.core.transaction.SingleTransactionNode;
import dk.superawesome.core.transaction.SortingMethod;
import dk.superawesome.core.transaction.TransactionNode;

import java.util.EnumMap;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public interface Node {

    default Collection getCollection() {
        return Collection.SINGLE;
    }

    interface Linked<TO extends Node> extends Node {

        TO node();
    }

    enum Collection {
        SINGLE(SingleVisitable::new, false),
        GROUPED(GroupedVisitable::new, true),
        GROUP_GROUPED(GroupGroupedVisitable::new, true);

        private final boolean isGroup;

        private final Supplier<PostQueryTransformer.SortBy.SortVisitable<? extends Node, ? extends PostQueryTransformer.SortBy.SortVisitor<? extends Node>>> comparatorSupplier;

        Collection(Supplier<PostQueryTransformer.SortBy.SortVisitable<? extends Node, ? extends PostQueryTransformer.SortBy.SortVisitor<? extends Node>>> comparatorSupplier, boolean isGroup) {
            this.comparatorSupplier = comparatorSupplier;
            this.isGroup = isGroup;
        }

        @SuppressWarnings("unchecked")
        public <N extends Node, V extends PostQueryTransformer.SortBy.SortVisitor<N>> PostQueryTransformer.SortBy.SortVisitable<N, V> getVisitable() {
            return (PostQueryTransformer.SortBy.SortVisitable<N, V>) this.comparatorSupplier.get();
        }

        public boolean isGroup() {
            return this.isGroup;
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

        static class GroupGroupedVisitable extends RegistryNodeVisitable<TransactionNode.GroupedBothWayTransactionNode, TransactionNode.GroupedBothWayTransactionNode.Visitor> {

            private GroupGroupedVisitable() {
                super(TransactionNode.GroupedBothWayTransactionNode.Visitor.SORTINGS);
            }
        }
    }
}
