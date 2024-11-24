package dk.superawesome.core.db;

import dk.superawesome.core.EngineCache;
import dk.superawesome.core.Node;

public interface Requester {

    String getQuery();

    String getQuery(EngineCache<? extends Node> cache);
}
