package io.braineous.dd.llm.query.client;

import ai.braineous.rag.prompt.cgo.api.*;
import ai.braineous.rag.prompt.cgo.query.QueryRequest;
import ai.braineous.rag.prompt.models.cgo.graph.GraphBuilder;
import ai.braineous.rag.prompt.models.cgo.graph.GraphSnapshot;
import ai.braineous.rag.prompt.observe.Console;

import java.util.List;

public class RESTClient implements QueryClient{
    public static final String VERSION = "v1";

    @Override
    public QueryResult query(LlmAdapter llmAdapter,
            String queryKind,
                             String query,
                             String fact,
                             List<String> relatedFacts) {

        if (queryKind == null || queryKind.trim().isEmpty()) {
            return null;
        }
        if (query == null || query.trim().isEmpty()) {
            return null;
        }
        if (fact == null || fact.trim().isEmpty()) {
            return null;
        }
        if(llmAdapter == null){
            return null;
        }

        List<String> safeRelatedFacts = relatedFacts;
        if (safeRelatedFacts == null) {
            safeRelatedFacts = java.util.Collections.emptyList();
        }

        GraphBuilder graphBuilder = GraphBuilder.getInstance();
        if (graphBuilder == null) {
            return null;
        }

        GraphSnapshot snapshot = graphBuilder.snapshot();
        if (snapshot == null) {
            return null;
        }

        Fact anchor = snapshot.findFact(fact);
        if (anchor == null) {
            return null;
        }
        if (anchor.getId() == null || anchor.getId().trim().isEmpty()) {
            return null;
        }

        Meta meta = new Meta(VERSION, queryKind, queryKind);

        ValidateTask task = new ValidateTask(query, anchor.getId());
        if (task == null) {
            return null;
        }

        GraphContextBuilder builder = new GraphContextBuilder();
        GraphContext context = builder.buildContext(snapshot, anchor, safeRelatedFacts);
        if (context == null) {
            return null;
        }

        QueryRequest request = QueryRequests.validateTask(
                meta,
                task,
                context,
                anchor.getId()
        );

        if (request == null) {
            return null;
        }

        Console.log("__request_debug____", request.toJson().toString());

        //integrate with QueryOrchestrator end-to-end, return the result
        QueryOrchestrator orch = new QueryOrchestrator();
        request.setAdapter(llmAdapter);
        QueryResult result = orch.execute(request);
        if(result == null){
            return null;
        }

        return result;
    }

}
