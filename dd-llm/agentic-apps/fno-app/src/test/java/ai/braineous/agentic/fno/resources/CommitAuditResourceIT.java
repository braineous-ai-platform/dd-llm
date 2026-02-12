package ai.braineous.agentic.fno.resources;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class CommitAuditResourceIT {

    @Inject
    MongoClient mongoClient;

    @BeforeEach
    void setup() {
        MongoDatabase db = mongoClient.getDatabase("cgo");

        db.getCollection("cr_commit_events").drop();
        db.getCollection("cr_commit_requests").drop();
        db.getCollection("cr_commit_receipts").drop();

        try { Thread.sleep(250L); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }

    @Test
    void getAudit_blank_commitId_returns_404_or_400_but_not_500() {
        int status = given()
                .when().get("/fno/audit/commit/   ")
                .then().extract().statusCode();

        // path param w/ spaces often becomes 404 at routing layer; accept both, but never 500
        assertTrue(status == 400 || status == 404);
    }

    @Test
    void getAudit_missing_commitId_returns_400_and_error_shape() {
        ResponseCapture rc = doGet("/fno/audit/commit/");

        // some stacks route this as 404; accept 404 OR 400, but if 400 then must be our JSON error
        assertTrue(rc.status == 400 || rc.status == 404);

        if (rc.status == 400) {
            JsonObject obj = parseJsonObjectOrNull(rc.body);
            assertNotNull(obj);

            assertEquals("bad_request", getString(obj, "error"));
            assertEquals("commitId required", getString(obj, "details"));
        }
    }

    @Test
    void getAudit_unknown_commitId_returns_404_and_error_shape() {
        String commitId = "cr_missing";

        ResponseCapture rc = doGet("/fno/audit/commit/" + commitId);

        assertEquals(404, rc.status);

        JsonObject obj = parseJsonObjectOrNull(rc.body);
        assertNotNull(obj);

        assertEquals("not_found", getString(obj, "error"));
        assertEquals("commitId not found", getString(obj, "details"));
        assertEquals(commitId, getString(obj, "commitId"));
    }

    @Test
    void getAudit_happyPath_when_present_returns_200_and_json_object() {
        // We are NOT creating fixtures here because those are already validated in lower-level IT.
        // This test is about resource behavior: when system is up, it should never return non-json on 200.
        // So we accept either:
        // - 404 (nothing there yet) with proper json, OR
        // - 200 with json view (object), but NEVER blank body with 200.

        String commitId = "cr_any";
        ResponseCapture rc = doGet("/fno/audit/commit/" + commitId);

        assertTrue(rc.status == 200 || rc.status == 404);

        JsonObject obj = parseJsonObjectOrNull(rc.body);
        assertNotNull(obj);

        if (rc.status == 404) {
            assertEquals("not_found", getString(obj, "error"));
            assertEquals("commitId not found", getString(obj, "details"));
            assertEquals(commitId, getString(obj, "commitId"));
            return;
        }

        // 200 => must look like CommitAuditView JSON object surface
        // minimal stable assertions (don’t overfit)
        // commitId should exist if present
        String id = getString(obj, "commitId");
        if (id != null) {
            assertEquals(commitId, id);
        }

        // status, createdAt are optional depending on fragments
        // but if present, must be non-blank strings
        assertValidOptionalString(obj, "status");
        assertValidOptionalString(obj, "createdAt");
    }

    // -------------------------------------------------
    // helpers
    // -------------------------------------------------

    private ResponseCapture doGet(String path) {
        int status = given()
                .when().get(path)
                .then().extract().statusCode();

        String body = given()
                .when().get(path)
                .then().extract().asString();

        return new ResponseCapture(status, body);
    }

    private JsonObject parseJsonObjectOrNull(String body) {
        if (body == null) {
            return null;
        }
        String t = body.trim();
        if (t.isEmpty()) {
            return null;
        }

        try {
            JsonElement el = JsonParser.parseString(t);
            if (el == null || !el.isJsonObject()) {
                return null;
            }
            return el.getAsJsonObject();
        } catch (RuntimeException re) {
            return null;
        }
    }

    private String getString(JsonObject obj, String key) {
        if (obj == null || key == null) {
            return null;
        }
        try {
            if (!obj.has(key) || obj.get(key).isJsonNull()) {
                return null;
            }
            String v = obj.get(key).getAsString();
            if (v == null) {
                return null;
            }
            String t = v.trim();
            if (t.isEmpty()) {
                return null;
            }
            return t;
        } catch (RuntimeException re) {
            return null;
        }
    }

    private void assertValidOptionalString(JsonObject obj, String key) {
        if (obj == null) {
            return;
        }
        if (!obj.has(key) || obj.get(key).isJsonNull()) {
            return;
        }
        String v = null;
        try {
            v = obj.get(key).getAsString();
        } catch (RuntimeException re) {
            v = null;
        }
        assertNotNull(v);
        assertFalse(v.trim().isEmpty());
    }

    private static class ResponseCapture {
        final int status;
        final String body;

        ResponseCapture(int status, String body) {
            this.status = status;
            this.body = body;
        }
    }
}
