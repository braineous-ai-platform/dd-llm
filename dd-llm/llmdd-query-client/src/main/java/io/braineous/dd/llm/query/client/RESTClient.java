package io.braineous.dd.llm.query.client;

import ai.braineous.rag.prompt.cgo.api.*;
import ai.braineous.rag.prompt.cgo.query.QueryRequest;
import ai.braineous.rag.prompt.models.cgo.graph.GraphBuilder;
import ai.braineous.rag.prompt.models.cgo.graph.GraphSnapshot;
import ai.braineous.rag.prompt.observe.Console;
import ai.braineous.rag.prompt.cgo.querygen.DeclarativeQueryCompiler;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

public class RESTClient implements QueryClient{
    public static final String VERSION = "v1";

    public RESTClient() {
    }

    @Override
    public QueryResult query(LlmAdapter adapter, String sql) {
        if (adapter == null) {
            return null;
        }

        if (sql == null || sql.trim().isEmpty()) {
            return null;
        }

        JsonObject compiled = new DeclarativeQueryCompiler().compile(sql);
        if (compiled == null || !compiled.has("task")) {
            return null;
        }

        JsonObject task = compiled.getAsJsonObject("task");
        if (task == null) {
            return null;
        }

        JsonObject intent = task.getAsJsonObject("intent");
        if (intent == null) {
            return null;
        }

        String queryKind = readString(intent, "type");
        String query = readString(intent, "goal");
        String factId = readString(task, "factId");
        java.util.List<String> relatedFacts = readStringArray(task, "relatedFactIds");

        return query(adapter, queryKind, query, factId, relatedFacts);
    }

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

    //------------------------------------
    private String readString(JsonObject json, String key) {
        if (json == null) {
            return null;
        }

        if (key == null) {
            return null;
        }

        if (!json.has(key)) {
            return null;
        }

        try {
            return json.get(key).getAsString();
        } catch (Exception e) {
            return null;
        }
    }

    private java.util.List<String> readStringArray(JsonObject json, String key) {
        java.util.List<String> values = new java.util.ArrayList<String>();

        if (json == null) {
            return values;
        }

        if (key == null) {
            return values;
        }

        if (!json.has(key)) {
            return values;
        }

        try {
            JsonArray array = json.getAsJsonArray(key);
            for (int i = 0; i < array.size(); i++) {
                values.add(array.get(i).getAsString());
            }
        } catch (Exception e) {
            return values;
        }

        return values;
    }

}
