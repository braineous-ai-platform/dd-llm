package io.braineous.dd.llm.cr.model;

import ai.braineous.rag.prompt.observe.Console;
import com.google.gson.JsonObject;
import io.braineous.dd.llm.cr.model.CommitRequest;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
    void safeDecision_trims_and_nulls_empty() {
        Console.log("CR_UT/safeDecision", "start");

        CommitRequest r = new CommitRequest();

        r.setDecision(null);
        assertNull(r.safeDecision());

        r.setDecision(" ");
        assertNull(r.safeDecision());

        r.setDecision("  ALLOW  ");
        assertEquals("ALLOW", r.safeDecision());

        Console.log("CR_UT/safeDecision", "done");
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
        r.setDecision("  ALLOW ");
        r.setActor("  sohil ");
        r.setRequestId("  rid-1  ");
        r.setNotes(Arrays.asList("  a  ", "   ", null, "b"));

        JsonObject payload = new JsonObject();
        payload.addProperty("x", "  y  ");
        r.setPayload(payload);

        JsonObject json = r.toJson();

        assertEquals("user.search", json.get("queryKind").getAsString());
        assertEquals("v1", json.get("catalogVersion").getAsString());
        assertEquals("ALLOW", json.get("decision").getAsString());
        assertEquals("sohil", json.get("actor").getAsString());
        assertEquals("rid-1", json.get("requestId").getAsString());

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
        r.setDecision(" ALLOW ");
        r.setActor(" sohil ");
        r.setRequestId(" rid ");

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
        assertEquals("ALLOW", loaded.safeDecision());
        assertEquals("sohil", loaded.safeActor());
        assertEquals("rid", loaded.safeRequestId());

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
    void canonicalCommitString_is_stable_and_ordered() {
        Console.log("CR_UT/canonicalCommitString_contract", "start");

        CommitRequest r = new CommitRequest();
        r.setQueryKind("  Q  ");
        r.setCatalogVersion("  V  ");
        r.setDecision("  ALLOW  ");
        r.setRequestId("  RID  ");

        JsonObject payload = new JsonObject();
        payload.addProperty("b", 2);
        payload.addProperty("a", 1);
        r.setPayload(payload);

        String c = CommitRequest.canonicalCommitString(r);

        assertTrue(c.startsWith("queryKind=Q|catalogVersion=V|decision=ALLOW|requestId=RID|payload="));
        assertTrue(c.contains("\"a\":1"));
        assertTrue(c.contains("\"b\":2"));

        Console.log("CR_UT/canonicalCommitString_contract", "done");
    }

    @Test
    void computeCommitId_is_deterministic_for_same_request() {
        Console.log("CR_UT/computeCommitId_deterministic", "start");

        CommitRequest r1 = new CommitRequest();
        r1.setQueryKind("Q");
        r1.setCatalogVersion("V");
        r1.setDecision("ALLOW");
        r1.setRequestId("RID");

        JsonObject p1 = new JsonObject();
        p1.addProperty("a", 1);
        p1.addProperty("b", 2);
        r1.setPayload(p1);

        CommitRequest r2 = new CommitRequest();
        r2.setQueryKind(" Q ");
        r2.setCatalogVersion(" V ");
        r2.setDecision(" ALLOW ");
        r2.setRequestId(" RID ");

        JsonObject p2 = new JsonObject();
        p2.addProperty("b", 2);
        p2.addProperty("a", 1);
        r2.setPayload(p2);

        String id1 = CommitRequest.computeCommitId(r1);
        String id2 = CommitRequest.computeCommitId(r2);

        assertNotNull(id1);
        assertNotNull(id2);
        assertTrue(id1.startsWith("cr_"));
        assertEquals(id1, id2);

        Console.log("CR_UT/computeCommitId_deterministic", "done");
    }

    @Test
    void computeCommitId_changes_when_payload_changes() {
        Console.log("CR_UT/computeCommitId_payload_change", "start");

        CommitRequest r = new CommitRequest();
        r.setQueryKind("Q");
        r.setCatalogVersion("V");
        r.setDecision("ALLOW");
        r.setRequestId("RID");

        JsonObject p = new JsonObject();
        p.addProperty("a", 1);
        r.setPayload(p);

        String id1 = CommitRequest.computeCommitId(r);

        p.addProperty("a", 2); // mutate payload
        r.setPayload(p);

        String id2 = CommitRequest.computeCommitId(r);

        assertNotEquals(id1, id2);

        Console.log("CR_UT/computeCommitId_payload_change", "done");
    }

    @Test
    void computeCommitId_changes_when_requestId_changes() {
        Console.log("CR_UT/computeCommitId_requestId_change", "start");

        CommitRequest r = new CommitRequest();
        r.setQueryKind("Q");
        r.setCatalogVersion("V");
        r.setDecision("ALLOW");

        JsonObject p = new JsonObject();
        p.addProperty("a", 1);
        r.setPayload(p);

        r.setRequestId("RID-1");
        String id1 = CommitRequest.computeCommitId(r);

        r.setRequestId("RID-2");
        String id2 = CommitRequest.computeCommitId(r);

        assertNotEquals(id1, id2);

        Console.log("CR_UT/computeCommitId_requestId_change", "done");
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
}

