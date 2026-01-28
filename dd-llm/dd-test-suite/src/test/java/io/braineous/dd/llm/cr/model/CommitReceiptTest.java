package io.braineous.dd.llm.cr.model;

import ai.braineous.rag.prompt.observe.Console;
import com.google.gson.JsonObject;
import io.braineous.dd.llm.core.model.Why;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CommitReceiptTest {

    @Test
    void toJson_includes_accepted_always_and_trims_optionals() {
        Console.log("CRR_UT/toJson_trims", "start");

        CommitReceipt r = new CommitReceipt();
        r.setCommitId("  cr_123  ");
        r.setAccepted(true);
        r.setMessage("  ok  ");

        JsonObject json = r.toJson();

        assertTrue(json.has("accepted"));
        assertTrue(json.get("accepted").isJsonPrimitive());
        assertTrue(json.get("accepted").getAsBoolean());

        assertEquals("cr_123", json.get("commitId").getAsString());
        assertEquals("ok", json.get("message").getAsString());

        Console.log("CRR_UT/toJson_trims", "done");
    }

    @Test
    void toJson_omits_blank_commitId_and_blank_message_but_keeps_accepted() {
        Console.log("CRR_UT/toJson_omits_blanks", "start");

        CommitReceipt r = new CommitReceipt();
        r.setCommitId("   ");
        r.setAccepted(false);
        r.setMessage("   ");

        JsonObject json = r.toJson();

        assertTrue(json.has("accepted"));
        assertFalse(json.get("accepted").getAsBoolean());

        assertFalse(json.has("commitId"));
        assertFalse(json.has("message"));

        Console.log("CRR_UT/toJson_omits_blanks", "done");
    }

    @Test
    void json_roundtrip_without_whyCode_still_works() {
        Console.log("CRR_UT/json_roundtrip_no_why", "start");

        CommitReceipt r = new CommitReceipt();
        r.setCommitId("cr_ok");
        r.setAccepted(true);
        r.setWhyCode(null);
        r.setMessage(null);

        String s = r.toJsonString();
        CommitReceipt loaded = CommitReceipt.fromJsonString(s);

        assertNotNull(loaded);
        assertEquals("cr_ok", loaded.getCommitId());
        assertTrue(loaded.isAccepted());
        assertNull(loaded.getWhyCode());
        assertNull(loaded.getMessage());

        Console.log("CRR_UT/json_roundtrip_no_why", "done");
    }

    @Test
    void fromJsonString_returns_null_on_blank_or_non_object() {
        Console.log("CRR_UT/fromJsonString_null_cases", "start");

        assertNull(CommitReceipt.fromJsonString(null));
        assertNull(CommitReceipt.fromJsonString("   "));
        assertNull(CommitReceipt.fromJsonString("[]"));
        assertNull(CommitReceipt.fromJsonString("\"x\""));
        assertNull(CommitReceipt.fromJsonString("123"));

        Console.log("CRR_UT/fromJsonString_null_cases", "done");
    }

    @Test
    void fromJson_handles_missing_fields_safely() {
        Console.log("CRR_UT/fromJson_missing_fields", "start");

        JsonObject json = new JsonObject();
        // accepted omitted -> should remain default false

        CommitReceipt r = CommitReceipt.fromJson(json);

        assertNotNull(r);
        assertFalse(r.isAccepted());
        assertNull(r.getCommitId());
        assertNull(r.getWhyCode());
        assertNull(r.getMessage());

        Console.log("CRR_UT/fromJson_missing_fields", "done");
    }

    @Test
    void fromJson_handles_bad_whyCode_shape_without_throwing() {
        Console.log("CRR_UT/fromJson_bad_whyCode", "start");

        JsonObject json = new JsonObject();
        json.addProperty("accepted", false);

        JsonObject bad = new JsonObject();
        // missing reason/details, or wrong types
        bad.addProperty("reason", 123);
        bad.add("details", null);
        json.add("whyCode", bad);

        CommitReceipt r = CommitReceipt.fromJson(json);

        assertNotNull(r);
        assertFalse(r.isAccepted());
        // parsing whyCode may fail -> should stay null
        assertNull(r.getWhyCode());

        Console.log("CRR_UT/fromJson_bad_whyCode", "done");
    }

    @Test
    void toJson_includes_whyCode_as_object_when_present() {
        Console.log("CRR_UT/toJson_includes_whyCode", "start");

        CommitReceipt r = new CommitReceipt();
        r.setAccepted(false);
        r.setWhyCode(new Why("SYSTEM_FAIL", "mongo down"));

        JsonObject json = r.toJson();

        assertTrue(json.has("accepted"));
        assertFalse(json.get("accepted").getAsBoolean());

        assertTrue(json.has("whyCode"));
        assertTrue(json.get("whyCode").isJsonObject());

        JsonObject wc = json.get("whyCode").getAsJsonObject();
        assertTrue(wc.has("reason"));
        assertTrue(wc.has("details"));
        assertEquals("SYSTEM_FAIL", wc.get("reason").getAsString());
        assertEquals("mongo down", wc.get("details").getAsString());

        Console.log("CRR_UT/toJson_includes_whyCode", "done");
    }

    @Test
    void json_roundtrip_with_whyCode_and_message() {
        Console.log("CRR_UT/json_roundtrip_with_why", "start");

        CommitReceipt r = new CommitReceipt();
        r.setCommitId(" cr_aaa ");
        r.setAccepted(false);
        r.setMessage("  failed  ");
        r.setWhyCode(new Why("VALIDATION_FAIL", "queryKind missing"));

        String s = r.toJsonString();
        CommitReceipt loaded = CommitReceipt.fromJsonString(s);

        assertNotNull(loaded);
        assertEquals("cr_aaa", loaded.getCommitId());
        assertFalse(loaded.isAccepted());
        assertEquals("failed", loaded.getMessage());

        assertNotNull(loaded.getWhyCode());
        assertEquals("VALIDATION_FAIL", loaded.getWhyCode().getReason());
        assertEquals("queryKind missing", loaded.getWhyCode().getDetails());

        Console.log("CRR_UT/json_roundtrip_with_why", "done");
    }

}
