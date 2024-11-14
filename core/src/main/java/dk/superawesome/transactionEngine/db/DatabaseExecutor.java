package dk.superawesome.transactionEngine.db;

import dk.superawesome.transactionEngine.EngineQuery;
import dk.superawesome.transactionEngine.Node;

public interface DatabaseExecutor<N extends Node> {

    EngineQuery<N> execute(DatabaseSettings settings, Requester requester);
}
