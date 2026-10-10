package io.braineous.dd.llm.query.client;

import ai.braineous.rag.prompt.cgo.api.*;
import ai.braineous.rag.prompt.cgo.query.QueryRequest;
import ai.braineous.rag.prompt.models.cgo.graph.GraphBuilder;
import ai.braineous.rag.prompt.models.cgo.graph.GraphSnapshot;
import ai.braineous.rag.prompt.observe.Console;
import ai.braineous.rag.prompt.cgo.querygen.DeclarativeQueryCompiler;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class RESTClient implements QueryClient {
    public static final String VERSION = "v1";

    public RESTClient() {
    }

    @Override
    public QueryResult query(String sql) {
        return querySql(sql);
    }

    private QueryResult querySql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return null;
        }

        JsonObject compiled = new DeclarativeQueryCompiler().compile(sql);
        if (compiled == null || !compiled.has("task")) {
            return null;
        }

        JsonObject taskJson = compiled.getAsJsonObject("task");
        if (taskJson == null) {
            return null;
        }

        JsonObject intent = taskJson.getAsJsonObject("intent");
        if (intent == null) {
            return null;
        }

        String queryKind = readString(intent, "type");
        String query = readString(intent, "goal");
        String factId = readString(taskJson, "factId");

        List<String> relatedFacts = readStringArray(taskJson, "relatedFactIds");
        List<String> requestedFields = readStringArray(taskJson, "select");
        List<Control> controls = readControlsFromTask(taskJson);

        return query(queryKind, query, factId, relatedFacts, requestedFields, controls);
    }

    @Override
    public QueryResult query(String queryKind,
                             String query,
                             String fact,
                             List<String> relatedFacts) {
        return query(queryKind, query, fact, relatedFacts, null, null);
    }

    private QueryResult query(String queryKind,
                              String query,
                              String fact,
                              List<String> relatedFacts,
                              List<String> requestedFields,
                              List<Control> controls) {

        if (queryKind == null || queryKind.trim().isEmpty()) {
            return null;
        }
        if (query == null || query.trim().isEmpty()) {
            return null;
        }
        if (fact == null || fact.trim().isEmpty()) {
            return null;
        }

        List<String> safeRelatedFacts = relatedFacts;
        if (safeRelatedFacts == null) {
            safeRelatedFacts = java.util.Collections.emptyList();
        }

        List<String> safeRequestedFields = requestedFields;
        if (safeRequestedFields == null) {
            safeRequestedFields = java.util.Collections.emptyList();
        }

        List<Control> safeControls = controls;
        if (safeControls == null) {
            safeControls = java.util.Collections.emptyList();
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

        ValidateTask task = new ValidateTask(query, anchor.getId(), safeRequestedFields, safeRelatedFacts);
        task.setControls(safeControls);

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

        QueryOrchestrator orch = new QueryOrchestrator();

        QueryResult result = orch.execute(request);
        if (result == null) {
            return null;
        }

        return result;
    }

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

    private List<String> readStringArray(JsonObject json, String key) {
        List<String> values = new ArrayList<String>();

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

    private List<Control> readControlsFromTask(JsonObject taskJson) {
        List<Control> controls = readControls(taskJson, "controls");

        if (controls.size() > 0) {
            return controls;
        }

        controls = readControls(taskJson, "control");

        if (controls.size() > 0) {
            return controls;
        }

        controls = readNestedControls(taskJson, "constraints", "control");

        return controls;
    }

    private List<Control> readNestedControls(JsonObject json, String parentKey, String childKey) {
        List<Control> controls = new ArrayList<Control>();

        if (json == null) {
            return controls;
        }

        if (parentKey == null) {
            return controls;
        }

        if (childKey == null) {
            return controls;
        }

        if (!json.has(parentKey)) {
            return controls;
        }

        if (!json.get(parentKey).isJsonObject()) {
            return controls;
        }

        JsonObject parent = json.getAsJsonObject(parentKey);

        return readControls(parent, childKey);
    }

    private List<Control> readControls(JsonObject json, String key) {
        List<Control> controls = new ArrayList<Control>();

        if (json == null) {
            return controls;
        }

        if (key == null) {
            return controls;
        }

        if (!json.has(key)) {
            return controls;
        }

        if (!json.get(key).isJsonObject()) {
            return controls;
        }

        JsonObject controlsObject = json.getAsJsonObject(key);
        for (String controlKey : controlsObject.keySet()) {
            if (!controlsObject.get(controlKey).isJsonNull()) {
                controls.add(new Control(controlKey, controlsObject.get(controlKey).getAsString()));
            } else {
                controls.add(new Control(controlKey, null));
            }
        }

        return controls;
    }
}