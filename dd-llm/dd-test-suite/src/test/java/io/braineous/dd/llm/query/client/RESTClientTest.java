package io.braineous.dd.llm.query.client;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import ai.braineous.rag.prompt.cgo.api.Fact;
import ai.braineous.rag.prompt.models.cgo.graph.GraphBuilder;
import ai.braineous.rag.prompt.models.cgo.graph.Input;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RESTClientTest {

    private GraphBuilder graphBuilder;

    @BeforeEach
    public void setup() {
        graphBuilder = GraphBuilder.getInstance();
        seedAirportsAndFlight(graphBuilder);
    }

    @Test
    public void query_when_queryKind_null_returns_null() {

        QueryClient client = new RESTClient();

        QueryResult r = client.query(
                null,
                "q",
                "Airport:AUS",
                new ArrayList<String>()
        );

        assertNull(r);
    }

    @Test
    public void query_when_query_blank_returns_null() {

        QueryClient client = new RESTClient();

        QueryResult r = client.query(
                "validate_flight_airports",
                "   ",
                "Airport:AUS",
                new ArrayList<String>()
        );

        assertNull(r);
    }

    @Test
    public void query_when_fact_blank_returns_null() {

        QueryClient client = new RESTClient();

        QueryResult r = client.query(
                "validate_flight_airports",
                "Validate airports",
                "   ",
                new ArrayList<String>()
        );

        assertNull(r);
    }

    @Test
    public void query_when_seededGraph_doesNotThrow_and_returnsResult() {

        QueryClient client = new RESTClient();

        String queryKind = "validate_flight_airports";
        String query = "Validate that the selected flight has valid departure and arrival airport codes based on the airport nodes in the graph. "
                + "A valid flight must have: (1) 'from' matching one Airport:* code, (2) 'to' matching one Airport:* code, (3) 'from' != 'to'. "
                + "\"factId\": \"Flight:F100\"";

        String anchor = "Airport:AUS";
        List<String> relatedFacts = new ArrayList<String>();

        QueryResult result = null;
        try {
            result = client.query(queryKind, query, anchor, relatedFacts);
        } catch (Exception e) {
            fail("RESTClient.query should not throw. Threw: " + e.getClass().getName() + " - " + e.getMessage());
        }

        assertNotNull(result);

        assertNotNull(result.getQueryExecutionJson());
    }

    @Test
    public void driver_style_smoke_doesNotThrow() {

        QueryClient client = new RESTClient();

        String queryKind = "validate_flight_airports";
        String query = "Validate that the selected flight has valid departure and arrival airport codes based on the airport nodes in the graph. "
                + "A valid flight must have: (1) 'from' matching one Airport:* code, (2) 'to' matching one Airport:* code, (3) 'from' != 'to'. "
                + "\"factId\": \"Flight:F100\"";

        String anchor = "Airport:AUS";
        List<String> relatedFacts = new ArrayList<String>();

        try {
            client.query(queryKind, query, anchor, relatedFacts);
        } catch (Exception e) {
            fail("Driver-style call should not throw. Threw: " + e.getClass().getName() + " - " + e.getMessage());
        }
    }

    @Test
    public void query_sql_when_null_returns_null() {

        QueryClient client = new RESTClient();

        QueryResult r = client.query((String) null);

        assertNull(r);
    }

    @Test
    public void query_sql_when_blank_returns_null() {

        QueryClient client = new RESTClient();

        QueryResult r = client.query("   ");

        assertNull(r);
    }

    @Test
    public void query_sql_valid_flow_delegates_and_returns_result_shape() {

        QueryClient client = new RESTClient();

        String sql = ""
                + "select ok, code "
                + "from llm "
                + "where factId = 'Flight:AUS-DFW:001' "
                + "and relatedFactIds = 'Airport:AUS,Airport:DFW' "
                + "control decision_mode = 'validate_flight_airports'";

        QueryResult result = null;

        try {
            result = client.query(sql);
        } catch (Exception e) {
            fail("SQL path should not throw. Threw: " + e.getClass().getName());
        }

        assertNotNull(result);
        assertNotNull(result.getQueryExecutionJson());
    }

    @Test
    public void query_sql_without_relatedFacts_still_works() {

        QueryClient client = new RESTClient();

        String sql = ""
                + "select ok "
                + "from llm "
                + "where factId = 'Flight:AUS-DFW:001'";

        QueryResult result = null;

        try {
            result = client.query(sql);
        } catch (Exception e) {
            fail("SQL minimal path should not throw.");
        }

        assertNotNull(result);
        assertNotNull(result.getQueryExecutionJson());
    }

    // ---------------- seeding (must stay consistent across all tests) ----------------

    private void seedAirportsAndFlight(GraphBuilder graphBuilder) {

        Fact aus = new Fact("Airport:AUS", """
        {"id":"Airport:AUS","kind":"Airport","name":"Austin"}
        """);

        Fact dfw = new Fact("Airport:DFW", """
        {"id":"Airport:DFW","kind":"Airport","name":"Dallas"}
        """);

        Fact flight = new Fact("Flight:AUS-DFW:001", """
        {"id":"Flight:AUS-DFW:001","kind":"Flight","from":"Airport:AUS","to":"Airport:DFW"}
        """);

        graphBuilder.addNode(aus);
        graphBuilder.addNode(dfw);
        graphBuilder.addNode(flight);

        Input input = new Input(aus, dfw, flight);
        graphBuilder.bind(input, null);
    }
}
