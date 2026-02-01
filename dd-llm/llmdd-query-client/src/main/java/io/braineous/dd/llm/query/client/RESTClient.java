package io.braineous.dd.llm.query.client;

import ai.braineous.rag.prompt.cgo.api.*;
import ai.braineous.rag.prompt.cgo.query.QueryRequest;
import ai.braineous.rag.prompt.models.cgo.graph.GraphBuilder;
import ai.braineous.rag.prompt.models.cgo.graph.GraphSnapshot;
import ai.braineous.rag.prompt.observe.Console;

import java.util.List;

public class RESTClient implements QueryClient{
    public static final String version = "v1";

    @Override
    public QueryResult query(String queryKind, String query, String fact, List<String> relatedFacts){
        if(queryKind == null || queryKind.trim().isBlank() ||
                query == null || query.trim().isBlank()
        ){
            return null;
        }

        GraphBuilder graphBuilder = GraphBuilder.getInstance();
        GraphSnapshot snapshot = graphBuilder.snapshot();
        GraphContextBuilder builder = new GraphContextBuilder();
        Fact anchor = snapshot.findFact(fact);

        Meta meta = new Meta(version, queryKind, queryKind);

        ValidateTask task = new ValidateTask(query, anchor.getId());

        GraphContext context = builder.buildContext(snapshot, anchor, relatedFacts);

        QueryRequest request = QueryRequests.validateTask(
                meta,
                task,
                context,
                anchor.getId()
        );

        if(request == null){
            return null;
        }

        //do the query orchestration to cgo -> llm-adapter
        Console.log("__request_debug____", request.toJson().toString());

        return null;
    }
}
