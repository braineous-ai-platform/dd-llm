package io.braineous.dd.llm.query.client;

import ai.braineous.cgo.llm.OpenAILlmAdapter;
import ai.braineous.rag.prompt.cgo.api.Edge;
import ai.braineous.rag.prompt.cgo.api.Fact;
import ai.braineous.rag.prompt.cgo.api.LlmAdapter;
import ai.braineous.rag.prompt.models.cgo.graph.BindResult;
import ai.braineous.rag.prompt.models.cgo.graph.GraphBuilder;
import ai.braineous.rag.prompt.models.cgo.graph.Input;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class RESTClientTest {

    @Test
    public void driver(){
        QueryClient client = new RESTClient();
        String queryKind = "validate_flight_airports";
        String query = "Validate that the selected flight has valid departure and arrival airport codes based on the airport nodes in the graph. A valid flight must have: (1) 'from' matching one Airport:* code, (2) 'to' matching one Airport:* code, (3) 'from' != 'to'.\",\n" +
                "    \"factId\": \"Flight:F100\"";
        String anchor = "Airport:AUS";
        List<String> relatedFacts = new ArrayList<>();

        //build the graph
        GraphBuilder graphBuilder = GraphBuilder.getInstance();

        // given
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

        // when: first bind
        Input input = new Input(aus, dfw, flight);
        BindResult firstBind = graphBuilder.bind(input, null);

        // v1: adapter config placeholder (keep stable shape, no assumptions)
        JsonObject config = new JsonObject();
        LlmAdapter adapter = new OpenAILlmAdapter(config);

        client.query(
                adapter,
               queryKind,
               query,
               anchor,
               relatedFacts
        );

    }
}
