package ai.braineous.agentic.fno.controllers;

import ai.braineous.agentic.fno.support.TestGraphReset;
import ai.braineous.cgo.config.CGOSystemConfig;
import ai.braineous.cgo.history.MongoHistoryStore;
import ai.braineous.rag.prompt.models.cgo.graph.GraphBuilder;
import ai.braineous.rag.prompt.models.cgo.graph.GraphStoreMongo;
import ai.braineous.rag.prompt.observe.Console;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class IngestionControllerTests {

    private MongoClient testClient;

    @BeforeEach
    public void setup() {

        GraphBuilder.getInstance().clear();

        String uri = CGOSystemConfig.resolveMongoDBUri();
        this.testClient = MongoClients.create(uri);

        MongoDatabase db = testClient.getDatabase("cgo");
        db.getCollection("cgo_nodes").deleteMany(new Document());
        db.getCollection("cgo_edges").deleteMany(new Document());
    }

    @AfterEach
    public void tearDown() {
        if (this.testClient != null) {
            this.testClient.close();
        }
    }



    @Test
    void ingest_accepts_wrapper_object_and_returns_graph_with_edges() {
        String body =
                "{" +
                        "\"flights\":[" +
                        "{\"id\":\"F100\",\"origin\":\"AUS\",\"dest\":\"DFW\",\"dep_utc\":\"2025-10-22T10:00:00Z\",\"arr_utc\":\"2025-10-22T11:10:00Z\"}," +
                        "{\"id\":\"F102\",\"origin\":\"AUS\",\"dest\":\"DFW\",\"dep_utc\":\"2025-10-22T11:30:00Z\",\"arr_utc\":\"2025-10-22T12:40:00Z\"}," +
                        "{\"id\":\"F110\",\"origin\":\"SAT\",\"dest\":\"DFW\",\"dep_utc\":\"2025-10-22T11:10:00Z\",\"arr_utc\":\"2025-10-22T12:15:00Z\"}," +
                        "{\"id\":\"F120\",\"origin\":\"IAH\",\"dest\":\"DFW\",\"dep_utc\":\"2025-10-22T11:20:00Z\",\"arr_utc\":\"2025-10-22T12:25:00Z\"}," +
                        "{\"id\":\"F200\",\"origin\":\"DFW\",\"dest\":\"ORD\",\"dep_utc\":\"2025-10-22T13:30:00Z\",\"arr_utc\":\"2025-10-22T16:50:00Z\"}," +
                        "{\"id\":\"F210\",\"origin\":\"DFW\",\"dest\":\"JFK\",\"dep_utc\":\"2025-10-22T13:20:00Z\",\"arr_utc\":\"2025-10-22T17:10:00Z\"}," +
                        "{\"id\":\"F220\",\"origin\":\"DFW\",\"dest\":\"LAX\",\"dep_utc\":\"2025-10-22T13:45:00Z\",\"arr_utc\":\"2025-10-22T15:20:00Z\"}" +
                        "]" +
                        "}";

        Console.log("test.fno.ingest.wrapper.in", body);

        String resp =
                given()
                        .contentType("application/json")
                        .body(body)
                        .when()
                        .post("/fno/ingest")
                        .then()
                        .statusCode(200)
                        .extract()
                        .asString();

        Console.log("test.fno.ingest.wrapper.out", resp);

        assertNotNull(resp);
        assertFalse(resp.isBlank());

        // minimal controller contract: returns GraphSnapshot string (v1 driver response)
        assertTrue(resp.contains("GraphSnapshot{nodes="), resp);
        assertTrue(resp.contains("Flight:F102"), resp);
        assertTrue(resp.contains("Airport:DFW"), resp);
        assertTrue(resp.contains("edges={"), resp);

        // at least one known edge
        assertTrue(resp.contains("Edge:Flight:F100->Flight:F200"), resp);
    }


    @Test
    void ingest_accepts_top_level_array_and_returns_graph() {
        String body =
                "[" +
                        "{\"id\":\"F100\",\"origin\":\"AUS\",\"dest\":\"DFW\",\"dep_utc\":\"2025-10-22T10:00:00Z\",\"arr_utc\":\"2025-10-22T11:10:00Z\"}," +
                        "{\"id\":\"F200\",\"origin\":\"DFW\",\"dest\":\"ORD\",\"dep_utc\":\"2025-10-22T13:30:00Z\",\"arr_utc\":\"2025-10-22T16:50:00Z\"}" +
                        "]";

        Console.log("test.fno.ingest.array.in", body);

        String resp =
                given()
                        .contentType("application/json")
                        .body(body)
                        .when()
                        .post("/fno/ingest")
                        .then()
                        .statusCode(200)
                        .extract()
                        .asString();

        Console.log("test.fno.ingest.array.out", resp);

        assertNotNull(resp);
        assertFalse(resp.isBlank());

        assertTrue(resp.contains("GraphSnapshot{nodes="), resp);
        assertTrue(resp.contains("Flight:F100"), resp);
        assertTrue(resp.contains("Flight:F200"), resp);
        assertTrue(resp.contains("Airport:DFW"), resp);
    }


    @Test
    void ingest_rejects_invalid_payload_shape_with_400() {
        String body = "{\"item\":\"Book\",\"quantity\":2,\"price\":10.5}";

        Console.log("test.fno.ingest.invalid.in", body);

        String resp =
                given()
                        .contentType("application/json")
                        .body(body)
                        .when()
                        .post("/fno/ingest")
                        .then()
                        .statusCode(400)
                        .extract()
                        .asString();

        Console.log("test.fno.ingest.invalid.out", resp);

        assertNotNull(resp);
        assertTrue(resp.contains("invalid payload") || resp.contains("parse failed"), resp);
    }

    @Test
    void ingest_malformed_json_returns_400() {
        String body = "[{";

        Console.log("test.fno.ingest.malformed.in", body);

        String resp =
                given()
                        .contentType("application/json")
                        .body(body)
                        .when()
                        .post("/fno/ingest")
                        .then()
                        .statusCode(400)
                        .extract()
                        .asString();

        Console.log("test.fno.ingest.malformed.out", resp);

        assertNotNull(resp);
        assertTrue(resp.contains("parse failed"), resp);
    }

    @Test
    void ingest_empty_array_returns_empty_graph_snapshot() {
        String body = "[]";

        Console.log("test.fno.ingest.empty.in", body);

        String resp =
                given()
                        .contentType("application/json")
                        .body(body)
                        .when()
                        .post("/fno/ingest")
                        .then()
                        .statusCode(200)
                        .extract()
                        .asString();

        Console.log("test.fno.ingest.empty.out", resp);

        assertNotNull(resp);
        assertFalse(resp.isBlank());

        // empty input → empty substrate
        assertTrue(resp.contains("GraphSnapshot"), resp);
        assertTrue(resp.contains("nodes={}"), resp);
        assertTrue(resp.contains("edges={}"), resp);
    }

    @Test
    void ingest_mixed_array_ignores_garbage_and_keeps_valid_facts() {
        String body =
                "[" +
                        "{\"id\":\"F102\",\"origin\":\"AUS\",\"dest\":\"DFW\",\"dep_utc\":\"2025-10-22T11:30:00Z\",\"arr_utc\":\"2025-10-22T12:40:00Z\"}," +
                        "{\"item\":\"Book\",\"quantity\":2,\"price\":10.5}" +
                        "]";

        Console.log("test.fno.ingest.mixed.in", body);

        String resp =
                given()
                        .contentType("application/json")
                        .body(body)
                        .when()
                        .post("/fno/ingest")
                        .then()
                        .statusCode(200)
                        .extract()
                        .asString();

        Console.log("test.fno.ingest.mixed.out", resp);

        assertNotNull(resp);
        assertFalse(resp.isBlank());

        // valid facts present
        assertTrue(resp.contains("Flight:F102"), resp);
        assertTrue(resp.contains("Airport:AUS"), resp);
        assertTrue(resp.contains("Airport:DFW"), resp);

        // garbage not promoted to nodes
        assertFalse(resp.contains("Book"), resp);
        assertFalse(resp.contains("quantity"), resp);
        assertFalse(resp.contains("price"), resp);
    }

    @Test
    void ingest_wrapper_with_empty_flights_array_returns_empty_graph() {
        String body = "{\"flights\":[]}";

        Console.log("test.fno.ingest.wrapper.empty.in", body);

        String resp =
                given()
                        .contentType("application/json")
                        .body(body)
                        .when()
                        .post("/fno/ingest")
                        .then()
                        .statusCode(200)
                        .extract()
                        .asString();

        Console.log("test.fno.ingest.wrapper.empty.out", resp);

        assertNotNull(resp);
        assertFalse(resp.isBlank());

        assertTrue(resp.contains("GraphSnapshot"), resp);
        assertTrue(resp.contains("nodes={}"), resp);
        assertTrue(resp.contains("edges={}"), resp);
    }

    @Test
    void ingest_null_flights_field_is_rejected() {
        String body = "{\"flights\":null}";

        Console.log("test.fno.ingest.wrapper.null.in", body);

        String resp =
                given()
                        .contentType("application/json")
                        .body(body)
                        .when()
                        .post("/fno/ingest")
                        .then()
                        .statusCode(400)
                        .extract()
                        .asString();

        Console.log("test.fno.ingest.wrapper.null.out", resp);

        assertNotNull(resp);
        assertTrue(resp.contains("invalid payload") || resp.contains("parse failed"), resp);
    }

    @Test
    void ingest_is_deterministic_for_same_input() {
        String body =
                "[" +
                        "{\"id\":\"F100\",\"origin\":\"AUS\",\"dest\":\"DFW\",\"dep_utc\":\"2025-10-22T10:00:00Z\",\"arr_utc\":\"2025-10-22T11:10:00Z\"}," +
                        "{\"id\":\"F102\",\"origin\":\"AUS\",\"dest\":\"DFW\",\"dep_utc\":\"2025-10-22T11:30:00Z\",\"arr_utc\":\"2025-10-22T12:40:00Z\"}," +
                        "{\"id\":\"F200\",\"origin\":\"DFW\",\"dest\":\"ORD\",\"dep_utc\":\"2025-10-22T13:30:00Z\",\"arr_utc\":\"2025-10-22T16:50:00Z\"}" +
                        "]";

        Console.log("test.fno.ingest.determinism.in", body);

        String r1 =
                given()
                        .contentType("application/json")
                        .body(body)
                        .when()
                        .post("/fno/ingest")
                        .then()
                        .statusCode(200)
                        .extract()
                        .asString();

        String r2 =
                given()
                        .contentType("application/json")
                        .body(body)
                        .when()
                        .post("/fno/ingest")
                        .then()
                        .statusCode(200)
                        .extract()
                        .asString();

        Console.log("test.fno.ingest.determinism.out1", r1);
        Console.log("test.fno.ingest.determinism.out2", r2);

        assertNotNull(r1);
        assertNotNull(r2);

        // driver response should be stable
        assertEquals(r1, r2);
    }

}
