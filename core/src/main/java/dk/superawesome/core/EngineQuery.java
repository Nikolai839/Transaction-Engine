package dk.superawesome.core;

import dk.superawesome.core.exceptions.RequestException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class EngineQuery<N extends Node> {

    public static <N extends Node> EngineQuery<N> create(ResultSet set, NodeFactory<N> factory) throws RequestException {
        try {
            EngineQuery<N> query = new EngineQuery<>();

            while (set.next()) {
                query.nodes.add(factory.createNode(set));
            }

            return query;
        } catch (SQLException ex) {
            throw new RequestException(ex);
        }
    }

    private final List<N> nodes = new ArrayList<>();

    private EngineQuery(List<N> nodes) {
        this.nodes.addAll(nodes);
    }

    public EngineQuery(EngineQuery<N> query) {
        this(query.nodes());
    }

    private EngineQuery() {}

    public EngineQuery<N> filter(EngineRequest<? super N> request) {
        this.nodes.removeIf(Predicate.not(request::filter));
        return this;
    }

    public EngineQuery<N> filter(QueryFilter<? super N> filter) {
        this.nodes.removeIf(Predicate.not(filter::test));
        return this;
    }

    public <TN extends Node> EngineQuery<TN> transform(PostQueryTransformer<N, TN> transformer) {
        return new EngineQuery<>(transformer.transform(this.nodes));
    }

    public boolean isEmpty() {
        return this.nodes.isEmpty();
    }

    public int size() {
        return this.nodes.size();
    }

    public List<N> nodes() {
        return this.nodes;
    }
}
