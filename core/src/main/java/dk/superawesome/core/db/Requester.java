package dk.superawesome.core.db;

import java.time.LocalDateTime;

public interface Requester {

    String toQuery();

    String toQueryAfter(LocalDateTime after);
}
