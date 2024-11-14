package dk.superawesome;

import dk.superawesome.exceptions.RequestException;

public class Engine {

    public static <N extends Node> EngineQuery<N> query(EngineRequest<N> request) throws RequestException {
        try {
            return request.getExecutor().execute(request.getSettings(), request.getRequester())
                    .filter(request);
        } catch (Exception ex) {
            throw new RequestException(ex);
        }
    }
}
