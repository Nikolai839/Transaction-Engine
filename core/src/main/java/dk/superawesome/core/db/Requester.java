package dk.superawesome.core.db;

import java.time.LocalDateTime;

public interface Requester {

    String getQuery();

    String getQuery(LocalDateTime dateTime);
}
