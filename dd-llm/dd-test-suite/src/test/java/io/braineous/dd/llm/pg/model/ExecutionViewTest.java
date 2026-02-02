package io.braineous.dd.llm.pg.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import ai.braineous.rag.prompt.cgo.api.QueryExecution;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

public class ExecutionViewTest {

    @Test
    public void toJson_fromJson_roundtrip_smoke() {

        ExecutionView v = new ExecutionView();
        v.setQueryKind("validate_flight_airports");

        List<QueryExecution> execs = new ArrayList<QueryExecution>();

        // raw-types on purpose: we don't need task/context wiring for this test
        QueryExecution e1 = new QueryExecution(null, "RAW_1", null, null, null);
        QueryExecution e2 = new QueryExecution(null, "RAW_2", null, null, null);

        execs.add(e1);
        execs.add(e2);

        v.setExecutions(execs);

        String json = v.toJsonString();
        assertNotNull(json);
        assertFalse(json.trim().isEmpty());

        ExecutionView v2 = ExecutionView.fromJsonString(json);
        assertNotNull(v2);
        assertEquals("validate_flight_airports", v2.getQueryKind());

        // best-effort parsing should preserve count for valid items
        assertNotNull(v2.getExecutions());
        assertEquals(2, v2.getExecutions().size());
    }

    @Test
    public void fromJson_returns_null_when_queryKind_missing() {

        JsonObject root = new JsonObject();
        // executions present but queryKind missing => invalid view
        root.add("executions", new com.google.gson.JsonArray());

        ExecutionView v = ExecutionView.fromJson(root);
        assertNull(v);
    }

    @Test
    public void fromJson_best_effort_skips_bad_execution_items() {

        JsonObject root = new JsonObject();
        root.addProperty("queryKind", "qk");

        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();

        // bad item: not an object
        arr.add("not_an_object");

        // good item: minimal object (QueryExecution.fromJson tolerates missing fields)
        JsonObject goodExec = new JsonObject();
        goodExec.add("request", null);
        goodExec.addProperty("rawResponse", "RAW");
        goodExec.add("promptValidation", null);
        goodExec.add("llmResponseValidation", null);
        goodExec.add("domainValidation", null);
        arr.add(goodExec);

        root.add("executions", arr);

        ExecutionView v = ExecutionView.fromJson(root);
        assertNotNull(v);
        assertEquals("qk", v.getQueryKind());
        assertNotNull(v.getExecutions());

        // should keep only the good object
        assertEquals(1, v.getExecutions().size());
    }
}
