package dk.superawesome.core.db;

import dk.superawesome.core.EngineCache;
import dk.superawesome.core.EngineQuery;
import dk.superawesome.core.Node;
import dk.superawesome.core.exceptions.RequestException;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface DatabaseExecutor<N extends Node> {

    EngineQuery<N> execute(EngineCache<N> cache, DatabaseSettings settings, String query) throws RequestException, SQLException;
}
