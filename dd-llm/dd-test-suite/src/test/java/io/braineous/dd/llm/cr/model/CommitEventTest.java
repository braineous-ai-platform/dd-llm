package io.braineous.dd.llm.cr.model;

import ai.braineous.rag.prompt.observe.Console;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CommitEventTest {

    @Test
    void safeCommitId_trims_and_nulls_empty() {
        Console.log("CE_UT/safeCommitId", "start");

        CommitEvent e = new CommitEvent();

        e.setCommitId(null);
        assertNull(e.safeCommitId());

        e.setCommitId("   ");
        assertNull(e.safeCommitId());

        e.setCommitId("  cr_1  ");
        assertEquals("cr_1", e.safeCommitId());

        Console.log("CE_UT/safeCommitId", "done");
    }

    @Test
    void safeCreatedAt_trims_and_nulls_empty() {
        Console.log("CE_UT/safeCreatedAt", "start");

        CommitEvent e = new CommitEvent();

        e.setCreatedAt(null);
        assertNull(e.safeCreatedAt());

        e.setCreatedAt("   ");
        assertNull(e.safeCreatedAt());

        e.setCreatedAt("  2026-01-27T00:00:00Z  ");
        assertEquals("2026-01-27T00:00:00Z", e.safeCreatedAt());

        Console.log("CE_UT/safeCreatedAt", "done");
    }

    @Test
    void toJson_omits_blank_fields_but_includes_request_when_present() {
        Console.log("CE_UT/toJson_omits_blanks", "start");

        CommitEvent e = new CommitEvent();
        e.setCommitId("  ");
        e.setCreatedAt("  ");

        CommitRequest r = new CommitRequest();
        r.setQueryKind("  user.search  ");
        r.setNotes(java.util.Arrays.asList("  a  ", " ", null));

        JsonObject payload = new JsonObject();
        payload.addProperty("x", "y");
        r.setPayload(payload);

        e.setRequest(r);

        JsonObject json = e.toJson();

        assertFalse(json.has("commitId"));
        assertFalse(json.has("createdAt"));

        assertTrue(json.has("request"));
        assertTrue(json.get("request").isJsonObject());

        JsonObject rj = json.get("request").getAsJsonObject();
        assertEquals("user.search", rj.get("queryKind").getAsString());

        Console.log("CE_UT/toJson_omits_blanks", "done");
    }

    @Test
    void json_roundtrip_with_request() {
        Console.log("CE_UT/json_roundtrip_with_request", "start");

        CommitEvent e = new CommitEvent();
        e.setCommitId(" cr_aaa ");
        e.setCreatedAt(" 2026-01-27T01:02:03Z ");

        CommitRequest r = new CommitRequest();
        r.setQueryKind("  user.search  ");
        r.setCatalogVersion("  v1  ");
        r.setActor("  sohil ");
        r.setNotes(java.util.Arrays.asList("  a  ", " ", null, "b"));

        JsonObject payload = new JsonObject();
        payload.addProperty("k1", "v1");
        payload.addProperty("k2", 2);
        r.setPayload(payload);

        e.setRequest(r);

        String s = e.toJsonString();
        CommitEvent loaded = CommitEvent.fromJsonString(s);

        assertNotNull(loaded);

        // commitId/createdAt were trimmed by toJson() before serialization
        assertEquals("cr_aaa", loaded.getCommitId());
        assertEquals("2026-01-27T01:02:03Z", loaded.getCreatedAt());

        assertNotNull(loaded.getRequest());
        assertEquals("user.search", loaded.getRequest().safeQueryKind());
        assertEquals("v1", loaded.getRequest().safeCatalogVersion());
        assertEquals("sohil", loaded.getRequest().safeActor());

        assertNotNull(loaded.getRequest().getNotes());
        assertEquals(2, loaded.getRequest().getNotes().size());
        assertEquals("a", loaded.getRequest().getNotes().get(0));
        assertEquals("b", loaded.getRequest().getNotes().get(1));

        assertNotNull(loaded.getRequest().getPayload());
        assertEquals("v1", loaded.getRequest().getPayload().get("k1").getAsString());
        assertEquals(2, loaded.getRequest().getPayload().get("k2").getAsInt());

        Console.log("CE_UT/json_roundtrip_with_request", "done");
    }

    @Test
    void json_roundtrip_without_request() {
        Console.log("CE_UT/json_roundtrip_no_request", "start");

        CommitEvent e = new CommitEvent();
        e.setCommitId("cr_ok");
        e.setCreatedAt("t1");
        e.setRequest(null);

        String s = e.toJsonString();
        CommitEvent loaded = CommitEvent.fromJsonString(s);

        assertNotNull(loaded);
        assertEquals("cr_ok", loaded.getCommitId());
        assertEquals("t1", loaded.getCreatedAt());
        assertNull(loaded.getRequest());

        Console.log("CE_UT/json_roundtrip_no_request", "done");
    }

    @Test
    void fromJsonString_returns_null_on_blank_or_non_object() {
        Console.log("CE_UT/fromJsonString_null_cases", "start");

        assertNull(CommitEvent.fromJsonString(null));
        assertNull(CommitEvent.fromJsonString("   "));
        assertNull(CommitEvent.fromJsonString("[]"));
        assertNull(CommitEvent.fromJsonString("\"x\""));
        assertNull(CommitEvent.fromJsonString("123"));

        Console.log("CE_UT/fromJsonString_null_cases", "done");
    }

    @Test
    void fromJson_handles_missing_fields_safely() {
        Console.log("CE_UT/fromJson_missing_fields", "start");

        JsonObject json = new JsonObject();
        CommitEvent loaded = CommitEvent.fromJson(json);

        assertNotNull(loaded);
        assertNull(loaded.getCommitId());
        assertNull(loaded.getCreatedAt());
        assertNull(loaded.getRequest());

        Console.log("CE_UT/fromJson_missing_fields", "done");
    }

    @Test
    void fromJson_ignores_bad_request_shape_without_throwing() {
        Console.log("CE_UT/fromJson_bad_request_shape", "start");

        JsonObject json = new JsonObject();
        json.addProperty("commitId", "cr_x");
        json.addProperty("createdAt", "t1");
        // request is present but not an object -> should be ignored
        json.addProperty("request", "not-an-object");

        CommitEvent loaded = CommitEvent.fromJson(json);

        assertNotNull(loaded);
        assertEquals("cr_x", loaded.getCommitId());
        assertEquals("t1", loaded.getCreatedAt());
        assertNull(loaded.getRequest());

        Console.log("CE_UT/fromJson_bad_request_shape", "done");
    }
}
