package dk.superawesome.db;

import dk.superawesome.EngineQuery;
import dk.superawesome.Node;

public interface DatabaseExecutor<N extends Node> {

    EngineQuery<N> execute(DatabaseSettings settings, Requester requester);
}
