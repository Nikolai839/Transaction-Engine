package dk.superawesome.core;

public interface GroupedNode<N extends Node> extends Node {

    default Collection getCollection() {
        return Collection.GROUPED;
    }

    default int size() {
        return nodes().size();
    }

    java.util.Collection<N> nodes();
}
