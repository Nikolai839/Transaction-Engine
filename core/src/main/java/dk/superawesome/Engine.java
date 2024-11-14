package dk.superawesome;

public class Engine {

    public static <N extends Node> EngineQuery<N> query(EngineRequest<N> request) {
        return request.getExecutor().execute(request.getSettings(), request.getRequester())
                .filter(request);
    }
}
