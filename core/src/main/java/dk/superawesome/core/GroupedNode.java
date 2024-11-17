package dk.superawesome.core;

import java.util.List;

public interface GroupedNode<N extends Node> extends Node {

    default boolean isGrouped() {
        return true;
    }

    List<N> getNodes();
}
