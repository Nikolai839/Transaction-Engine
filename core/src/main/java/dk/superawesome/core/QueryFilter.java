package dk.superawesome.core;

import dk.superawesome.core.transaction.SingleTransactionNode;
import dk.superawesome.core.transaction.TransactionNode;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public interface QueryFilter<N extends Node> extends Predicate<N> {

    interface Operator<N extends Node> {

        static <N extends Node> Operator<N> and() {
            return filters -> n -> filters.stream().allMatch(f -> f.test(n));
        }

        static <N extends Node> Operator<N> or() {
            return filters -> n -> filters.stream().anyMatch(f -> f.test(n));
        }

        Predicate<N> test(List<QueryFilter<? super N>> filters);
    }

    boolean test(N input);

    record FilterData<N extends Node>(FilterType<?, ? super N> type, QueryFilter<? super N> filter) {

    }

    FilterType<ZonedDateTime, SingleTransactionNode> TIME = new FilterType<>("time", SingleTransactionNode::time);
    FilterType<Double, SingleTransactionNode> AMOUNT = new FilterType<>("amount", SingleTransactionNode::amount);
    FilterType<TransactionNode.PayType, SingleTransactionNode> TYPE = new FilterType<>("type", SingleTransactionNode::type);
    FilterType<String, SingleTransactionNode> FROM_USER = new FilterType<>("from_user", SingleTransactionNode::fromUserName);
    FilterType<String, SingleTransactionNode> TO_USER = new FilterType<>("to_user", SingleTransactionNode::toUserName);

    class FilterType<T, N extends Node> implements Identifiable<String> {

        private final String identifier;
        private final Function<N, T> converter;

        public FilterType(String identifier, Function<N, T> converter) {
            this.identifier = identifier;
            this.converter = converter;
        }

        public QueryFilter<N> makeFilter(Predicate<T> filter) {
            return input -> filter.test(converter.apply(input));
        }

        @Override
        public String getIdentifier() {
            return this.identifier;
        }

        @Override
        public boolean equals(Object b) {
            return (b instanceof FilterType<?, ?> type) && type.getIdentifier().equals(this.identifier);
        }
    }
}
