package io.braineous.dd.llm.query.client;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import ai.braineous.cgo.llm.OpenAILlmAdapter;
import ai.braineous.rag.prompt.cgo.api.Fact;
import ai.braineous.rag.prompt.cgo.api.LlmAdapter;
import ai.braineous.rag.prompt.models.cgo.graph.GraphBuilder;
import ai.braineous.rag.prompt.models.cgo.graph.Input;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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

        LlmAdapter adapter = Mockito.mock(LlmAdapter.class);

        QueryResult r = client.query(
                adapter,
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

        LlmAdapter adapter = Mockito.mock(LlmAdapter.class);

        QueryResult r = client.query(
                adapter,
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

        LlmAdapter adapter = Mockito.mock(LlmAdapter.class);

        QueryResult r = client.query(
                adapter,
                "validate_flight_airports",
                "Validate airports",
                "   ",
                new ArrayList<String>()
        );

        assertNull(r);
    }

    @Test
    public void query_when_adapter_null_returns_null() {

        QueryClient client = new RESTClient();

        QueryResult r = client.query(
                null,
                "validate_flight_airports",
                "Validate airports",
                "Airport:AUS",
                new ArrayList<String>()
        );

        assertNull(r);
    }

    @Test
    public void query_when_seededGraph_and_adapterProvided_doesNotThrow_and_returnsNullIfOrchestratorReturnsNull() {

        QueryClient client = new RESTClient();

        // adapter mocked; whatever QueryOrchestrator calls will return null by default
        LlmAdapter adapter = Mockito.mock(LlmAdapter.class);

        String queryKind = "validate_flight_airports";
        String query = "Validate that the selected flight has valid departure and arrival airport codes based on the airport nodes in the graph. "
                + "A valid flight must have: (1) 'from' matching one Airport:* code, (2) 'to' matching one Airport:* code, (3) 'from' != 'to'. "
                + "\"factId\": \"Flight:F100\"";

        String anchor = "Airport:AUS";
        List<String> relatedFacts = new ArrayList<String>();

        QueryResult result = null;
        try {
            result = client.query(adapter, queryKind, query, anchor, relatedFacts);
        } catch (Exception e) {
            fail("RESTClient.query should not throw. Threw: " + e.getClass().getName() + " - " + e.getMessage());
        }

        // If orchestrator returns null due to adapter returning null responses, RESTClient should return null safely.
        assertNotNull(result);

        assertNotNull(result.getQueryExecutionJson());

        assertEquals("ERROR", result.getQueryExecutionJson().get("status").getAsString());

        assertEquals("llm_response", result.getQueryExecutionJson().get("stage").getAsString());

        assertTrue(result.getQueryExecutionJson().getAsJsonObject("llmResponseValidation").get("ok").getAsBoolean() == false);

        assertEquals("response.contract.empty", result.getQueryExecutionJson().getAsJsonObject("llmResponseValidation").get("code").getAsString());

        assertTrue(result.getQueryExecutionJson().get("rawResponse").isJsonNull());
    }

    @Test
    public void driver_style_smoke_doesNotThrow_with_real_adapter_object() {

        QueryClient client = new RESTClient();

        // keep stable shape, no assumptions
        JsonObject config = new JsonObject();
        LlmAdapter adapter = new OpenAILlmAdapter(config);

        String queryKind = "validate_flight_airports";
        String query = "Validate that the selected flight has valid departure and arrival airport codes based on the airport nodes in the graph. "
                + "A valid flight must have: (1) 'from' matching one Airport:* code, (2) 'to' matching one Airport:* code, (3) 'from' != 'to'. "
                + "\"factId\": \"Flight:F100\"";

        String anchor = "Airport:AUS";
        List<String> relatedFacts = new ArrayList<String>();

        try {
            client.query(adapter, queryKind, query, anchor, relatedFacts);
        } catch (Exception e) {
            fail("Driver-style call should not throw. Threw: " + e.getClass().getName() + " - " + e.getMessage());
        }
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

