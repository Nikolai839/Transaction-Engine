package dk.superawesome.core;

import java.util.List;

public interface GroupedNode<N extends Node> extends Node {

    default boolean isGrouped() {
        return true;
    }

    default int size() {
        return getNodes().size();
    }

    List<N> getNodes();
}
