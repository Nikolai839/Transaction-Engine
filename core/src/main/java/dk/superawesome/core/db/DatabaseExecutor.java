package dk.superawesome.core.db;

import dk.superawesome.core.EngineQuery;
import dk.superawesome.core.Node;
import dk.superawesome.core.exceptions.RequestException;

import java.sql.SQLException;

public interface DatabaseExecutor<N extends Node> {

    EngineQuery<N> execute(DatabaseSettings settings, Requester requester) throws RequestException, SQLException;
}
