package dk.superawesome.core;

import dk.superawesome.core.db.DatabaseExecutor;
import dk.superawesome.core.db.DatabaseSettings;
import dk.superawesome.core.db.Requester;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TransactionRequestBuilder extends EngineRequest.Builder<SingleTransactionNode, TransactionRequestBuilder> {

    public static TransactionRequestBuilder makeRequest(EngineCache<SingleTransactionNode> cache, DatabaseSettings settings, DatabaseExecutor<SingleTransactionNode> executor, Requester requester) {
        return new TransactionRequestBuilder(cache, settings, executor, requester);
    }

    public TransactionRequestBuilder(EngineCache<SingleTransactionNode> cache, DatabaseSettings settings, DatabaseExecutor<SingleTransactionNode> executor, Requester requester) {
        super(cache, settings, executor, requester);
    }

    public TransactionRequestBuilder from(ZonedDateTime date) {
        addFilter(QueryFilter.FilterTypes.TIME, QueryFilter.FilterTypes.TIME.makeFilter(d -> d.isAfter(date)));
        return this;
    }

    public TransactionRequestBuilder to(ZonedDateTime date) {
        addFilter(QueryFilter.FilterTypes.TIME, QueryFilter.FilterTypes.TIME.makeFilter(d -> d.isBefore(date)));
        return this;
    }

    public TransactionRequestBuilder range(ZonedDateTime from, ZonedDateTime to) {
        from(to.isAfter(from) ? from : to);
        to(to.isAfter(from) ? to : from);
        return this;
    }

    public TransactionRequestBuilder range(double from, double to) {
        addFilter(QueryFilter.FilterTypes.AMOUNT, QueryFilter.FilterTypes.AMOUNT.makeFilter(d -> d > Math.min(from, to) && d < Math.max(from, to)));
        return this;
    }

    public TransactionRequestBuilder from(double from) {
        return range(from, Double.MAX_VALUE);
    }

    public TransactionRequestBuilder to(double to) {
        return range(0, to);
    }

    private TransactionRequestBuilder forPlayers(QueryFilter.FilterType<String, SingleTransactionNode> filter, String... names) {
        if (names.length > 0) {
            List<String> lowerCasedNames = Arrays.stream(names).map(String::toLowerCase).toList();
            addFilter(filter, filter.makeFilter(n -> lowerCasedNames.contains(n.toLowerCase())));
        }

        return this;
    }

    public TransactionRequestBuilder from(String... names) {
        return forPlayers(QueryFilter.FilterTypes.FROM_USER, names);
    }

    public TransactionRequestBuilder to(String... names) {
        return forPlayers(QueryFilter.FilterTypes.TO_USER, names);
    }

    public TransactionRequestBuilder is(TransactionNode.PayType... types) {
        List<TransactionNode.PayType> typesList = Arrays.stream(types).toList();
        addFilter(QueryFilter.FilterTypes.TYPE, QueryFilter.FilterTypes.TYPE.makeFilter(typesList::contains));
        return this;
    }

    public TransactionRequestBuilder isNot(TransactionNode.PayType... types) {
        List<TransactionNode.PayType> typesList = Arrays.stream(types).toList();
        addFilter(QueryFilter.FilterTypes.TYPE, QueryFilter.FilterTypes.TYPE.makeFilter(Predicate.not(typesList::contains)));
        return this;
    }
}
