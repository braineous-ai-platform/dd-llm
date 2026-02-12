package ai.braineous.agentic.fno.resources;

import com.google.gson.JsonObject;
import io.braineous.dd.llm.pg.model.ExecutionView;
import io.braineous.dd.llm.pg.model.PolicyGateResult;
import io.braineous.dd.llm.policygate.client.PolicyGateApproveRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestResponse;

import ai.braineous.agentic.fno.reasoning.governance.FNOPolicyGate;

@Path("/fno")
public class PolicyGateResource {

    @GET
    @Path("/policygate/executions/{queryKind}")
    @Produces(MediaType.APPLICATION_JSON)
    public RestResponse<String> getExecutions(@PathParam("queryKind") String queryKind) {

        String qk = safe(queryKind);
        if (qk == null) {
            JsonObject err = new JsonObject();
            err.addProperty("error", "bad_request");
            err.addProperty("details", "queryKind required");
            return RestResponse.status(Response.Status.BAD_REQUEST, err.toString());
        }

        try {
            FNOPolicyGate pg = new FNOPolicyGate();
            ExecutionView v = pg.getExecutions(qk);

            if (v == null) {
                ExecutionView out = new ExecutionView();
                out.setQueryKind(qk);
                out.setExecutions(java.util.Collections.emptyList());
                return RestResponse.ok(out.toJsonString());
            }

            return RestResponse.ok(v.toJsonString());

        } catch (RuntimeException re) {
            JsonObject err = new JsonObject();
            err.addProperty("error", "system_error");
            err.addProperty("details", safeMsg(re.getMessage(), "system_error"));
            return RestResponse.status(Response.Status.INTERNAL_SERVER_ERROR, err.toString());
        }
    }

    @POST
    @Path("/policygate/commit/approve")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public RestResponse<String> approve(PolicyGateApproveRequest body) {

        if (body == null) {
            JsonObject err = new JsonObject();
            err.addProperty("error", "bad_request");
            err.addProperty("details", "body is required");
            return RestResponse.status(Response.Status.BAD_REQUEST, err.toString());
        }

        String qk = safe(body.getQueryKind());
        if (qk == null) {
            JsonObject err = new JsonObject();
            err.addProperty("error", "bad_request");
            err.addProperty("details", "queryKind required");
            return RestResponse.status(Response.Status.BAD_REQUEST, err.toString());
        }

        String cid = safe(body.getCommitId());
        if (cid == null) {
            JsonObject err = new JsonObject();
            err.addProperty("error", "bad_request");
            err.addProperty("details", "commitId required");
            return RestResponse.status(Response.Status.BAD_REQUEST, err.toString());
        }

        try {
            FNOPolicyGate pg = new FNOPolicyGate();
            PolicyGateResult r = pg.approve(qk, cid);

            if (r == null) {
                JsonObject err = new JsonObject();
                err.addProperty("error", "bad_request");
                err.addProperty("details", "approve failed");
                return RestResponse.status(Response.Status.BAD_REQUEST, err.toString());
            }

            String payload = r.toJsonString();

            if (r.isOk()) {
                return RestResponse.ok(payload);
            }

            return RestResponse.status(422, payload);

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

