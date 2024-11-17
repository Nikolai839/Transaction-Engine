package dk.superawesome.core;

import java.util.List;

public interface GroupedNode<N extends Node> extends Node {

    List<N> getNodes();
}
