package dk.superawesome.core;

import java.time.LocalDateTime;
import java.util.Set;

public interface EngineCache<N extends Node> {

    LocalDateTime latestCacheTime();

    boolean isCacheEmpty();

    Set<N> getCachedNodes();

    void markCached();
}
