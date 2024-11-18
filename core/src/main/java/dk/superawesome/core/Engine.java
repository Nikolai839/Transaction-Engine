package dk.superawesome.core;

import dk.superawesome.core.exceptions.RequestException;

public class Engine {

    public static <N extends Node> EngineQuery<N> query(EngineRequest<N> request) throws RequestException {
        try {
            return request.getExecutor().execute(request.getSettings(), request.getRequester())
                    .filter(request);
        } catch (Exception ex) {
            throw new RequestException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    public static <N extends TransactionNode, V extends PostQueryTransformer.SortBy.SortVisitor<N>> EngineQuery<N> doTransformation(Node.Collection collection, SortingMethod method, EngineQuery<N> query) {
        PostQueryTransformer.SortBy.SortVisitor<N> visitor = PostQueryTransformer.SortBy.getVisitor(collection);
        PostQueryTransformer<N, N> sort = collection.<N, V>getComparator().visit(method, (V) visitor);

        return query.transform(sort);
    }
}
