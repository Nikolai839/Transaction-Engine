package dk.superawesome.core;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public interface EngineCache<N extends Node> {

    boolean isCacheEmpty();

    Collection<N> getCachedNodes();

    boolean isRunning();

    LocalDateTime start(CompletableFuture<Void> invoker);

    void reset();
}
