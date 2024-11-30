package dk.superawesome.core.transaction;

import dk.superawesome.core.*;
import dk.superawesome.core.db.DatabaseExecutor;
import dk.superawesome.core.db.DatabaseSettings;
import dk.superawesome.core.db.Requester;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class TransactionRequestBuilder<B extends EngineRequest.Builder<SingleTransactionNode, RESULT>, RESULT> {

    public static TransactionRequestBuilder<EngineRequest.QueryWrapperBuilder<SingleTransactionNode>, EngineQuery<SingleTransactionNode>> wrap(EngineQuery<SingleTransactionNode> query) {
        return new TransactionRequestBuilder<>(new EngineRequest.QueryWrapperBuilder<>(query));
    }

    public static TransactionRequestBuilder<EngineRequest.RequestBuilder<SingleTransactionNode>, EngineRequest<SingleTransactionNode>> builder(EngineCache<SingleTransactionNode> cache, DatabaseSettings settings, DatabaseExecutor<SingleTransactionNode> executor, Requester requester) {
        return new TransactionRequestBuilder<>(new EngineRequest.RequestBuilder<>(cache, settings, executor, requester));
    }

    private final B builder;

    public TransactionRequestBuilder(B builder) {
        this.builder = builder;
    }

    public TransactionRequestBuilder<B, RESULT> from(ZonedDateTime date) {
        builder.addFilter(QueryFilter.TIME, QueryFilter.TIME.makeFilter(d -> d.isAfter(date)));
        return this;
    }

    public TransactionRequestBuilder<B, RESULT> to(ZonedDateTime date) {
        builder.addFilter(QueryFilter.TIME, QueryFilter.TIME.makeFilter(d -> d.isBefore(date)));
        return this;
    }

    public TransactionRequestBuilder<B, RESULT> range(ZonedDateTime from, ZonedDateTime to) {
        from(to.isAfter(from) ? from : to);
        to(to.isAfter(from) ? to : from);
        return this;
    }

    public TransactionRequestBuilder<B, RESULT> range(double from, double to) {
        builder.addFilter(QueryFilter.AMOUNT, QueryFilter.AMOUNT.makeFilter(d -> d > Math.min(from, to) && d < Math.max(from, to)));
        return this;
    }

    public TransactionRequestBuilder<B, RESULT> from(double from) {
        return range(from, Double.MAX_VALUE);
    }

    public TransactionRequestBuilder<B, RESULT> to(double to) {
        return range(0, to);
    }

    private TransactionRequestBuilder<B, RESULT> forPlayers(QueryFilter.FilterType<String, SingleTransactionNode> filter, String... names) {
        if (names.length > 0) {
            List<String> lowerCasedNames = Arrays.stream(names).map(String::toLowerCase).toList();
            builder.addFilter(filter, filter.makeFilter(n -> lowerCasedNames.contains(n.toLowerCase())));
        }

        return this;
    }

    public TransactionRequestBuilder<B, RESULT> from(String... names) {
        return forPlayers(QueryFilter.FROM_USER, names);
    }

    public TransactionRequestBuilder<B, RESULT> to(String... names) {
        return forPlayers(QueryFilter.TO_USER, names);
    }

    public TransactionRequestBuilder<B, RESULT> is(TransactionNode.PayType... types) {
        List<TransactionNode.PayType> typesList = Arrays.stream(types).toList();
        builder.addFilter(QueryFilter.TYPE, QueryFilter.TYPE.makeFilter(typesList::contains));
        return this;
    }

    public TransactionRequestBuilder<B, RESULT> isNot(TransactionNode.PayType... types) {
        List<TransactionNode.PayType> typesList = Arrays.stream(types).toList();
        builder.addFilter(QueryFilter.TYPE, QueryFilter.TYPE.makeFilter(Predicate.not(typesList::contains)));
        return this;
    }

    public RESULT build() {
        return builder.build();
    }
}
