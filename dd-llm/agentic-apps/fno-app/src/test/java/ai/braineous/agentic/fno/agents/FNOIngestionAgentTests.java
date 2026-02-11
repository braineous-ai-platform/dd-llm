package ai.braineous.agentic.fno.agents;


import ai.braineous.cgo.config.CGOSystemConfig;
import ai.braineous.rag.prompt.models.cgo.graph.GraphBuilder;
import ai.braineous.rag.prompt.models.cgo.graph.GraphSnapshot;
import ai.braineous.rag.prompt.observe.Console;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FNOIngestionAgentTests {
    private FNOIngestionAgent fnoOrchestrator = new FNOIngestionAgent();

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
    void orchestrate_builds_nodes_and_edges_for_dfw_hub() {
        String body =
                "[" +
                        "{\"id\":\"F100\",\"number\":\"F100\",\"origin\":\"AUS\",\"dest\":\"DFW\",\"dep_utc\":\"2025-10-22T10:00:00Z\",\"arr_utc\":\"2025-10-22T11:10:00Z\",\"capacity\":150,\"equipment\":\"320\"}," +
                        "{\"id\":\"F102\",\"number\":\"F102\",\"origin\":\"AUS\",\"dest\":\"DFW\",\"dep_utc\":\"2025-10-22T11:30:00Z\",\"arr_utc\":\"2025-10-22T12:40:00Z\",\"capacity\":150,\"equipment\":\"320\"}," +
                        "{\"id\":\"F110\",\"number\":\"F110\",\"origin\":\"SAT\",\"dest\":\"DFW\",\"dep_utc\":\"2025-10-22T11:10:00Z\",\"arr_utc\":\"2025-10-22T12:15:00Z\",\"capacity\":150,\"equipment\":\"320\"}," +
                        "{\"id\":\"F120\",\"number\":\"F120\",\"origin\":\"IAH\",\"dest\":\"DFW\",\"dep_utc\":\"2025-10-22T11:20:00Z\",\"arr_utc\":\"2025-10-22T12:25:00Z\",\"capacity\":150,\"equipment\":\"319\"}," +
                        "{\"id\":\"F200\",\"number\":\"F200\",\"origin\":\"DFW\",\"dest\":\"ORD\",\"dep_utc\":\"2025-10-22T13:30:00Z\",\"arr_utc\":\"2025-10-22T16:50:00Z\",\"capacity\":150,\"equipment\":\"738\"}," +
                        "{\"id\":\"F210\",\"number\":\"F210\",\"origin\":\"DFW\",\"dest\":\"JFK\",\"dep_utc\":\"2025-10-22T13:20:00Z\",\"arr_utc\":\"2025-10-22T17:10:00Z\",\"capacity\":150,\"equipment\":\"321\"}," +
                        "{\"id\":\"F220\",\"number\":\"F220\",\"origin\":\"DFW\",\"dest\":\"LAX\",\"dep_utc\":\"2025-10-22T13:45:00Z\",\"arr_utc\":\"2025-10-22T15:20:00Z\",\"capacity\":150,\"equipment\":\"73G\"}" +
                        "]";

        Console.log("test.fno.orchestrate.in", body);

        JsonArray flights = JsonParser.parseString(body).getAsJsonArray();

        FNOIngestionAgent orch = new FNOIngestionAgent();
        GraphSnapshot view = (GraphSnapshot) orch.ingestFlights(flights);

        assertNotNull(view);
        Console.log("test.fno.orchestrate.out.nodes", "" + view.nodes().size());
        Console.log("test.fno.orchestrate.out.edges", "" + view.edges().size());

        // Node invariants
        assertNotNull(view.getFactById("Flight:F102"));
        assertNotNull(view.getFactById("Airport:DFW"));

        // Edge invariants
        assertTrue(view.edges().size() > 0);

        // Strong edge expectation from your known 4x3 construction
        assertTrue(view.edges().containsKey("Edge:Flight:F100->Flight:F200"));
    }


    @Test
    void orchestrate_contains_expected_edge_keys_for_dfw_hub() {
        String body =
                "[" +
                        "{\"id\":\"F100\",\"number\":\"F100\",\"origin\":\"AUS\",\"dest\":\"DFW\",\"dep_utc\":\"2025-10-22T10:00:00Z\",\"arr_utc\":\"2025-10-22T11:10:00Z\",\"capacity\":150,\"equipment\":\"320\"}," +
                        "{\"id\":\"F102\",\"number\":\"F102\",\"origin\":\"AUS\",\"dest\":\"DFW\",\"dep_utc\":\"2025-10-22T11:30:00Z\",\"arr_utc\":\"2025-10-22T12:40:00Z\",\"capacity\":150,\"equipment\":\"320\"}," +
                        "{\"id\":\"F110\",\"number\":\"F110\",\"origin\":\"SAT\",\"dest\":\"DFW\",\"dep_utc\":\"2025-10-22T11:10:00Z\",\"arr_utc\":\"2025-10-22T12:15:00Z\",\"capacity\":150,\"equipment\":\"320\"}," +
                        "{\"id\":\"F120\",\"number\":\"F120\",\"origin\":\"IAH\",\"dest\":\"DFW\",\"dep_utc\":\"2025-10-22T11:20:00Z\",\"arr_utc\":\"2025-10-22T12:25:00Z\",\"capacity\":150,\"equipment\":\"319\"}," +
                        "{\"id\":\"F200\",\"number\":\"F200\",\"origin\":\"DFW\",\"dest\":\"ORD\",\"dep_utc\":\"2025-10-22T13:30:00Z\",\"arr_utc\":\"2025-10-22T16:50:00Z\",\"capacity\":150,\"equipment\":\"738\"}," +
                        "{\"id\":\"F210\",\"number\":\"F210\",\"origin\":\"DFW\",\"dest\":\"JFK\",\"dep_utc\":\"2025-10-22T13:20:00Z\",\"arr_utc\":\"2025-10-22T17:10:00Z\",\"capacity\":150,\"equipment\":\"321\"}," +
                        "{\"id\":\"F220\",\"number\":\"F220\",\"origin\":\"DFW\",\"dest\":\"LAX\",\"dep_utc\":\"2025-10-22T13:45:00Z\",\"arr_utc\":\"2025-10-22T15:20:00Z\",\"capacity\":150,\"equipment\":\"73G\"}" +
                        "]";

        Console.log("test.fno.orchestrate.edgeKeys.in", body);

        JsonArray flights = JsonParser.parseString(body).getAsJsonArray();

        FNOIngestionAgent orch = new FNOIngestionAgent();
        GraphSnapshot view = (GraphSnapshot) orch.ingestFlights(flights);

        assertNotNull(view);

        Console.log("test.fno.orchestrate.edgeKeys.count", "" + view.edges().size());

        // exact known edges (sample of the 12)
        assertTrue(view.edges().containsKey("Edge:Flight:F100->Flight:F200"));
        assertTrue(view.edges().containsKey("Edge:Flight:F100->Flight:F210"));
        assertTrue(view.edges().containsKey("Edge:Flight:F100->Flight:F220"));

        // another inbound flight connects to all outbound flights
        assertTrue(view.edges().containsKey("Edge:Flight:F120->Flight:F200"));
        assertTrue(view.edges().containsKey("Edge:Flight:F120->Flight:F210"));
        assertTrue(view.edges().containsKey("Edge:Flight:F120->Flight:F220"));
    }



    @Test
    void orchestrate_returns_empty_graph_for_empty_input() {
        String body = "[]";
        Console.log("test.fno.orchestrate.empty.in", body);

        JsonArray flights = JsonParser.parseString(body).getAsJsonArray();

        FNOIngestionAgent orch = new FNOIngestionAgent();
        GraphSnapshot view = (GraphSnapshot) orch.ingestFlights(flights);

        assertNotNull(view);

        Console.log("test.fno.orchestrate.empty.out.nodes", "" + view.nodes().size());
        Console.log("test.fno.orchestrate.empty.out.edges", "" + view.edges().size());

        assertEquals(0, view.nodes().size());
        assertEquals(0, view.edges().size());
    }

    @Test
    void orchestrate_ignores_garbage_and_keeps_valid_flights() {
        String body =
                "[" +
                        "{\"id\":\"F102\",\"origin\":\"AUS\",\"dest\":\"DFW\",\"dep_utc\":\"2025-10-22T11:30:00Z\",\"arr_utc\":\"2025-10-22T12:40:00Z\"}," +
                        "{\"item\":\"Book\",\"quantity\":2,\"price\":10.5}" +
                        "]";

        Console.log("test.fno.orchestrate.mixed.in", body);

        JsonArray flights = JsonParser.parseString(body).getAsJsonArray();

        FNOIngestionAgent orch = new FNOIngestionAgent();
        GraphSnapshot view = (GraphSnapshot) orch.ingestFlights(flights);

        assertNotNull(view);

        Console.log("test.fno.orchestrate.mixed.out.nodes", "" + view.nodes().size());
        Console.log("test.fno.orchestrate.mixed.out.edges", "" + view.edges().size());

        // valid flight survives
        assertNotNull(view.getFactById("Flight:F102"));
        assertNotNull(view.getFactById("Airport:AUS"));
        assertNotNull(view.getFactById("Airport:DFW"));

        // garbage does not pollute graph
        assertFalse(view.nodes().keySet().stream().anyMatch(k -> k.contains("Book")));
    }


    @Test
    void orchestrate_single_flight_produces_nodes_but_no_edges() {
        String body =
                "[" +
                        "{\"id\":\"F102\",\"number\":\"F102\",\"origin\":\"AUS\",\"dest\":\"DFW\"," +
                        "\"dep_utc\":\"2025-10-22T11:30:00Z\",\"arr_utc\":\"2025-10-22T12:40:00Z\"}" +
                        "]";

        Console.log("test.fno.orchestrate.single.in", body);

        JsonArray flights =
                JsonParser.parseString(body).getAsJsonArray();

        FNOIngestionAgent orch = new FNOIngestionAgent();
        GraphSnapshot view = (GraphSnapshot) orch.ingestFlights(flights);

        assertNotNull(view);

        Console.log("test.fno.orchestrate.single.out.nodes", "" + view.nodes().size());
        Console.log("test.fno.orchestrate.single.out.edges", "" + view.edges().size());

        // nodes exist
        assertNotNull(view.getFactById("Flight:F102"));
        assertNotNull(view.getFactById("Airport:AUS"));
        assertNotNull(view.getFactById("Airport:DFW"));

        // no possible connections with single flight
        assertEquals(0, view.edges().size());
    }

    @Test
    void orchestrate_is_deterministic_for_same_input() {
        String body =
                "[" +
                        "{\"id\":\"F100\",\"origin\":\"AUS\",\"dest\":\"DFW\",\"dep_utc\":\"2025-10-22T10:00:00Z\",\"arr_utc\":\"2025-10-22T11:10:00Z\"}," +
                        "{\"id\":\"F102\",\"origin\":\"AUS\",\"dest\":\"DFW\",\"dep_utc\":\"2025-10-22T11:30:00Z\",\"arr_utc\":\"2025-10-22T12:40:00Z\"}," +
                        "{\"id\":\"F200\",\"origin\":\"DFW\",\"dest\":\"ORD\",\"dep_utc\":\"2025-10-22T13:30:00Z\",\"arr_utc\":\"2025-10-22T16:50:00Z\"}" +
                        "]";

        Console.log("test.fno.orchestrate.determinism.in", body);

        JsonArray flights =
                JsonParser.parseString(body).getAsJsonArray();

        FNOIngestionAgent orch = new FNOIngestionAgent();

        GraphSnapshot v1 = (GraphSnapshot) orch.ingestFlights(flights);
        GraphSnapshot v2 = (GraphSnapshot) orch.ingestFlights(flights);

        assertNotNull(v1);
        assertNotNull(v2);

        Console.log("test.fno.orchestrate.determinism.v1.nodes", "" + v1.nodes().size());
        Console.log("test.fno.orchestrate.determinism.v1.edges", "" + v1.edges().size());
        Console.log("test.fno.orchestrate.determinism.v2.nodes", "" + v2.nodes().size());
        Console.log("test.fno.orchestrate.determinism.v2.edges", "" + v2.edges().size());

        // deterministic sizes
        assertEquals(v1.nodes().size(), v2.nodes().size());
        assertEquals(v1.edges().size(), v2.edges().size());

        // deterministic keys
        assertEquals(v1.nodes().keySet(), v2.nodes().keySet());
        assertEquals(v1.edges().keySet(), v2.edges().keySet());
    }


    @Test
    void orchestrate_ignores_malformed_elements_and_still_builds_graph() {
        String body =
                "[" +
                        "{\"id\":\"F100\",\"origin\":\"AUS\",\"dest\":\"DFW\",\"dep_utc\":\"2025-10-22T10:00:00Z\",\"arr_utc\":\"2025-10-22T11:10:00Z\"}," +
                        "\"NOT_JSON_OBJECT\"," +
                        "{\"id\":\"F200\",\"origin\":\"DFW\",\"dest\":\"ORD\",\"dep_utc\":\"2025-10-22T13:30:00Z\",\"arr_utc\":\"2025-10-22T16:50:00Z\"}" +
                        "]";

        Console.log("test.fno.orchestrate.malformed.in", body);

        JsonArray flights =
                JsonParser.parseString(body).getAsJsonArray();

        FNOIngestionAgent orch = new FNOIngestionAgent();
        GraphSnapshot view = (GraphSnapshot) orch.ingestFlights(flights);

        assertNotNull(view);

        Console.log("test.fno.orchestrate.malformed.out.nodes", "" + view.nodes().size());
        Console.log("test.fno.orchestrate.malformed.out.edges", "" + view.edges().size());

        // valid facts still survive
        assertNotNull(view.getFactById("Flight:F100"));
        assertNotNull(view.getFactById("Flight:F200"));
        assertNotNull(view.getFactById("Airport:AUS"));
        assertNotNull(view.getFactById("Airport:DFW"));
        assertNotNull(view.getFactById("Airport:ORD"));
    }

}
