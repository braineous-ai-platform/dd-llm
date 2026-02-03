package io.braineous.dd.llm.pg.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

public class PolicyGateResultTest {

    @Test
    public void toJson_includes_ok_and_optional_fields() {

        PolicyGateResult r = new PolicyGateResult();
        r.setOk(true);
        r.setCommitId("c-1");
        r.setWhy("submitted");

        JsonObject j = r.toJson();
        assertNotNull(j);

        assertTrue(j.has("ok"));
        assertTrue(j.get("ok").getAsBoolean());

        assertTrue(j.has("commitId"));
        assertEquals("c-1", j.get("commitId").getAsString());

        assertTrue(j.has("why"));
        assertEquals("submitted", j.get("why").getAsString());
    }

    @Test
    public void toJson_trims_strings_and_writes_nulls() {

        PolicyGateResult r = new PolicyGateResult();
        r.setOk(false);
        r.setCommitId("   ");
        r.setWhy(null);

        JsonObject j = r.toJson();
        assertNotNull(j);

        assertTrue(j.has("ok"));
        assertFalse(j.get("ok").getAsBoolean());

        assertTrue(j.has("commitId"));
        assertTrue(j.get("commitId").isJsonNull());

        assertTrue(j.has("why"));
        assertTrue(j.get("why").isJsonNull());
    }

    @Test
    public void fromJsonString_roundtrip_smoke() {

        PolicyGateResult r = PolicyGateResult.ok("c-9", "already submitted");

        String json = r.toJsonString();
        assertNotNull(json);
        assertFalse(json.trim().isEmpty());

        PolicyGateResult r2 = PolicyGateResult.fromJsonString(json);
        assertNotNull(r2);

        assertTrue(r2.isOk());
        assertEquals("c-9", r2.safeCommitId());
        assertEquals("already submitted", r2.safeWhy());
    }

    @Test
    public void fromJson_tolerates_missing_fields() {

        JsonObject j = new JsonObject();
        // no ok/why/commitId

        PolicyGateResult r = PolicyGateResult.fromJson(j);
        assertNotNull(r);

        // ok defaults to false
        assertFalse(r.isOk());
        assertNull(r.safeCommitId());
        assertNull(r.safeWhy());
    }

    @Test
    public void fromJsonString_returns_null_for_bad_inputs() {

        assertNull(PolicyGateResult.fromJsonString(null));
        assertNull(PolicyGateResult.fromJsonString(""));
        assertNull(PolicyGateResult.fromJsonString("   "));
        assertNull(PolicyGateResult.fromJsonString("not-json"));
        assertNull(PolicyGateResult.fromJsonString("[]"));
    }

    @Test
    public void factories_build_expected_objects() {

        PolicyGateResult ok = PolicyGateResult.ok("c-1", "submitted");
        assertTrue(ok.isOk());
        assertEquals("c-1", ok.safeCommitId());
        assertEquals("submitted", ok.safeWhy());

        PolicyGateResult fail = PolicyGateResult.fail("c-2", "unknown");
        assertFalse(fail.isOk());
        assertEquals("c-2", fail.safeCommitId());
        assertEquals("unknown", fail.safeWhy());
    }
}
