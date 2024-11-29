package dk.superawesome.spigot;

import dk.superawesome.core.EngineCache;
import dk.superawesome.core.SingleTransactionNode;

import java.time.LocalDateTime;
import java.util.*;

public class Cache implements EngineCache<SingleTransactionNode> {

    private LocalDateTime lastCache;
    private final Collection<SingleTransactionNode> cache = new LinkedList<>();

    @Override
    public LocalDateTime latestCacheTime() {
        return this.lastCache;
    }

    @Override
    public boolean isCacheEmpty() {
        return this.lastCache == null;
    }

    @Override
    public Collection<SingleTransactionNode> getCachedNodes() {
        return this.cache;
    }

    @Override
    public void markCached() {
        this.lastCache = LocalDateTime.now();
    }
}
