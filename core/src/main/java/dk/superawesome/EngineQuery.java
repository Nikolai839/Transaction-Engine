package dk.superawesome;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class EngineQuery<N extends Node> {

    private final List<N> nodes = new ArrayList<>();

    public EngineQuery() {

    }

    public EngineQuery<N> filter(EngineRequest<N> request) {
        this.nodes.removeIf(Predicate.not(request::filter));
        return this;
    }

    public EngineQuery<N> transform(PostQueryTransformer<N> transformer) {
        transformer.transform(this.nodes);
        return this;
    }

    public List<N> nodes() {
        return this.nodes;
    }
}
