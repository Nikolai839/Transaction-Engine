package dk.superawesome.core;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;

public interface QueryFilter<N extends Node> {

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
