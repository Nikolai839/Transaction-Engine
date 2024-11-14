package dk.superawesome;

import dk.superawesome.db.DatabaseExecutor;
import dk.superawesome.db.DatabaseSettings;
import dk.superawesome.db.Requester;

import java.util.Arrays;
import java.util.Date;

public class TransactionRequestBuilder extends EngineRequest.Builder<TransactionNode, TransactionRequestBuilder> {

    public TransactionRequestBuilder(DatabaseSettings settings, DatabaseExecutor<TransactionNode> executor, Requester requester) {
        super(settings, executor, requester);
    }

    public TransactionRequestBuilder from(Date date) {
        addFilter(QueryFilter.FilterTypes.TIME, QueryFilter.FilterTypes.TIME.makeFilter(d -> d.after(date)));
        return this;
    }

    public TransactionRequestBuilder to(Date date) {
        addFilter(QueryFilter.FilterTypes.TIME, QueryFilter.FilterTypes.TIME.makeFilter(d -> d.before(date)));
        return this;
    }

    public TransactionRequestBuilder range(Date from, Date to) {
        from(to.getTime() > from.getTime() ? from : to);
        to(to.getTime() > from.getTime() ? to : from);
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

    private TransactionRequestBuilder forPlayers(QueryFilter.FilterType<String, TransactionNode> filter, String... names) {
        addFilter(filter, filter.makeFilter(n -> Arrays.asList(names).contains(n)));
        return this;
    }

    public TransactionRequestBuilder from(String... names) {
        return forPlayers(QueryFilter.FilterTypes.FROM_USER, names);
    }

    public TransactionRequestBuilder to(String... names) {
        return forPlayers(QueryFilter.FilterTypes.TO_USER, names);
    }
}
