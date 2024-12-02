package dk.superawesome.core;

import dk.superawesome.core.exceptions.RequestException;
import dk.superawesome.core.transaction.SingleTransactionNode;
import dk.superawesome.core.transaction.SortingMethod;
import dk.superawesome.core.transaction.TransactionNode;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class Engine {

    public static <N extends Node> EngineQuery<N> queryFromCache(EngineRequest<N> request) throws RequestException {
        try {
            if (request.getCache().isCacheEmpty() && !request.getCache().isRunning()) {
                return query(request);
            }

            CompletableFuture<Void> invoker = new CompletableFuture<>();
            LocalDateTime dateTime = request.getCache().start(invoker);

            EngineQuery<N> query = new EngineQuery<>(request.getCache().getCachedNodes());
            query.addNodes(
                    request.getExecutor().execute(request.getCache(), request.getSettings(), request.getRequester().getQuery(dateTime))
                            .nodes()
            );

            invoker.complete(null);

            return query.filter(request);
        } catch (Exception ex) {
            throw new RequestException(ex);
        }
    }

    public static <N extends Node> EngineQuery<N> query(EngineRequest<N> request) throws RequestException {
        CompletableFuture<Void> invoker = new CompletableFuture<>();
        try {
            request.getCache().start(invoker);
            EngineQuery<N> query = request.getExecutor().execute(request.getCache(), request.getSettings(), request.getRequester().getQuery())
                    .filter(request);

            invoker.complete(null);

            return query;
        } catch (Exception ex) {
            throw new RequestException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    public static <N extends TransactionNode, V extends PostQueryTransformer.SortBy.SortVisitor<N>> EngineQuery<N> sort(Node.Collection collection, SortingMethod method, EngineQuery<N> query) {
        PostQueryTransformer.SortBy.SortVisitor<N> visitor = PostQueryTransformer.SortBy.getVisitor(collection);
        PostQueryTransformer<N, N> sort = collection.<N, V>getVisitable().visit(method, (V) visitor);

        return query.transform(sort);
    }

    @SuppressWarnings("unchecked")
    public static <N extends SingleTransactionNode> EngineQuery<N> trace(EngineQuery<SingleTransactionNode> query) {
        Map<String, Double> currentTrace = new HashMap<>();

        List<SingleTransactionNode.Traced> traced = new LinkedList<>();
        for (SingleTransactionNode node : query.nodes()) {

            boolean toConsole = node.toUserName().equals(TransactionNode.CONSOLE);
            boolean fromConsole = node.fromUserName().equals(TransactionNode.CONSOLE);

            if (!toConsole || !fromConsole) {
                double toUserTrace = -1;
                double fromUserTrace = -1;
                if (!toConsole) {
                    currentTrace.put(node.toUserName(), (toUserTrace = currentTrace.getOrDefault(node.toUserName(), 0d)) + node.amount());
                }
                if (!fromConsole) {
                    currentTrace.put(node.fromUserName(), (fromUserTrace = currentTrace.getOrDefault(node.fromUserName(), 0d)) - node.amount());
                }

                traced.add(new SingleTransactionNode.Traced(node, fromUserTrace, toUserTrace));
            }
        }

        return (EngineQuery<N>) new EngineQuery<>(traced);
    }
}
