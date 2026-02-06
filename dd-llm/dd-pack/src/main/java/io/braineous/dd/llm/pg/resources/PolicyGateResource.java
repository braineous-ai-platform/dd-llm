package io.braineous.dd.llm.pg.resources;

import com.google.gson.JsonObject;
import io.braineous.dd.llm.pg.model.ExecutionView;
import io.braineous.dd.llm.pg.model.PolicyGateResult;
import io.braineous.dd.llm.pg.services.PolicyGateOrchestrator;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/v1")
public class PolicyGateResource {

    @Inject
    private PolicyGateOrchestrator orch;

    @GET
    @Path("/policygate/executions/{queryKind}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getExecutions(@PathParam("queryKind") String queryKind) {

        try {
            String qk = safe(queryKind);
            if (qk == null) {
                return badRequest("queryKind required");
            }


            ExecutionView v = this.orch.getExecutions(qk);
            if (v == null) {
                // best-effort: treat as empty view for this queryKind
                ExecutionView out = new ExecutionView();
                out.setQueryKind(qk);
                out.setExecutions(java.util.Collections.emptyList());
                return Response.ok(out.toJsonString()).build();
            }

            return Response.ok(v.toJsonString()).build();

        } catch (RuntimeException re) {
            re.printStackTrace();
            return systemError("system_error", re);
        }
    }

    @POST
    @Path("/policygate/commit/approve")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response approve(PolicyGateApproveRequest body) {

        try {
            if (body == null) {
                return badRequest("body is required");
            }

            String qk = safe(body.getQueryKind());
            if (qk == null) {
                return badRequest("queryKind required");
            }

            String commitId = safe(body.getCommitId());
            if (commitId == null) {
                return badRequest("commitId required");
            }

            // commitId == factId invariant (locked)
            PolicyGateResult r = this.orch.approve(qk, commitId);
            if (r == null) {
                return badRequest("approve failed");
            }

            String payload = r.toJsonString();

            if (r.isOk()) {
                return Response.ok(payload).build();
            }

            // domain failure (unknown factId etc) -> 422
            return Response.status(422).entity(payload).build();

        } catch (RuntimeException re) {
            re.printStackTrace();
            return systemError("system_error", re);
        }
    }
    // -------------------------
    // error helpers (match CommitAuditResource style)
    // -------------------------

    private Response badRequest(String msg) {
        JsonObject root = new JsonObject();
        root.addProperty("error", safeMsg(msg, "bad_request"));
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(root.toString())
                .build();
    }

    private Response systemError(String code, RuntimeException re) {
        JsonObject root = new JsonObject();
        root.addProperty("error", safeMsg(code, "system_error"));
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(root.toString())
                .build();
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
