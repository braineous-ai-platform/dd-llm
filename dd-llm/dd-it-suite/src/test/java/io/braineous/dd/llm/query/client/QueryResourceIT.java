package io.braineous.dd.llm.query.client;

import ai.braineous.rag.prompt.cgo.api.Fact;
import ai.braineous.rag.prompt.models.cgo.graph.GraphBuilder;
import ai.braineous.rag.prompt.observe.Console;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;

import jakarta.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class QueryResourceIT {

    @Inject
    MongoClient mongoClient;

    @BeforeEach
    void setup() {

        awaitMongoReady(mongoClient);

        MongoDatabase db = mongoClient.getDatabase("cgo");

        // GraphStoreMongo defaults:
        // nodes: cgo_nodes
        // edges: cgo_edges
        db.getCollection("cgo_nodes").drop();
        db.getCollection("cgo_edges").drop();

        Console.log("QueryResourceIT.setup", "dropped cgo_nodes + cgo_edges");

        // Seed minimal substrate for happy-path query
        GraphBuilder gb = GraphBuilder.getInstance();

        Fact aus = new Fact("Airport:AUS",
                "{\"id\":\"Airport:AUS\",\"kind\":\"Airport\",\"name\":\"Austin\"}\n");
        Fact dfw = new Fact("Airport:DFW",
                "{\"id\":\"Airport:DFW\",\"kind\":\"Airport\",\"name\":\"Dallas\"}\n");

        gb.addNode(aus);
        gb.addNode(dfw);

        Console.log("QueryResourceIT.setup", "seeded Airport:AUS, Airport:DFW");
    }

    @Test
    void postQuery_happyPath_returns_200_and_ok_true() {

        String payload =
                "{"
                        + "\"adapter\":\"openai\","
                        + "\"queryKind\":\"validate_flight_airports\","
                        + "\"query\":\"Validate that the selected flight has valid departure and arrival airport codes based on the airport nodes in the graph.\","
                        + "\"fact\":\"Airport:AUS\","
                        + "\"relatedFacts\":[\"Airport:DFW\"]"
                        + "}";

        String body =
                given()
                        .contentType("application/json")
                        .body(payload)
                        .when().post("/api/v1/query")
                        .then().statusCode(200)
                        .extract().asString();

        Console.log("QueryResourceIT.postQuery_happyPath", body);

        JsonObject o = JsonParser.parseString(body).getAsJsonObject();

        assertTrue(o.has("ok"));
        assertTrue(o.get("ok").getAsBoolean());

        assertTrue(o.has("requestJson"));
        assertTrue(o.get("requestJson").isJsonObject());

        JsonObject rq = o.get("requestJson").getAsJsonObject();
        assertEquals("Airport:AUS", rq.get("factId").getAsString());

        assertTrue(o.has("queryExecutionJson"));
        assertTrue(o.get("queryExecutionJson").isJsonObject());
    }

    @Test
    void postQuery_invalidAdapter_returns_400_with_error_body() {

        String payload =
                "{"
                        + "\"adapter\":\"nope\","
                        + "\"queryKind\":\"validate_flight_airports\","
                        + "\"query\":\"Validate airports.\","
                        + "\"fact\":\"Airport:AUS\""
                        + "}";

        String body =
                given()
                        .contentType("application/json")
                        .body(payload)
                        .when().post("/api/v1/query")
                        .then().statusCode(400)
                        .extract().asString();

        Console.log("QueryResourceIT.postQuery_invalidAdapter", body);

        JsonObject o = JsonParser.parseString(body).getAsJsonObject();
        assertEquals("invalid adapter", o.get("error").getAsString());
    }

    @Test
    void postQuery_missingFact_returns_400_with_error_body() {

        String payload =
                "{"
                        + "\"adapter\":\"openai\","
                        + "\"queryKind\":\"validate_flight_airports\","
                        + "\"query\":\"Validate airports.\""
                        + "}";

        String body =
                given()
                        .contentType("application/json")
                        .body(payload)
                        .when().post("/api/v1/query")
                        .then().statusCode(400)
                        .extract().asString();

        Console.log("QueryResourceIT.postQuery_missingFact", body);

        JsonObject o = JsonParser.parseString(body).getAsJsonObject();
        assertEquals("fact required", o.get("error").getAsString());
    }

    @Test
    void postQuery_anchorMissing_returns_400_query_failed() {

        // anchor not seeded
        String payload =
                "{"
                        + "\"adapter\":\"openai\","
                        + "\"queryKind\":\"validate_flight_airports\","
                        + "\"query\":\"Validate airports.\","
                        + "\"fact\":\"Airport:SFO\""
                        + "}";

        String body =
                given()
                        .contentType("application/json")
                        .body(payload)
                        .when().post("/api/v1/query")
                        .then().statusCode(400)
                        .extract().asString();

        Console.log("QueryResourceIT.postQuery_anchorMissing", body);

        JsonObject o = JsonParser.parseString(body).getAsJsonObject();
        assertEquals("query failed", o.get("error").getAsString());
    }

    // -------------------------
    // mongo readiness (same style)
    // -------------------------

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

