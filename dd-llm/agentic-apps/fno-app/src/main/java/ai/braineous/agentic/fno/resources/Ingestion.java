package ai.braineous.agentic.fno.resources;

import ai.braineous.agentic.fno.agents.FNOAgent;
import ai.braineous.rag.prompt.cgo.api.GraphView;
import ai.braineous.rag.prompt.observe.Console;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestResponse;

@Path("/fno/ingest")
public class Ingestion {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public RestResponse<String> ingest(String body) {
        Console.log("api.ingest.in", body);

        JsonArray flights;
        try {
            var root = JsonParser.parseString(body);
            if (root.isJsonArray()) {
                flights = root.getAsJsonArray();
            } else if (root.isJsonObject() && root.getAsJsonObject().has("flights")) {
                flights = root.getAsJsonObject().getAsJsonArray("flights");
            } else {
                return RestResponse.status(Response.Status.BAD_REQUEST, "{\"error\":\"invalid payload\"}");
            }
        } catch (Exception e) {
            Console.log("api.ingest.error", String.valueOf(e));
            return RestResponse.status(Response.Status.BAD_REQUEST, "{\"error\":\"parse failed\"}");
        }

        FNOAgent agent = new FNOAgent();
        GraphView graph = agent.orchestrate(flights);

        String out = (graph == null) ? "{}" : String.valueOf(graph); // v1: driver response
        Console.log("api.ingest.out", out);

        return RestResponse.ok(out);
    }

}
