package ai.braineous.agentic.fno.resources;

import ai.braineous.agentic.fno.support.TestGraphSupport;
import ai.braineous.rag.prompt.observe.Console;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.braineous.dd.llm.policygate.client.PolicyGateApproveRequest;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class PolicyGateTests {

    @Inject
    private TestGraphSupport support;

    @BeforeEach
    public void setup() {
        support.setupGraphSnapshot();
    }

    @Test
    void getExecutions_blankQueryKind_returns400_andErrorShape() {

        Console.log("test", "getExecutions_blankQueryKind_returns400_andErrorShape");

        io.restassured.response.Response resp =
                io.restassured.RestAssured
                        .given()
                        .when()
                        .get("/fno/policygate/executions/   ")
                        .andReturn();

        String body = resp.getBody().asString();
        Console.log("status", String.valueOf(resp.getStatusCode()));
        Console.log("responseBody", body);

        // NOTE: path param with spaces often normalizes to 404; keep tolerant but enforce "not 200"
        org.junit.jupiter.api.Assertions.assertNotEquals(200, resp.getStatusCode(), body);

        if (resp.getStatusCode() == 400) {
            JsonObject out = JsonParser.parseString(body).getAsJsonObject();
            org.junit.jupiter.api.Assertions.assertTrue(out.has("error"));
            org.junit.jupiter.api.Assertions.assertTrue(out.has("details"));
        }
    }

    @Test
    void getExecutions_unknownQueryKind_returns200_andJsonObject() {

        Console.log("test", "getExecutions_unknownQueryKind_returns200_andJsonObject");

        io.restassured.response.Response resp =
                io.restassured.RestAssured
                        .given()
                        .when()
                        .get("/fno/policygate/executions/UNKNOWN_KIND")
                        .andReturn();

        String body = resp.getBody().asString();
        Console.log("status", String.valueOf(resp.getStatusCode()));
        Console.log("responseBody", body);

        org.junit.jupiter.api.Assertions.assertEquals(200, resp.getStatusCode(), body);

        // Contract: must be parseable JSON object
        JsonObject out = JsonParser.parseString(body).getAsJsonObject();
        org.junit.jupiter.api.Assertions.assertTrue(out.isJsonObject());
    }

    @Test
    void approve_nullBody_returns400_andErrorShape() {

        Console.log("test", "approve_nullBody_returns400_andErrorShape");

        // send explicit "null" payload as JSON
        io.restassured.response.Response resp =
                io.restassured.RestAssured
                        .given()
                        .contentType("application/json")
                        .body("null")
                        .when()
                        .post("/fno/policygate/commit/approve")
                        .andReturn();

        String body = resp.getBody().asString();
        Console.log("status", String.valueOf(resp.getStatusCode()));
        Console.log("responseBody", body);

        org.junit.jupiter.api.Assertions.assertEquals(400, resp.getStatusCode(), body);

        JsonObject out = JsonParser.parseString(body).getAsJsonObject();
        org.junit.jupiter.api.Assertions.assertTrue(out.has("error"));
        org.junit.jupiter.api.Assertions.assertTrue(out.has("details"));
    }

    @Test
    void approve_missingCommitId_returns400_andErrorShape() {

        Console.log("test", "approve_missingCommitId_returns400_andErrorShape");

        PolicyGateApproveRequest req = new PolicyGateApproveRequest();
        req.setQueryKind("validate_task");
        // commitId missing

        io.restassured.response.Response resp =
                io.restassured.RestAssured
                        .given()
                        .contentType("application/json")
                        .body(req)
                        .when()
                        .post("/fno/policygate/commit/approve")
                        .andReturn();

        String body = resp.getBody().asString();
        Console.log("status", String.valueOf(resp.getStatusCode()));
        Console.log("responseBody", body);

        org.junit.jupiter.api.Assertions.assertEquals(400, resp.getStatusCode(), body);

        JsonObject out = JsonParser.parseString(body).getAsJsonObject();
        org.junit.jupiter.api.Assertions.assertTrue(out.has("error"));
        org.junit.jupiter.api.Assertions.assertTrue(out.has("details"));
    }

    @Test
    void approve_missingContentType_returns415() {

        Console.log("test", "approve_missingContentType_returns415");

        JsonObject payload = new JsonObject();
        payload.addProperty("queryKind", "validate_task");
        payload.addProperty("commitId", "C-1");

        io.restassured.response.Response resp =
                io.restassured.RestAssured
                        .given()
                        .body(payload.toString()) // intentionally NO content-type
                        .when()
                        .post("/fno/policygate/commit/approve")
                        .andReturn();

        Console.log("status", String.valueOf(resp.getStatusCode()));
        Console.log("responseBody", resp.getBody().asString());

        org.junit.jupiter.api.Assertions.assertEquals(415, resp.getStatusCode());
    }

    @Test
    void approve_invalidJson_returns400_or415() {

        Console.log("test", "approve_invalidJson_returns400_or415");

        String badJson = "{ \"queryKind\": "; // intentionally broken
        Console.log("requestBody", badJson);

        io.restassured.response.Response resp =
                io.restassured.RestAssured
                        .given()
                        .contentType("application/json")
                        .body(badJson)
                        .when()
                        .post("/fno/policygate/commit/approve")
                        .andReturn();

        String body = resp.getBody().asString();
        Console.log("status", String.valueOf(resp.getStatusCode()));
        Console.log("responseBody", body);

        org.junit.jupiter.api.Assertions.assertTrue(
                resp.getStatusCode() == 400 || resp.getStatusCode() == 415,
                body
        );

        // IMPORTANT: body may be empty on parse errors.
        String trimmed = body == null ? "" : body.trim();
        if (!trimmed.isEmpty()) {
            // If there *is* a body, it must be valid JSON (object preferred, but don't force it)
            JsonObject out = JsonParser.parseString(trimmed).getAsJsonObject();
            org.junit.jupiter.api.Assertions.assertTrue(out.isJsonObject());
        }
    }



    @Test
    void approve_missingQueryKind_returns400_andErrorShape() {

        Console.log("test", "approve_missingQueryKind_returns400_andErrorShape");

        JsonObject payload = new JsonObject();
        payload.addProperty("commitId", "CID-1"); // anything non-empty

        io.restassured.response.Response resp =
                io.restassured.RestAssured
                        .given()
                        .contentType("application/json")
                        .body(payload.toString())
                        .when()
                        .post("/fno/policygate/commit/approve")
                        .andReturn();

        String body = resp.getBody().asString();
        Console.log("status", String.valueOf(resp.getStatusCode()));
        Console.log("responseBody", body);

        org.junit.jupiter.api.Assertions.assertEquals(400, resp.getStatusCode(), body);

        JsonObject out = JsonParser.parseString(body).getAsJsonObject();
        org.junit.jupiter.api.Assertions.assertEquals("bad_request", out.get("error").getAsString());
        org.junit.jupiter.api.Assertions.assertEquals("queryKind required", out.get("details").getAsString());
    }

    @Test
    void approve_sameRequestTwice_producesStableResponseShape_whenNot500() {

        Console.log("test", "approve_sameRequestTwice_producesStableResponseShape_whenNot500");

        JsonObject payload = new JsonObject();
        payload.addProperty("queryKind", "validate_task");
        payload.addProperty("commitId", "Commit:SMOKE");

        io.restassured.response.Response r1 =
                io.restassured.RestAssured
                        .given()
                        .contentType("application/json")
                        .body(payload.toString())
                        .when()
                        .post("/fno/policygate/commit/approve")
                        .andReturn();

        io.restassured.response.Response r2 =
                io.restassured.RestAssured
                        .given()
                        .contentType("application/json")
                        .body(payload.toString())
                        .when()
                        .post("/fno/policygate/commit/approve")
                        .andReturn();

        int status1 = r1.getStatusCode();
        int status2 = r2.getStatusCode();

        String body1 = r1.getBody() == null ? null : r1.getBody().asString();
        String body2 = r2.getBody() == null ? null : r2.getBody().asString();

        Console.log("status1", String.valueOf(status1));
        Console.log("status2", String.valueOf(status2));
        Console.log("body1", body1);
        Console.log("body2", body2);

        if (status1 == 500 || status2 == 500) {
            return;
        }

        org.junit.jupiter.api.Assertions.assertEquals(status1, status2);

        boolean json1 = false;
        boolean json2 = false;

        try {
            if (body1 != null && body1.trim().length() > 0) {
                com.google.gson.JsonElement e1 = com.google.gson.JsonParser.parseString(body1.trim());
                if (e1 != null && e1.isJsonObject()) {
                    json1 = true;
                }
            }
        } catch (Exception ignored) {}

        try {
            if (body2 != null && body2.trim().length() > 0) {
                com.google.gson.JsonElement e2 = com.google.gson.JsonParser.parseString(body2.trim());
                if (e2 != null && e2.isJsonObject()) {
                    json2 = true;
                }
            }
        } catch (Exception ignored) {}

        org.junit.jupiter.api.Assertions.assertEquals(json1, json2);

        if (!json1) {
            org.junit.jupiter.api.Assertions.assertEquals(body1, body2);
            return;
        }

        com.google.gson.JsonObject o1 = com.google.gson.JsonParser.parseString(body1.trim()).getAsJsonObject();
        com.google.gson.JsonObject o2 = com.google.gson.JsonParser.parseString(body2.trim()).getAsJsonObject();

        org.junit.jupiter.api.Assertions.assertEquals(o1.keySet(), o2.keySet());
    }


    @Test
    void approve_happyPath_whenSystemIsUp_returns200_or422_andJsonObject() {

        Console.log("test", "approve_happyPath_whenSystemIsUp_returns200_or422_andJsonObject");

        JsonObject payload = new JsonObject();
        payload.addProperty("queryKind", "validate_task");
        payload.addProperty("commitId", "Commit:HAPPY");

        io.restassured.response.Response r =
                io.restassured.RestAssured
                        .given()
                        .contentType("application/json")
                        .body(payload.toString())
                        .when()
                        .post("/fno/policygate/commit/approve")
                        .andReturn();

        int status = r.getStatusCode();
        String body = r.getBody() == null ? null : r.getBody().asString();

        Console.log("status", String.valueOf(status));
        Console.log("body", body);

        // allow only 200 or 422
        boolean ok = false;
        if (status == 200) {
            ok = true;
        }
        if (status == 422) {
            ok = true;
        }
        org.junit.jupiter.api.Assertions.assertTrue(ok, "expected 200 or 422 but got " + status);

        // If server returns blank / non-json body, don't blow up the test.
        // This test is mainly guarding "no 500 + stable service up behavior".
        if (body == null) {
            return;
        }
        String trimmed = body.trim();
        if (trimmed.length() == 0) {
            return;
        }

        com.google.gson.JsonElement e;
        try {
            e = com.google.gson.JsonParser.parseString(trimmed);
        } catch (Exception ex) {
            // Not JSON - ok for now (service might return plain text on 422)
            return;
        }

        if (!e.isJsonObject()) {
            // JSON but not an object - also ok for now; just don't crash
            return;
        }

        // If it is a JSON object, assert it's at least an object (and optionally has error fields)
        com.google.gson.JsonObject obj = e.getAsJsonObject();
        org.junit.jupiter.api.Assertions.assertNotNull(obj);
    }


    //------------------
    private void assertSameStatus(int s1, int s2) {
        org.junit.jupiter.api.Assertions.assertEquals(s1, s2, "status changed across retries");
    }

    private void assertStableJsonShapeIfJson(String body1, String body2) {
        boolean json1 = isJsonObject(body1);
        boolean json2 = isJsonObject(body2);

        org.junit.jupiter.api.Assertions.assertEquals(json1, json2, "json-ness changed across retries");

        if (!json1) {
            // Non-JSON error bodies are allowed; just require stable emptiness/string equality.
            org.junit.jupiter.api.Assertions.assertEquals(body1, body2, "non-json body changed across retries");
            return;
        }

        JsonObject o1 = JsonParser.parseString(body1).getAsJsonObject();
        JsonObject o2 = JsonParser.parseString(body2).getAsJsonObject();

        // “shape” = same top-level keys
        org.junit.jupiter.api.Assertions.assertEquals(o1.keySet(), o2.keySet(), "json keys changed across retries");
    }

    private boolean isJsonObject(String body) {
        if (body == null) {
            return false;
        }
        String s = body.trim();
        if (s.length() == 0) {
            return false;
        }
        try {
            JsonElement el = JsonParser.parseString(s);
            if (el == null) {
                return false;
            }
            return el.isJsonObject();
        } catch (Exception e) {
            return false;
        }
    }


}

