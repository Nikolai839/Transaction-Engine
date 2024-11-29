package dk.superawesome.core;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface EngineCache<N extends Node> {

    LocalDateTime latestCacheTime();

    boolean isCacheEmpty();

    Collection<N> getCachedNodes();

    void markCached();
}
