package io.braineous.dd.llm.cr.model;

import ai.braineous.rag.prompt.cgo.api.QueryExecution;
import ai.braineous.rag.prompt.cgo.query.CgoQueryPipeline;
import ai.braineous.rag.prompt.cgo.query.QueryRequest;
import ai.braineous.rag.prompt.observe.Console;
import com.google.gson.JsonObject;
import io.braineous.dd.llm.query.client.QueryClient;
import io.braineous.dd.llm.query.client.QueryOrchestrator;
import io.braineous.dd.llm.query.client.QueryResult;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CommitRequestTest {

    @Test
    void safeQueryKind_trims_and_nulls_empty() {
        Console.log("CR_UT/safeQueryKind", "start");

        CommitRequest r = new CommitRequest();

        r.setQueryKind(null);
        assertNull(r.safeQueryKind());

        r.setQueryKind("   ");
        assertNull(r.safeQueryKind());

        r.setQueryKind("  user.search  ");
        assertEquals("user.search", r.safeQueryKind());

        Console.log("CR_UT/safeQueryKind", "done");
    }

    @Test
    void safeNotes_null_returns_empty_list() {
        Console.log("CR_UT/safeNotes_null", "start");

        CommitRequest r = new CommitRequest();
        r.setNotes(null);

        List<String> list = r.safeNotes();
        assertNotNull(list);
        assertEquals(0, list.size());

        Console.log("CR_UT/safeNotes_null", "done");
    }

    @Test
    void toJson_filters_notes_and_trims_fields() {
        Console.log("CR_UT/toJson_filters_notes", "start");

        CommitRequest r = new CommitRequest();
        r.setQueryKind("  user.search  ");
        r.setCatalogVersion("  v1  ");
        r.setActor("  sohil ");
        r.setNotes(Arrays.asList("  a  ", "   ", null, "b"));

        JsonObject payload = new JsonObject();
        payload.addProperty("x", "  y  ");
        r.setPayload(payload);

        JsonObject json = r.toJson();

        assertEquals("user.search", json.get("queryKind").getAsString());
        assertEquals("v1", json.get("catalogVersion").getAsString());
        assertEquals("sohil", json.get("actor").getAsString());

        assertTrue(json.has("notes"));
        assertTrue(json.get("notes").isJsonArray());
        assertEquals(2, json.get("notes").getAsJsonArray().size());
        assertEquals("a", json.get("notes").getAsJsonArray().get(0).getAsString());
        assertEquals("b", json.get("notes").getAsJsonArray().get(1).getAsString());

        assertTrue(json.has("payload"));
        assertTrue(json.get("payload").isJsonObject());
        assertEquals("  y  ", json.get("payload").getAsJsonObject().get("x").getAsString());

        Console.log("CR_UT/toJson_filters_notes", "done");
    }

    @Test
    void json_roundtrip_preserves_payload_and_filters_notes() {
        Console.log("CR_UT/json_roundtrip", "start");

        CommitRequest r = new CommitRequest();
        r.setQueryKind("  user.search  ");
        r.setCatalogVersion(" v1 ");
        r.setActor(" sohil ");

        r.setNotes(Arrays.asList("  a  ", "", "   ", null, "b"));

        JsonObject payload = new JsonObject();
        payload.addProperty("k1", "v1");
        payload.addProperty("k2", 2);
        r.setPayload(payload);

        String s = r.toJsonString();
        CommitRequest loaded = CommitRequest.fromJsonString(s);

        assertNotNull(loaded);
        assertEquals("user.search", loaded.safeQueryKind());
        assertEquals("v1", loaded.safeCatalogVersion());

        assertEquals("sohil", loaded.safeActor());

        assertNotNull(loaded.getNotes());
        assertEquals(2, loaded.getNotes().size());
        assertEquals("a", loaded.getNotes().get(0));
        assertEquals("b", loaded.getNotes().get(1));

        assertNotNull(loaded.getPayload());
        assertEquals("v1", loaded.getPayload().get("k1").getAsString());
        assertEquals(2, loaded.getPayload().get("k2").getAsInt());

        Console.log("CR_UT/json_roundtrip", "done");
    }

    @Test
    void canonicalJsonString_is_stable_under_key_order() {
        Console.log("CR_UT/canonicalJsonString_key_order", "start");

        JsonObject a = new JsonObject();
        a.addProperty("b", 2);
        a.addProperty("a", 1);

        JsonObject b = new JsonObject();
        b.addProperty("a", 1);
        b.addProperty("b", 2);

        String ca = CommitRequest.canonicalJsonString(a);
        String cb = CommitRequest.canonicalJsonString(b);

        assertEquals(ca, cb);

        Console.log("CR_UT/canonicalJsonString_key_order", "done");
    }


    @Test
    void computeCommitId_is_deterministic_for_same_request() {
        Console.log("CR_UT/computeCommitId_deterministic", "start");

        CommitRequest r1 = new CommitRequest();
        r1.setQueryKind("Q");
        r1.setCatalogVersion("V");

        JsonObject p1 = new JsonObject();
        p1.addProperty("a", 1);
        p1.addProperty("b", 2);
        r1.setPayload(p1);

        CommitRequest r2 = new CommitRequest();
        r2.setQueryKind(" Q ");
        r2.setCatalogVersion(" V ");

        JsonObject p2 = new JsonObject();
        p2.addProperty("b", 2);
        p2.addProperty("a", 1);
        r2.setPayload(p2);

        Console.log("CR_UT/computeCommitId_deterministic", "done");
    }

    @Test
    void computeCommitId_changes_when_payload_changes() {
        Console.log("CR_UT/computeCommitId_payload_change", "start");

        CommitRequest r = new CommitRequest();
        r.setQueryKind("Q");
        r.setCatalogVersion("V");

        JsonObject p = new JsonObject();
        p.addProperty("a", 1);
        r.setPayload(p);


        p.addProperty("a", 2); // mutate payload
        r.setPayload(p);

        Console.log("CR_UT/computeCommitId_payload_change", "done");
    }


    @Test
    void fromJsonString_returns_null_on_blank_or_non_object() {
        Console.log("CR_UT/fromJsonString_null_cases", "start");

        assertNull(CommitRequest.fromJsonString(null));
        assertNull(CommitRequest.fromJsonString("   "));
        assertNull(CommitRequest.fromJsonString("[]"));
        assertNull(CommitRequest.fromJsonString("\"x\""));
        assertNull(CommitRequest.fromJsonString("123"));

        Console.log("CR_UT/fromJsonString_null_cases", "done");
    }

    @Test
    void canonicalJsonString_handles_nested_objects_and_arrays() {
        Console.log("CR_UT/canonicalJsonString_nested", "start");

        JsonObject root = new JsonObject();
        JsonObject inner = new JsonObject();
        inner.addProperty("z", 1);
        inner.addProperty("a", 2);
        root.add("inner", inner);

        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        arr.add("x");
        arr.add(2);
        root.add("arr", arr);

        String c = CommitRequest.canonicalJsonString(root);

        assertTrue(c.contains("\"arr\":[\"x\",2]"));
        assertTrue(c.contains("\"inner\":{"));
        assertTrue(c.contains("\"a\":2"));
        assertTrue(c.contains("\"z\":1"));

        Console.log("CR_UT/canonicalJsonString_nested", "done");
    }

    @Test
    void canonicalCommitString_is_stable_and_ordered() {
        Console.log("CR_UT/canonicalCommitString_contract", "start");

        CommitRequest r1 = new CommitRequest();
        r1.setQueryKind("  Q  ");
        r1.setCatalogVersion("  V  ");

        JsonObject p1 = new JsonObject();
        p1.addProperty("b", 2);
        p1.addProperty("a", 1);
        r1.setPayload(p1);

        CommitRequest r2 = new CommitRequest();
        r2.setQueryKind("Q");
        r2.setCatalogVersion("V");

        JsonObject p2 = new JsonObject();
        p2.addProperty("a", 1);
        p2.addProperty("b", 2);
        r2.setPayload(p2);

        String c1 = CommitRequest.canonicalJsonString(r1.getPayload());
        String c2 = CommitRequest.canonicalJsonString(r2.getPayload());

        assertEquals(c1, c2);

        Console.log("CR_UT/canonicalCommitString_contract", "done");
    }

    public static class QueryOrchestratorTest {

        @Test
        void execute_null_request_returns_fail_with_why() {
            Console.log("UT", "execute_null_request_returns_fail_with_why");

            CgoQueryPipeline pipeline = mock(CgoQueryPipeline.class);
            QueryOrchestrator orch = new QueryOrchestrator(pipeline);

            QueryResult out = orch.execute(null);

            Console.log("UT", out.toJson());

            assertFalse(out.isOk());
            assertNotNull(out.getWhy());
            assertEquals("DD-LLM-QUERYORCH-FAIL-request_null", out.getWhy().reason());

            verifyNoInteractions(pipeline);
        }

        @Test
        void execute_pipeline_returns_null_execution_returns_fail_with_why() {
            Console.log("UT", "execute_pipeline_returns_null_execution_returns_fail_with_why");

            CgoQueryPipeline pipeline = mock(CgoQueryPipeline.class);
            QueryRequest request = mock(QueryRequest.class);

            when(pipeline.execute(request)).thenReturn(null);

            QueryOrchestrator orch = new QueryOrchestrator(pipeline);
            QueryResult out = orch.execute(request);

            Console.log("UT", out.toJson());

            assertFalse(out.isOk());
            assertNotNull(out.getWhy());
            assertEquals("DD-LLM-QUERYORCH-FAIL-execution_null", out.getWhy().reason());

            verify(pipeline, times(1)).execute(request);
            verifyNoMoreInteractions(pipeline);
        }

        @Test
        void execute_success_returns_ok_with_request_and_execution_json() {
            Console.log("UT", "execute_success_returns_ok_with_request_and_execution_json");

            CgoQueryPipeline pipeline = mock(CgoQueryPipeline.class);
            QueryRequest request = mock(QueryRequest.class);
            QueryExecution execution = mock(QueryExecution.class);

            JsonObject reqJson = new JsonObject();
            reqJson.addProperty("q", "hello");

            JsonObject execJson = new JsonObject();
            execJson.addProperty("tookMs", 7);

            when(request.toJson()).thenReturn(reqJson);
            when(execution.toJson()).thenReturn(execJson);
            when(pipeline.execute(request)).thenReturn(execution);

            QueryOrchestrator orch = new QueryOrchestrator(pipeline);
            QueryResult out = orch.execute(request);

            Console.log("UT", out.toJson());

            assertTrue(out.isOk());
            assertNull(out.getWhy());
            assertNotNull(out.getRequestJson());
            assertNotNull(out.getQueryExecutionJson());

            assertEquals("hello", out.getRequestJson().get("q").getAsString());
            assertEquals(7, out.getQueryExecutionJson().get("tookMs").getAsInt());

            verify(pipeline, times(1)).execute(request);
        }
    }
}

