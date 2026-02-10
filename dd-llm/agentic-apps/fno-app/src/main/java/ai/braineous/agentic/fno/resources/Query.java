package ai.braineous.agentic.fno.resources;


import ai.braineous.agentic.fno.reasoning.query.FNOQueryExecution;
import ai.braineous.rag.prompt.cgo.api.QueryExecution;
import ai.braineous.rag.prompt.cgo.api.ValidateTask;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.RestResponse.Status;

@Path("/fno/query")
public class Query {
    
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public RestResponse<String> query(String requestJson) {
        System.out.println("____input____");
        System.out.println(requestJson);

        // 1) Parse request body -> JsonObject
        JsonObject json;
        try {
            json = JsonParser.parseString(requestJson).getAsJsonObject();
        } catch (Exception e) {
            JsonObject err = new JsonObject();
            err.addProperty("error", "Invalid JSON");
            err.addProperty("details", e.getMessage());
            return RestResponse.status(Status.BAD_REQUEST, err.toString());
        }

        // 2) Orchestrate
        QueryExecution<ValidateTask> execution;
        try {
            FNOQueryExecution orchestrator = new FNOQueryExecution();
            execution = orchestrator.executeQuery(json);
        } catch (Exception e) {
            JsonObject err = new JsonObject();
            err.addProperty("error", "Orchestration failed");
            err.addProperty("details", e.getMessage());
            return RestResponse.status(Status.INTERNAL_SERVER_ERROR, err.toString());
        }

        // 3) Response (stable shape for E2E tests)
        JsonObject out = new JsonObject();
        out.addProperty("rawResponse", execution.getRawResponse());

        // keep these as strings for now (freeze-friendly)
        out.addProperty("promptValidation", String.valueOf(execution.getPromptValidation()));
        out.addProperty("llmResponseValidation", String.valueOf(execution.getLlmResponseValidation()));
        out.addProperty("domainValidation", String.valueOf(execution.getDomainValidation()));

        return RestResponse.status(Status.OK, out.toString());
    }
}
