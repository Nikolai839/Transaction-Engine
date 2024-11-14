package dk.superawesome;

import dk.superawesome.exceptions.RequestException;

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

    private EngineQuery() {}

    public EngineQuery<N> filter(EngineRequest<N> request) {
        this.nodes.removeIf(Predicate.not(request::filter));
        return this;
    }

    public EngineQuery<N> transform(PostQueryTransformer<N> transformer) {
        transformer.transform(this.nodes);
        return this;
    }

    public boolean isEmpty() {
        return this.nodes.isEmpty();
    }

    public List<N> nodes() {
        return this.nodes;
    }
}
