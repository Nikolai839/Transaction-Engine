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

    private final Deque<Node> initialNodes = new LinkedList<>();
    private Deque<N> nodes = new LinkedList<>();

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

    public EngineQuery<N> filter(Predicate<? super N> filter) {
        this.nodes.removeIf(Predicate.not(filter));
        return this;
    }

    public EngineQuery<N> limit(int limit) {
        Deque<N> nodes = new LinkedList<>();
        for (int i = 0; i <= limit; i++) {
            N node = this.nodes.poll();
            if (node == null) {
                break;
            }
            nodes.add(node);
        }

        this.nodes = nodes;
        return this;
    }

    public <TN extends Node> EngineQuery<TN> transform(PostQueryTransformer<N, TN> transformer) {
        return new EngineQuery<>(transformer.transform(new LinkedList<>(this.nodes)), this.initialNodes);
    }

    public void addNodes(Collection<N> nodes) {
        nodes.stream().sorted(Collections.reverseOrder()).forEach(node -> {
            this.nodes.addFirst(node);
            this.initialNodes.addFirst(node);
        });
    }

    public boolean isEmpty() {
        return this.nodes.isEmpty();
    }

    public int size() {
        return this.nodes.size();
    }

    public Collection<N> nodes() {
        return this.nodes;
    }

    public Collection<Node> initialNodes() {
        return this.initialNodes;
    }
}
