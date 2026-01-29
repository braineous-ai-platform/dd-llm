package io.braineous.dd.llm.cr.resources;

import io.braineous.dd.llm.cr.model.CommitAuditStatus;
import io.braineous.dd.llm.cr.model.CommitAuditView;
import io.braineous.dd.llm.cr.services.CommitAuditService;

import com.google.gson.JsonObject;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/cr/commit")
public class CommitAuditResource {

    @Inject
    CommitAuditService svc;

    @GET
    @Path("/{commitId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAudit(@PathParam("commitId") String commitId) {

        try {
            String id = safe(commitId);
            if (id == null) {
                return badRequest("commitId required");
            }

            CommitAuditView v = svc.getAudit(id);

            // service must return null when nothing exists for this commitId
            if (v == null) {
                return notFound("commitId not found", id);
            }

            return Response.ok(v.toJsonString()).build();

        } catch (RuntimeException re) {
            re.printStackTrace();
            return systemError("system_error", re);
        }
    }


    // -------------------------
    // error helpers
    // -------------------------

    private Response badRequest(String msg) {
        JsonObject root = new JsonObject();
        root.addProperty("error", safeMsg(msg, "bad_request"));
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(root.toString())
                .build();
    }

    private Response notFound(String msg, String commitId) {
        JsonObject root = new JsonObject();
        root.addProperty("error", safeMsg(msg, "not_found"));
        root.addProperty("commitId", commitId);
        return Response.status(Response.Status.NOT_FOUND)
                .entity(root.toString())
                .build();
    }

    private Response systemError(String code, RuntimeException re) {
        JsonObject root = new JsonObject();
        root.addProperty("error", safeMsg(code, "system_error"));

        // IMPORTANT:
        // Do NOT leak exception message / stack trace.
        // Observability should capture re elsewhere (filter / logger).
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

