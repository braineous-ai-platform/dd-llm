package ai.braineous.agentic.fno.resources;

import ai.braineous.rag.prompt.observe.Console;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class QueryTests {

    @Test
    void fnoQuery_returns200_andStableResponseShape() {

        Console.log("test", "fnoQuery_returns200_andStableResponseShape");

        JsonObject payload = new JsonObject();

        // META (PromptInput expects: version, query_kind, description)
        JsonObject meta = new JsonObject();
        meta.addProperty("version", "v1");
        meta.addProperty("query_kind", "validate_task");
        meta.addProperty("description", "e2e smoke");
        payload.add("meta", meta);

        // TASK (ValidateTask expects: description?, factId)
        JsonObject task = new JsonObject();
        task.addProperty("description", "smoke");
        task.addProperty("factId", "FACT-123"); // REQUIRED by your orchestrator
        payload.add("task", task);

        // CONTEXT (PromptInput expects key: context; GraphContext.fromJson expects: nodes as JsonObject)
        JsonObject context = new JsonObject();
        context.add("nodes", new JsonObject()); // minimal valid shape
        payload.add("context", context);

        Console.log("requestBody", payload);

        io.restassured.response.Response resp =
                io.restassured.RestAssured
                        .given()
                        .contentType("application/json")
                        .body(payload.toString())
                        .when()
                        .post("/fno/query")
                        .andReturn();

        String body = resp.getBody().asString();
        Console.log("status", String.valueOf(resp.getStatusCode()));
        Console.log("responseBody", body);

        // Assert 200 (if it fails, the Console.log above already shows why)
        org.junit.jupiter.api.Assertions.assertEquals(200, resp.getStatusCode(), body);

        JsonObject out = JsonParser.parseString(body).getAsJsonObject();
        org.junit.jupiter.api.Assertions.assertTrue(out.has("rawResponse"));
        org.junit.jupiter.api.Assertions.assertTrue(out.has("promptValidation"));
        org.junit.jupiter.api.Assertions.assertTrue(out.has("llmResponseValidation"));
        org.junit.jupiter.api.Assertions.assertTrue(out.has("domainValidation"));
    }

    @Test
    void fnoQuery_invalidJson_returns400_andErrorShape() {

        Console.log("test", "fnoQuery_invalidJson_returns400_andErrorShape");

        String badJson = "{ \"meta\": "; // intentionally broken
        Console.log("requestBody", badJson);

        io.restassured.response.Response resp =
                io.restassured.RestAssured
                        .given()
                        .contentType("application/json")
                        .body(badJson)
                        .when()
                        .post("/fno/query")
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
    void fnoQuery_missingFactId_returns500_andErrorShape() {

        Console.log("test", "fnoQuery_missingFactId_returns500_andErrorShape");

        JsonObject payload = new JsonObject();

        JsonObject meta = new JsonObject();
        meta.addProperty("version", "v1");
        meta.addProperty("query_kind", "validate_task");
        meta.addProperty("description", "e2e smoke");
        payload.add("meta", meta);

        JsonObject task = new JsonObject();
        task.addProperty("description", "smoke");
        // factId intentionally missing
        payload.add("task", task);

        JsonObject context = new JsonObject();
        context.add("nodes", new JsonObject());
        payload.add("context", context);

        Console.log("requestBody", payload);

        io.restassured.response.Response resp =
                io.restassured.RestAssured
                        .given()
                        .contentType("application/json")
                        .body(payload.toString())
                        .when()
                        .post("/fno/query")
                        .andReturn();

        String body = resp.getBody().asString();
        Console.log("status", String.valueOf(resp.getStatusCode()));
        Console.log("responseBody", body);

        org.junit.jupiter.api.Assertions.assertEquals(500, resp.getStatusCode(), body);

        JsonObject out = JsonParser.parseString(body).getAsJsonObject();
        org.junit.jupiter.api.Assertions.assertTrue(out.has("error"));
        org.junit.jupiter.api.Assertions.assertTrue(out.has("details"));
    }

    @Test
    void fnoQuery_unknownQueryKind_stillReturns200_andStableShape() {

        Console.log("test", "fnoQuery_unknownQueryKind_stillReturns200_andStableShape");

        JsonObject payload = new JsonObject();

        JsonObject meta = new JsonObject();
        meta.addProperty("version", "v1");
        meta.addProperty("query_kind", "UNKNOWN_KIND"); // intentionally wrong
        meta.addProperty("description", "e2e tolerance");
        payload.add("meta", meta);

        JsonObject task = new JsonObject();
        task.addProperty("description", "smoke");
        task.addProperty("factId", "FACT-456");
        payload.add("task", task);

        JsonObject context = new JsonObject();
        context.add("nodes", new JsonObject());
        payload.add("context", context);

        Console.log("requestBody", payload);

        io.restassured.response.Response resp =
                io.restassured.RestAssured
                        .given()
                        .contentType("application/json")
                        .body(payload.toString())
                        .when()
                        .post("/fno/query")
                        .andReturn();

        String body = resp.getBody().asString();
        Console.log("status", String.valueOf(resp.getStatusCode()));
        Console.log("responseBody", body);

        org.junit.jupiter.api.Assertions.assertEquals(200, resp.getStatusCode(), body);

        JsonObject out = JsonParser.parseString(body).getAsJsonObject();
        org.junit.jupiter.api.Assertions.assertTrue(out.has("rawResponse"));
        org.junit.jupiter.api.Assertions.assertTrue(out.has("promptValidation"));
        org.junit.jupiter.api.Assertions.assertTrue(out.has("llmResponseValidation"));
        org.junit.jupiter.api.Assertions.assertTrue(out.has("domainValidation"));
    }

    @Test
    void fnoQuery_missingContext_returns500_andErrorShape() {

        Console.log("test", "fnoQuery_missingContext_returns500_andErrorShape");

        JsonObject payload = new JsonObject();

        JsonObject meta = new JsonObject();
        meta.addProperty("version", "v1");
        meta.addProperty("query_kind", "validate_task");
        meta.addProperty("description", "missing context");
        payload.add("meta", meta);

        JsonObject task = new JsonObject();
        task.addProperty("description", "smoke");
        task.addProperty("factId", "FACT-789");
        payload.add("task", task);

        // NOTE: context intentionally omitted
        Console.log("requestBody", payload);

        io.restassured.response.Response resp =
                io.restassured.RestAssured
                        .given()
                        .contentType("application/json")
                        .body(payload.toString())
                        .when()
                        .post("/fno/query")
                        .andReturn();

        String body = resp.getBody().asString();
        Console.log("status", String.valueOf(resp.getStatusCode()));
        Console.log("responseBody", body);

        org.junit.jupiter.api.Assertions.assertEquals(500, resp.getStatusCode(), body);

        JsonObject out = JsonParser.parseString(body).getAsJsonObject();
        org.junit.jupiter.api.Assertions.assertTrue(out.has("error"));
        org.junit.jupiter.api.Assertions.assertTrue(out.has("details"));
    }

    @Test
    void fnoQuery_getMethod_returns405() {

        Console.log("test", "fnoQuery_getMethod_returns405");

        io.restassured.response.Response resp =
                io.restassured.RestAssured
                        .when()
                        .get("/fno/query")
                        .andReturn();

        Console.log("status", String.valueOf(resp.getStatusCode()));
        Console.log("responseBody", resp.getBody().asString());

        org.junit.jupiter.api.Assertions.assertEquals(405, resp.getStatusCode());
    }

    @Test
    void fnoQuery_missingContentType_returns415() {

        Console.log("test", "fnoQuery_missingContentType_returns415");

        JsonObject payload = new JsonObject();

        JsonObject meta = new JsonObject();
        meta.addProperty("version", "v1");
        meta.addProperty("query_kind", "validate_task");
        meta.addProperty("description", "strict ingress");
        payload.add("meta", meta);

        JsonObject task = new JsonObject();
        task.addProperty("description", "smoke");
        task.addProperty("factId", "FACT-999");
        payload.add("task", task);

        JsonObject context = new JsonObject();
        context.add("nodes", new JsonObject());
        payload.add("context", context);

        Console.log("requestBody", payload);

        io.restassured.response.Response resp =
                io.restassured.RestAssured
                        .given()
                        .body(payload.toString()) // intentionally NO content-type
                        .when()
                        .post("/fno/query")
                        .andReturn();

        Console.log("status", String.valueOf(resp.getStatusCode()));
        Console.log("responseBody", resp.getBody().asString());

        org.junit.jupiter.api.Assertions.assertEquals(415, resp.getStatusCode());
    }

    @Test
    void fnoQuery_extraFields_areIgnored_andStill200() {

        Console.log("test", "fnoQuery_extraFields_areIgnored_andStill200");

        JsonObject payload = new JsonObject();

        JsonObject meta = new JsonObject();
        meta.addProperty("version", "v1");
        meta.addProperty("query_kind", "validate_task");
        meta.addProperty("description", "extra fields test");
        meta.addProperty("random_meta", "ignore-me"); // extra
        payload.add("meta", meta);

        JsonObject task = new JsonObject();
        task.addProperty("description", "smoke");
        task.addProperty("factId", "FACT-1000");
        task.addProperty("foo", "bar"); // extra
        payload.add("task", task);

        JsonObject context = new JsonObject();
        context.add("nodes", new JsonObject());
        context.add("edges", new JsonObject()); // even if unused
        context.addProperty("junk", 123);       // extra
        payload.add("context", context);

        payload.addProperty("topLevelGarbage", true); // extra

        Console.log("requestBody", payload);

        io.restassured.response.Response resp =
                io.restassured.RestAssured
                        .given()
                        .contentType("application/json")
                        .body(payload.toString())
                        .when()
                        .post("/fno/query")
                        .andReturn();

        Console.log("status", String.valueOf(resp.getStatusCode()));
        Console.log("responseBody", resp.getBody().asString());

        org.junit.jupiter.api.Assertions.assertEquals(200, resp.getStatusCode());

        JsonObject out = JsonParser.parseString(resp.getBody().asString()).getAsJsonObject();
        org.junit.jupiter.api.Assertions.assertTrue(out.has("rawResponse"));
    }

    @Test
    void fnoQuery_on200_responseIsValidJsonObject() {

        Console.log("test", "fnoQuery_on200_responseIsValidJsonObject");

        JsonObject payload = new JsonObject();

        JsonObject meta = new JsonObject();
        meta.addProperty("version", "v1");
        meta.addProperty("query_kind", "validate_task");
        meta.addProperty("description", "json response contract");
        payload.add("meta", meta);

        JsonObject task = new JsonObject();
        task.addProperty("description", "smoke");
        task.addProperty("factId", "FACT-2000");
        payload.add("task", task);

        JsonObject context = new JsonObject();
        context.add("nodes", new JsonObject());
        payload.add("context", context);

        Console.log("requestBody", payload);

        io.restassured.response.Response resp =
                io.restassured.RestAssured
                        .given()
                        .contentType("application/json")
                        .body(payload.toString())
                        .when()
                        .post("/fno/query")
                        .andReturn();

        String body = resp.getBody().asString();
        Console.log("status", String.valueOf(resp.getStatusCode()));
        Console.log("responseBody", body);

        org.junit.jupiter.api.Assertions.assertEquals(200, resp.getStatusCode());

        // Contract: must be parseable JSON object
        JsonObject out = JsonParser.parseString(body).getAsJsonObject();
        org.junit.jupiter.api.Assertions.assertTrue(out.isJsonObject());
    }

    @Test
    void fnoQuery_on200_alwaysContainsAllTopLevelKeys() {

        Console.log("test", "fnoQuery_on200_alwaysContainsAllTopLevelKeys");

        JsonObject payload = new JsonObject();

        JsonObject meta = new JsonObject();
        meta.addProperty("version", "v1");
        meta.addProperty("query_kind", "validate_task");
        meta.addProperty("description", "required keys contract");
        payload.add("meta", meta);

        JsonObject task = new JsonObject();
        task.addProperty("description", "smoke");
        task.addProperty("factId", "FACT-3000");
        payload.add("task", task);

        JsonObject context = new JsonObject();
        context.add("nodes", new JsonObject());
        payload.add("context", context);

        Console.log("requestBody", payload);

        io.restassured.response.Response resp =
                io.restassured.RestAssured
                        .given()
                        .contentType("application/json")
                        .body(payload.toString())
                        .when()
                        .post("/fno/query")
                        .andReturn();

        String body = resp.getBody().asString();
        Console.log("status", String.valueOf(resp.getStatusCode()));
        Console.log("responseBody", body);

        org.junit.jupiter.api.Assertions.assertEquals(200, resp.getStatusCode());

        JsonObject out = JsonParser.parseString(body).getAsJsonObject();

        org.junit.jupiter.api.Assertions.assertTrue(out.has("rawResponse"));
        org.junit.jupiter.api.Assertions.assertTrue(out.has("promptValidation"));
        org.junit.jupiter.api.Assertions.assertTrue(out.has("llmResponseValidation"));
        org.junit.jupiter.api.Assertions.assertTrue(out.has("domainValidation"));
    }

    @Test
    void fnoQuery_sameRequestTwice_producesStableResponseShape() {

        Console.log("test", "fnoQuery_sameRequestTwice_producesStableResponseShape");

        JsonObject payload = new JsonObject();

        JsonObject meta = new JsonObject();
        meta.addProperty("version", "v1");
        meta.addProperty("query_kind", "validate_task");
        meta.addProperty("description", "determinism check");
        payload.add("meta", meta);

        JsonObject task = new JsonObject();
        task.addProperty("description", "smoke");
        task.addProperty("factId", "FACT-4000");
        payload.add("task", task);

        JsonObject context = new JsonObject();
        context.add("nodes", new JsonObject());
        payload.add("context", context);

        Console.log("requestBody", payload);

        io.restassured.response.Response r1 =
                io.restassured.RestAssured
                        .given()
                        .contentType("application/json")
                        .body(payload.toString())
                        .when()
                        .post("/fno/query")
                        .andReturn();

        io.restassured.response.Response r2 =
                io.restassured.RestAssured
                        .given()
                        .contentType("application/json")
                        .body(payload.toString())
                        .when()
                        .post("/fno/query")
                        .andReturn();

        Console.log("status1", String.valueOf(r1.getStatusCode()));
        Console.log("status2", String.valueOf(r2.getStatusCode()));

        org.junit.jupiter.api.Assertions.assertEquals(200, r1.getStatusCode());
        org.junit.jupiter.api.Assertions.assertEquals(200, r2.getStatusCode());

        JsonObject o1 = JsonParser.parseString(r1.getBody().asString()).getAsJsonObject();
        JsonObject o2 = JsonParser.parseString(r2.getBody().asString()).getAsJsonObject();

        // Shape determinism (NOT value determinism)
        org.junit.jupiter.api.Assertions.assertEquals(o1.keySet(), o2.keySet());
    }
}
