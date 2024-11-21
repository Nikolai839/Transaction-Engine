package dk.superawesome.core;

public interface GroupedNode<N extends Node> extends Node {

    default boolean isGrouped() {
        return true;
    }

    default int size() {
        return getNodes().size();
    }

    java.util.Collection<N> getNodes();
}
