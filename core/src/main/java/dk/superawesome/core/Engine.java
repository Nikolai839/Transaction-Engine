package dk.superawesome.core;

import dk.superawesome.core.exceptions.RequestException;

import java.util.ArrayList;

public class Engine {

    public static <N extends Node> EngineQuery<N> queryFromCache(EngineRequest<N> request) throws RequestException {
        try {
            if (request.getCache().isCacheEmpty()) {
                return query(request);
            }

            EngineQuery<N> query = new EngineQuery<>(request.getCache().getCachedNodes());
            query.addNodes(
                    request.getExecutor().execute(request.getCache(), request.getSettings(), request.getRequester().getQuery(request.getCache().latestCacheTime()))
                            .nodes()
            );

            return query.filter(request);
        } catch (Exception ex) {
            throw new RequestException(ex);
        }
    }

    public static <N extends Node> EngineQuery<N> query(EngineRequest<N> request) throws RequestException {
        try {
            return request.getExecutor().execute(request.getCache(), request.getSettings(), request.getRequester().getQuery())
                    .filter(request);
        } catch (Exception ex) {
            throw new RequestException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    public static <N extends TransactionNode, V extends PostQueryTransformer.SortBy.SortVisitor<N>> EngineQuery<N> doTransformation(Node.Collection collection, SortingMethod method, EngineQuery<N> query) {
        PostQueryTransformer.SortBy.SortVisitor<N> visitor = PostQueryTransformer.SortBy.getVisitor(collection);
        PostQueryTransformer<N, N> sort = collection.<N, V>getVisitable().visit(method, (V) visitor);

        return query.transform(sort);
    }
}
