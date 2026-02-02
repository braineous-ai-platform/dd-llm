package io.braineous.dd.llm.pg.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ai.braineous.rag.prompt.cgo.api.QueryExecution;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ExecutionView {

    private String queryKind;

    private List<QueryExecution> executions;

    public ExecutionView() {
    }

    public String getQueryKind() {
        return queryKind;
    }

    public void setQueryKind(String queryKind) {
        this.queryKind = queryKind;
    }

    public List<QueryExecution> getExecutions() {
        return executions;
    }

    public void setExecutions(List<QueryExecution> executions) {
        this.executions = executions;
    }

    // ---------------------------------------------------------
    // Safe getters / helpers
    // ---------------------------------------------------------

    public String safeQueryKind() {
        return safe(this.queryKind);
    }

    public List<QueryExecution> safeExecutions() {
        if (this.executions == null) {
            return Collections.emptyList();
        }
        return this.executions;
    }

    public boolean hasExecutions() {
        return this.executions != null && !this.executions.isEmpty();
    }

    public void addExecution(QueryExecution exec) {
        if (exec == null) {
            return;
        }
        if (this.executions == null) {
            this.executions = new ArrayList<QueryExecution>();
        }
        this.executions.add(exec);
    }

    // ---------------------------------------------------------
    // JSON
    // ---------------------------------------------------------

    public JsonObject toJson() {

        JsonObject root = new JsonObject();

        String qk = safe(this.queryKind);
        if (qk != null) {
            root.addProperty("queryKind", qk);
        } else {
            root.add("queryKind", null);
        }

        JsonArray arr = new JsonArray();
        List<QueryExecution> list = safeExecutions();

        for (int i = 0; i < list.size(); i++) {
            QueryExecution exec = list.get(i);
            if (exec == null) {
                continue;
            }
            try {
                JsonObject ej = exec.toJson();
                if (ej != null) {
                    arr.add(ej);
                }
            } catch (RuntimeException re) {
                // best-effort: skip bad item
            }
        }

        root.add("executions", arr);
        return root;
    }

    public String toJsonString() {
        return toJson().toString();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static ExecutionView fromJson(JsonObject json) {

        if (json == null) {
            return null;
        }

        String queryKind = null;
        try {
            if (json.has("queryKind") && !json.get("queryKind").isJsonNull()) {
                queryKind = safe(json.get("queryKind").getAsString());
            }
        } catch (RuntimeException re) {
            queryKind = null;
        }

        if (queryKind == null) {
            return null;
        }

        List<QueryExecution> out = new ArrayList<QueryExecution>();

        try {
            if (json.has("executions") && !json.get("executions").isJsonNull()) {
                JsonElement el = json.get("executions");
                if (el != null && el.isJsonArray()) {
                    JsonArray arr = el.getAsJsonArray();
                    for (int i = 0; i < arr.size(); i++) {
                        JsonElement item = arr.get(i);
                        if (item == null || !item.isJsonObject()) {
                            continue;
                        }
                        try {
                            QueryExecution<?> exec = (QueryExecution<?>) QueryExecution.fromJson(item.getAsJsonObject());
                            if (exec != null) {
                                out.add((QueryExecution) exec);
                            }
                        } catch (RuntimeException re) {
                            // best-effort: skip bad item
                        }
                    }
                }
            }
        } catch (RuntimeException re) {
            // best-effort: treat as empty
        }

        ExecutionView v = new ExecutionView();
        v.setQueryKind(queryKind);
        v.setExecutions(out);
        return v;
    }

    public static ExecutionView fromJsonString(String json) {
        if (json == null) {
            return null;
        }

        String t = json.trim();
        if (t.isEmpty()) {
            return null;
        }

        try {
            JsonElement el = JsonParser.parseString(t);
            if (el == null || !el.isJsonObject()) {
                return null;
            }
            return fromJson(el.getAsJsonObject());
        } catch (RuntimeException re) {
            return null;
        }
    }

    // ---------------------------------------------------------
    // helpers
    // ---------------------------------------------------------

    private static String safe(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return null;
        }
        return t;
    }
}


