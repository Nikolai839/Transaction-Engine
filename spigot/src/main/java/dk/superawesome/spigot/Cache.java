package dk.superawesome.spigot;

import dk.superawesome.core.EngineCache;
import dk.superawesome.core.transaction.SingleTransactionNode;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class Cache implements EngineCache<SingleTransactionNode> {

    private LocalDateTime lastCache;
    private final Collection<SingleTransactionNode> cache = new LinkedList<>();
    private Object running;

    @Override
    public boolean isCacheEmpty() {
        return this.lastCache == null;
    }

    @Override
    public Collection<SingleTransactionNode> getCachedNodes() {
        return this.cache;
    }

    @Override
    public boolean isRunning() {
        return this.running != null;
    }

    @Override
    public LocalDateTime start(CompletableFuture<Void> future) {
        if (this.running != null) {
            synchronized (this.running) {
                try {
                    this.running.wait();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return null;
                }
            }
        }

        LocalDateTime prevLastCache = this.lastCache;
        this.lastCache = LocalDateTime.now();
        this.running = new Object();
        future.thenAccept(v -> {
            synchronized (this.running) {
                this.running.notifyAll();
                this.running = null;
            }
        });

        return prevLastCache;
    }

    @Override
    public void reset() {
        lastCache = null;
        cache.clear();
    }
}
