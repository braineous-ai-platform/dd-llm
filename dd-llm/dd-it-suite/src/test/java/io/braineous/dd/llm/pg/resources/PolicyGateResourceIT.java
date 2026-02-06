package io.braineous.dd.llm.pg.resources;

import ai.braineous.cgo.history.HistoryRecord;
import ai.braineous.cgo.history.MongoHistoryStore;
import ai.braineous.cgo.history.ScorerResult;
import ai.braineous.rag.prompt.cgo.api.GraphContext;
import ai.braineous.rag.prompt.cgo.api.Meta;
import ai.braineous.rag.prompt.cgo.api.QueryExecution;
import ai.braineous.rag.prompt.cgo.api.ValidateTask;
import ai.braineous.rag.prompt.cgo.query.QueryRequest;
import ai.braineous.rag.prompt.observe.Console;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.client.MongoClient;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class PolicyGateResourceIT {

    @Inject
    MongoClient mongoClient;

    @Inject
    MongoHistoryStore historyStore;

    @BeforeEach
    void setup() {

        awaitMongoReady(mongoClient);

        // hard reset history only (these endpoints depend on history evidence)
        if (this.historyStore != null) {
            this.historyStore.clear();
        }

        Console.log("PolicyGateResourceIT.setup", "cleared history_store");
    }

    @Test
    void getExecutions_happyPath_returns_200_and_contains_seeded_factId() {

        seedHistoryRecord("validate_flight_airports", "Flight:F100");

        String body =
                given()
                        .when().get("/api/v1/policygate/executions/validate_flight_airports")
                        .then().statusCode(200)
                        .extract().asString();

        Console.log("PolicyGateResourceIT.getExecutions_happyPath", body);

        JsonObject o = JsonParser.parseString(body).getAsJsonObject();

        assertTrue(o.has("queryKind"));
        assertEquals("validate_flight_airports", o.get("queryKind").getAsString());

        assertTrue(o.has("executions"));
        assertTrue(o.get("executions").isJsonArray());

        JsonArray arr = o.getAsJsonArray("executions");
        assertTrue(arr.size() >= 1);

        boolean found = false;
        for (int i = 0; i < arr.size(); i++) {

            JsonObject ex = arr.get(i).getAsJsonObject();
            if (ex == null) {
                continue;
            }

            if (!ex.has("request") || !ex.get("request").isJsonObject()) {
                continue;
            }

            JsonObject rq = ex.getAsJsonObject("request");
            if (!rq.has("factId") || rq.get("factId").isJsonNull()) {
                continue;
            }

            String fid = rq.get("factId").getAsString();
            if ("Flight:F100".equals(fid)) {
                found = true;
                break;
            }
        }

        assertTrue(found);
    }

    @Test
    void approve_happyPath_returns_200_ok_true_and_commitId_echo() {

        seedHistoryRecord("validate_flight_airports", "Flight:F100");

        String payload =
                "{"
                        + "\"queryKind\":\"validate_flight_airports\","
                        + "\"commitId\":\"Flight:F100\""
                        + "}";

        String body =
                given()
                        .contentType("application/json")
                        .body(payload)
                        .when().post("/api/v1/policygate/commit/approve")
                        .then().statusCode(200)
                        .extract().asString();

        Console.log("PolicyGateResourceIT.approve_happyPath", body);

        JsonObject o = JsonParser.parseString(body).getAsJsonObject();

        assertTrue(o.has("ok"));
        assertTrue(o.get("ok").getAsBoolean());

        assertTrue(o.has("commitId"));
        assertEquals("Flight:F100", o.get("commitId").getAsString());
    }

    @Test
    void approve_unknownCommitId_returns_422_with_ok_false() {

        seedHistoryRecord("validate_flight_airports", "Flight:F100");

        String payload =
                "{"
                        + "\"queryKind\":\"validate_flight_airports\","
                        + "\"commitId\":\"Flight:DOES_NOT_EXIST\""
                        + "}";

        String body =
                given()
                        .contentType("application/json")
                        .body(payload)
                        .when().post("/api/v1/policygate/commit/approve")
                        .then().statusCode(422)
                        .extract().asString();

        Console.log("PolicyGateResourceIT.approve_unknownCommitId", body);

        JsonObject o = JsonParser.parseString(body).getAsJsonObject();

        assertTrue(o.has("ok"));
        assertTrue(!o.get("ok").getAsBoolean());

        assertTrue(o.has("commitId"));
        assertEquals("Flight:DOES_NOT_EXIST", o.get("commitId").getAsString());

        assertTrue(o.has("why"));
    }

    // ---------------------------------------------------------
    // Seed (history evidence = source of truth)
    // ---------------------------------------------------------

    private void seedHistoryRecord(String queryKind, String factId) {

        Meta meta = new Meta(
                "v1",
                queryKind,
                "seed"
        );

        GraphContext ctx = new GraphContext();

        ValidateTask task = new ValidateTask(queryKind, factId);

        QueryRequest<?> req =
                new QueryRequest<>(meta, ctx, task, factId);

        QueryExecution<?> exec =
                new QueryExecution<>(req);

        ScorerResult scorer = ScorerResult.ok("ok");

        HistoryRecord record =
                new HistoryRecord(exec, scorer);

        this.historyStore.addRecord(record);

        Console.log("PolicyGateResourceIT.seedHistoryRecord",
                "seeded queryKind=" + queryKind + " factId=" + factId);
    }

    // ---------------------------------------------------------
    // mongo readiness (same style as QueryResourceIT)
    // ---------------------------------------------------------

    private void awaitMongoReady(MongoClient mongoClient) {
        long start = System.currentTimeMillis();
        long timeoutMs = 15000L;

        while (true) {
            try {
                mongoClient.getDatabase("cgo").runCommand(new Document("ping", 1));
                return;
            } catch (RuntimeException e) {
                if ((System.currentTimeMillis() - start) > timeoutMs) {
                    throw new RuntimeException("Mongo not ready within " + timeoutMs + "ms", e);
                }
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ie);
                }
            }
        }
    }
}

