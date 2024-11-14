package dk.superawesome.db;

import dk.superawesome.EngineQuery;
import dk.superawesome.Node;
import dk.superawesome.exceptions.RequestException;

import java.sql.SQLException;

public interface DatabaseExecutor<N extends Node> {

    EngineQuery<N> execute(DatabaseSettings settings, Requester requester) throws RequestException, SQLException;
}
