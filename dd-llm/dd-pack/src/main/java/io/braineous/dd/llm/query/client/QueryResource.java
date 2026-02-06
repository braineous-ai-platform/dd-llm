package io.braineous.dd.llm.query.client;

import ai.braineous.rag.prompt.cgo.api.LlmAdapter;
import com.google.gson.JsonObject;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/v1")
public class QueryResource {

    @Inject
    QueryAdapterResolver adapterResolver;

    @POST
    @Path("/query")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response query(QueryHttpRequest body) {

        try {
            if (body == null) {
                return badRequest("body is required");
            }

            String queryKind = safe(body.getQueryKind());
            if (queryKind == null) {
                return badRequest("queryKind required");
            }

            String query = safe(body.getQuery());
            if (query == null) {
                return badRequest("query required");
            }

            String fact = safe(body.getFact());
            if (fact == null) {
                return badRequest("fact required");
            }

            LlmAdapter adapter = this.adapterResolver.resolve(body.getAdapter());
            if (adapter == null) {
                return badRequest("invalid adapter");
            }

            QueryClient client = new RESTClient();

            QueryResult result =
                    client.query(
                            adapter,
                            queryKind,
                            query,
                            fact,
                            body.getRelatedFacts()
                    );

            // RESTClient currently returns null on many input/data gaps
            if (result == null) {
                return badRequest("query failed");
            }

            String payload = result.toJson().toString();

            if (result.isOk()) {
                return Response.ok(payload).build();
            }

            // domain failure with WHY -> 422
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

