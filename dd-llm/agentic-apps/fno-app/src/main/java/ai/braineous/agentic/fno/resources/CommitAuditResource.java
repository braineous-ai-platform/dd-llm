package ai.braineous.agentic.fno.resources;

import com.google.gson.JsonObject;
import io.braineous.dd.llm.cr.model.CommitAuditView;
import org.jboss.resteasy.reactive.RestResponse;

import ai.braineous.agentic.fno.reasoning.governance.FNOCommitAudit;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/fno")
public class CommitAuditResource {

    @Inject
    FNOCommitAudit audit;

    @GET
    @Path("/audit/commit/{commitId}")
    @Produces(MediaType.APPLICATION_JSON)
    public RestResponse<String> getAudit(@PathParam("commitId") String commitId) {

        String id = safe(commitId);
        if (id == null) {
            JsonObject err = new JsonObject();
            err.addProperty("error", "bad_request");
            err.addProperty("details", "commitId required");
            return RestResponse.status(Response.Status.BAD_REQUEST, err.toString());
        }

        try {
            if (this.audit == null) {
                JsonObject err = new JsonObject();
                err.addProperty("error", "system_error");
                err.addProperty("details", "audit not initialized");
                return RestResponse.status(Response.Status.INTERNAL_SERVER_ERROR, err.toString());
            }

            CommitAuditView v = this.audit.getAudit(id);

            if (v == null) {
                JsonObject err = new JsonObject();
                err.addProperty("error", "not_found");
                err.addProperty("details", "commitId not found");
                err.addProperty("commitId", id);
                return RestResponse.status(Response.Status.NOT_FOUND, err.toString());
            }

            return RestResponse.ok(v.toJsonString());

        } catch (RuntimeException re) {
            JsonObject err = new JsonObject();
            err.addProperty("error", "system_error");
            err.addProperty("details", safeMsg(re.getMessage(), "system_error"));
            return RestResponse.status(Response.Status.INTERNAL_SERVER_ERROR, err.toString());
        }
    }

    // -------------------------
    // helpers
    // -------------------------

    private String safe(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return null;
        }
        return t;
    }

    private String safeMsg(String s, String fallback) {
        String t = safe(s);
        if (t == null) {
            return fallback;
        }
        return t;
    }
}


