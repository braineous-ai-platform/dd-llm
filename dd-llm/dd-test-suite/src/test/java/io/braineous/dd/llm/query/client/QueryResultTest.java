package io.braineous.dd.llm.query.client;

import ai.braineous.rag.prompt.observe.Console;
import com.google.gson.JsonObject;
import io.braineous.dd.llm.core.model.Why;
import io.braineous.dd.llm.query.client.QueryResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class QueryResultTest {

    @Test
    void ok_factory_sets_ok_true_sets_payloads_and_why_null() {
        Console.log("UT", "ok_factory_sets_ok_true_sets_payloads_and_why_null");

        JsonObject req = new JsonObject();
        req.addProperty("q", "hello");

        JsonObject exec = new JsonObject();
        exec.addProperty("tookMs", 12);

        QueryResult r = QueryResult.ok(req, exec);

        Console.log("UT", r.toString());

        assertNotNull(r.getId());
        assertTrue(r.getId().startsWith("DD-LLM-QUERY"));

        assertTrue(r.isOk());
        assertSame(req, r.getRequestJson());
        assertSame(exec, r.getQueryExecutionJson());
        assertNull(r.getWhy());
    }

    @Test
    void ok_constructor_when_requestJson_null_converts_to_failure_with_why_and_null_payloads() {
        Console.log("UT", "ok_constructor_requestJson_null");

        JsonObject exec = new JsonObject();
        exec.addProperty("t", "x");

        QueryResult r = new QueryResult(null, exec, true, null);

        Console.log("UT", r.toJson());

        assertFalse(r.isOk());
        assertNull(r.getRequestJson());
        assertNull(r.getQueryExecutionJson());

        assertNotNull(r.getWhy());
        assertEquals("DD-LLM-QUERY-FAIL-requestJson_null", r.getWhy().reason());
    }

    @Test
    void ok_constructor_when_queryExecutionJson_null_converts_to_failure_with_why_and_null_payloads() {
        Console.log("UT", "ok_constructor_queryExecutionJson_null");

        JsonObject req = new JsonObject();
        req.addProperty("q", "x");

        QueryResult r = new QueryResult(req, null, true, null);

        Console.log("UT", r.toJson());

        assertFalse(r.isOk());
        assertNull(r.getRequestJson());
        assertNull(r.getQueryExecutionJson());

        assertNotNull(r.getWhy());
        assertEquals("DD-LLM-QUERY-FAIL-requestJson_null", r.getWhy().reason());
    }

    @Test
    void fail_factory_with_why_sets_ok_false_preserves_payloads_and_why() {
        Console.log("UT", "fail_factory_with_why");

        JsonObject req = new JsonObject();
        req.addProperty("q", "x");

        JsonObject exec = new JsonObject();
        exec.addProperty("step", "parse");

        Why why = new Why("X", "bad");

        QueryResult r = QueryResult.fail(req, exec, why);

        Console.log("UT", r.toJson());

        assertFalse(r.isOk());
        assertSame(req, r.getRequestJson());
        assertSame(exec, r.getQueryExecutionJson());
        assertSame(why, r.getWhy());
        assertNotNull(r.getId());
    }

    @Test
    void fail_constructor_when_why_null_autofills_missing_why() {
        Console.log("UT", "fail_constructor_missing_why");

        JsonObject req = new JsonObject();
        req.addProperty("q", "x");

        JsonObject exec = new JsonObject();
        exec.addProperty("step", "y");

        QueryResult r = new QueryResult(req, exec, false, null);

        Console.log("UT", r.toJson());

        assertFalse(r.isOk());
        assertNotNull(r.getWhy());
        assertEquals("DD-LLM-QUERY-missing_why", r.getWhy().reason());
    }

    @Test
    void toJson_includes_ok_and_payloads_and_why_null_on_success() {
        Console.log("UT", "toJson_success");

        JsonObject req = new JsonObject();
        req.addProperty("q", "hello");

        JsonObject exec = new JsonObject();
        exec.addProperty("tookMs", 9);

        QueryResult r = QueryResult.ok(req, exec);
        JsonObject json = r.toJson();

        Console.log("UT", json);

        assertTrue(json.get("ok").getAsBoolean());
        assertTrue(json.get("why").isJsonNull());
        assertEquals("hello", json.getAsJsonObject("requestJson").get("q").getAsString());
        assertEquals(9, json.getAsJsonObject("queryExecutionJson").get("tookMs").getAsInt());
    }

    @Test
    void toJson_includes_why_object_on_failure() {
        Console.log("UT", "toJson_failure");

        Why why = new Why("R", "D");
        QueryResult r = QueryResult.fail(why);

        JsonObject json = r.toJson();
        Console.log("UT", json);

        assertFalse(json.get("ok").getAsBoolean());
        assertEquals("R", json.getAsJsonObject("why").get("reason").getAsString());
        assertEquals("D", json.getAsJsonObject("why").get("details").getAsString());
    }

    @Test
    void fromJson_null_throws() {
        Console.log("UT", "fromJson_null");

        try {
            QueryResult.fromJson(null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
            Console.log("UT", "IllegalArgumentException thrown as expected");
        }
    }


    @Test
    void roundtrip_success_preserves_fields_and_id() {
        Console.log("UT", "roundtrip_success");

        JsonObject req = new JsonObject();
        req.addProperty("q", "hello");

        JsonObject exec = new JsonObject();
        exec.addProperty("tookMs", 1);

        QueryResult r = QueryResult.ok(req, exec);
        JsonObject json = r.toJson();
        json.addProperty("id", "DD-LLM-QUERY999");

        Console.log("UT", json);

        QueryResult out = QueryResult.fromJson(json);

        Console.log("UT", out.toJson());

        assertEquals("DD-LLM-QUERY999", out.getId());
        assertTrue(out.isOk());
        assertNull(out.getWhy());
    }

    @Test
    void roundtrip_failure_preserves_why_and_payloads() {
        Console.log("UT", "roundtrip_failure");

        JsonObject req = new JsonObject();
        req.addProperty("q", "x");

        JsonObject exec = new JsonObject();
        exec.addProperty("step", "s1");

        Why why = new Why("BAD", "boom");

        QueryResult r = QueryResult.fail(req, exec, why);
        JsonObject json = r.toJson();

        Console.log("UT", json);

        QueryResult out = QueryResult.fromJson(json);

        Console.log("UT", out.toJson());

        assertFalse(out.isOk());
        assertEquals("BAD", out.getWhy().reason());
    }

    @Test
    void fromJson_ok_true_but_missing_payloads_converts_to_failure() {
        Console.log("UT", "fromJson_ok_true_missing_payloads");

        JsonObject json = new JsonObject();
        json.addProperty("ok", true);
        json.add("requestJson", null);
        json.add("queryExecutionJson", null);
        json.add("why", null);

        Console.log("UT", json);

        QueryResult out = QueryResult.fromJson(json);

        Console.log("UT", out.toJson());

        assertFalse(out.isOk());
        assertNotNull(out.getWhy());
        assertEquals("DD-LLM-QUERY-FAIL-requestJson_null", out.getWhy().reason());
    }
}

