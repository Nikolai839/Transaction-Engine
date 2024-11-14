package dk.superawesome;

import dk.superawesome.db.DatabaseExecutor;
import dk.superawesome.db.DatabaseSettings;
import dk.superawesome.db.Requester;
import dk.superawesome.exceptions.RequestSetupException;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EngineRequest<N extends Node> {

    public static class Builder<N extends Node, B extends Builder<N, B>> {

        @SuppressWarnings("unchecked")
        public static <N extends Node, B extends Builder<N, B>> Builder<N, B> makeRequest(Class<? extends Builder<N, B>> clazz, DatabaseSettings settings, DatabaseExecutor executor, Requester requester) throws RequestSetupException {
            try {
                return clazz.getDeclaredConstructor(DatabaseSettings.class, DatabaseExecutor.class, Requester.class)
                        .newInstance(settings, executor, requester);
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
                throw new RequestSetupException();
            }
        }

        protected final EngineRequest<N> request;

        public Builder(DatabaseSettings settings, DatabaseExecutor<N> executor, Requester requester) {
            this.request = new EngineRequest<>(settings, executor, requester);
        }

        public B addFilter(QueryFilter.FilterType<?, N> type, QueryFilter<N> filter) {
            this.request.addFilter(type, filter);
            return (B) this;
        }

        public EngineRequest<N> build() {
            return this.request;
        }
    }

    private final List<QueryFilter.FilterData<N>> filters = new ArrayList<>();
    private final DatabaseSettings settings;
    private final DatabaseExecutor<N> executor;
    private final Requester requester;

    public EngineRequest(DatabaseSettings settings, DatabaseExecutor<N> executor, Requester requester) {
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

    public void addFilter(QueryFilter.FilterType<?, N> type, QueryFilter<N> filter) {
        this.filters.add(new QueryFilter.FilterData<>(type, filter));
    }

    public boolean filter(N node) {
        return this.filters.stream().allMatch(f -> f.filter().test(node));
    }

    public List<QueryFilter.FilterData<N>> getFilters() {
        return this.filters;
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
