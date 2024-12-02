package dk.superawesome.core;

import dk.superawesome.core.exceptions.RequestException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Predicate;

public class EngineQuery<N extends Node> {

    public static <N extends Node> EngineQuery<N> create(ResultSet set, NodeFactory<N> factory, EngineCache<N> cache) throws RequestException {
        try {
            List<N> nodes = new LinkedList<>();
            while (set.next()) {
                N node = factory.createNode(set);
                nodes.add(node);
            }

            cache.getCachedNodes().addAll(nodes);

            set.close();

            return new EngineQuery<>(nodes);
        } catch (SQLException ex) {
            throw new RequestException(ex);
        }
    }

    private final List<Node> initialNodes = new LinkedList<>();
    private List<N> nodes = new LinkedList<>();

    public EngineQuery(Collection<N> nodes) {
        this.nodes.addAll(nodes);
        this.initialNodes.addAll(nodes);
    }

    public EngineQuery(Collection<N> filteredNodes, Collection<Node> initialNodes) {
        this.nodes.addAll(filteredNodes);
        this.initialNodes.addAll(initialNodes);
    }

    @SuppressWarnings("unchecked")
    public EngineQuery(EngineQuery<N> query, boolean filteredNodes) {
        this(new ArrayList<>(filteredNodes ? query.nodes() : (List<N>) query.initialNodes()), new ArrayList<>(query.initialNodes()));
    }

    public EngineQuery<N> filter(EngineRequest<? super N> request) {
        this.nodes.removeIf(Predicate.not(request::filter));
        return this;
    }

    public EngineQuery<N> filter(QueryFilter<? super N> filter) {
        this.nodes.removeIf(Predicate.not(filter::test));
        return this;
    }

    public EngineQuery<N> limit(int limit) {
        this.nodes = this.nodes.subList(0, limit);
        return this;
    }

    public <TN extends Node> EngineQuery<TN> transform(PostQueryTransformer<N, TN> transformer) {
        return new EngineQuery<>(transformer.transform(new LinkedList<>(this.nodes)), this.initialNodes);
    }

    public void addNodes(List<N> nodes) {
        this.nodes.addAll(nodes);
        this.initialNodes.addAll(nodes);
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

    public List<Node> initialNodes() {
        return this.initialNodes;
    }
}
