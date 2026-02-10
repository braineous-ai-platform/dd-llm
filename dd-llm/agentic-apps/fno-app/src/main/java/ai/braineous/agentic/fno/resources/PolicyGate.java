package ai.braineous.agentic.fno.resources;

import ai.braineous.agentic.fno.reasoning.governance.FNOPolicyGate;
import ai.braineous.cgo.observer.WhySnapshot;
import com.google.gson.JsonObject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestResponse;

@Path("/fno/observe")
public class PolicyGate {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public RestResponse<JsonObject> observe(@QueryParam("queryKind") String queryKind) {

        if (queryKind == null || queryKind.isBlank()) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "INVALID_REQUEST");
            error.addProperty("details", "queryKind must be non-empty");
            return RestResponse.status(Response.Status.BAD_REQUEST, error);
        }

        try {
            FNOPolicyGate observer = new FNOPolicyGate();
            WhySnapshot snapshot = observer.getHistory(queryKind);

            JsonObject response = new JsonObject();
            response.addProperty("queryKind", queryKind);
            response.add("why_snapshot", snapshot.toJson());

            return RestResponse.ok(response);

        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "INTERNAL_ERROR");
            error.addProperty("details", e.getMessage());
            return RestResponse.status(Response.Status.INTERNAL_SERVER_ERROR, error);
        }
    }
}
