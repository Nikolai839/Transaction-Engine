package dk.superawesome.core;

import dk.superawesome.core.db.DatabaseExecutor;
import dk.superawesome.core.db.DatabaseSettings;
import dk.superawesome.core.db.Requester;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EngineRequest<N extends Node> {

    public interface Builder<N extends Node, RESULT> {

        void addFilter(QueryFilter.FilterType<?, ? super N> type, QueryFilter<? super N> filter);

        RESULT build();
    }

    public static class RequestBuilder<N extends Node> implements Builder<N, EngineRequest<N>> {

        protected final EngineRequest<N> request;

        public RequestBuilder(EngineCache<N> cache, DatabaseSettings settings, DatabaseExecutor<N> executor, Requester requester) {
            this.request = new EngineRequest<>(cache, settings, executor, requester);
        }

        @Override
        public void addFilter(QueryFilter.FilterType<?, ? super N> type, QueryFilter<? super N> filter) {
            this.request.addFilter(type, filter);
        }

        @Override
        public EngineRequest<N> build() {
            return this.request;
        }
    }

    public static class QueryWrapperBuilder<N extends Node> implements Builder<N, EngineQuery<N>> {

        private final EngineQuery<N> query;

        public QueryWrapperBuilder(EngineQuery<N> query) {
            this.query = query;
        }

        @Override
        public void addFilter(QueryFilter.FilterType<?, ? super N> type, QueryFilter<? super N> filter) {
            this.query.filter(filter);
        }

        @Override
        public EngineQuery<N> build() {
            return this.query;
        }
    }

    private final List<QueryFilter.FilterData<N>> filters = new ArrayList<>();
    private final EngineCache<N> cache;
    private final DatabaseSettings settings;
    private final DatabaseExecutor<N> executor;
    private final Requester requester;

    public EngineRequest(EngineCache<N> cache, DatabaseSettings settings, DatabaseExecutor<N> executor, Requester requester) {
        this.cache = cache;
        this.settings = settings;
        this.executor = executor;
        this.requester = requester;
    }

    public void removeAllFiltersOf(QueryFilter.FilterType<?, N> type) {
        this.filters.removeIf(f -> f.type().equals(type));
    }

    public List<QueryFilter.FilterData<N>> allFiltersOf(QueryFilter.FilterType<?, N> type) {
        return this.filters.stream().filter(f -> f.type().equals(type)).collect(Collectors.toList());
    }

    public void addFilter(QueryFilter.FilterType<?, ? super N> type, QueryFilter<? super N> filter) {
        this.filters.add(new QueryFilter.FilterData<>(type, filter));
    }

    public boolean filter(N node) {
        return this.filters.stream().allMatch(f -> f.filter().test(node));
    }

    public List<QueryFilter.FilterData<N>> getFilters() {
        return this.filters;
    }

    public EngineCache<N> getCache() {
        return this.cache;
    }

    public DatabaseSettings getSettings() {
        return this.settings;
    }

    public DatabaseExecutor<N> getExecutor() {
        return this.executor;
    }

    public Requester getRequester() {
        return this.requester;
    }
}
