package io.braineous.dd.llm.cr.client;

import com.google.gson.JsonObject;
import io.braineous.dd.llm.core.model.Why;
import io.braineous.dd.llm.core.processor.HttpPoster;
import io.braineous.dd.llm.core.processor.JsonSerializer;
import io.braineous.dd.llm.cr.model.CommitResult;

public class CommitClient {
    private static final CommitClient client = new CommitClient();

    private CommitClient() {
    }

    public static CommitClient getInstance(){
        return client;
    }

    public CommitResult invoke(
            HttpPoster httpPoster,
            JsonSerializer serializer,
            final String endpoint,
            JsonObject commitJson,
            Object commit) {

        final long t0 = System.nanoTime();

        // ---- null/blank guards (no throw; return fail) ----
        if (httpPoster == null) {
            return CommitResult.fail(commitJson, endpoint, null, null,
                    new Why("LLMDD-REST-missing_httpPoster", "httpPoster is null"));
        }
        if (serializer == null) {
            return CommitResult.fail(commitJson, endpoint, null, null,
                    new Why("LLMDD-REST-missing_serializer", "serializer is null"));
        }
        if (endpoint == null || endpoint.isBlank()) {
            return CommitResult.fail(commitJson, endpoint, null, null,
                    new Why("LLMDD-REST-missing_endpoint", "endpoint is null/blank"));
        }
        if (commitJson == null) {
            // keep your earlier semantics: fail even before call
            return CommitResult.fail(endpoint, null, null,
                    new Why("LLMDD-REST-null_commitJson", "commitJson is null"));
        }
        if (commit == null) {
            return CommitResult.fail(commitJson, endpoint, null, null,
                    new Why("LLMDD-REST-null_event", "commit is null"));
        }

        try {
            String body = serializer.toJson(commit);

            int status = httpPoster.post(endpoint, body);

            long durationMs = (System.nanoTime() - t0) / 1_000_000L;

            if (status >= 200 && status < 300) {
                return CommitResult.ok(commitJson, endpoint, status, durationMs);
            }

            return CommitResult.fail(commitJson, endpoint, status, durationMs,
                    new Why("LLMDD-REST-non_2xx", "HTTP status " + status));

        } catch (Exception e) {
            e.printStackTrace();

            long durationMs = (System.nanoTime() - t0) / 1_000_000L;

            String msg = (e.getMessage() != null && !e.getMessage().isBlank())
                    ? e.getMessage()
                    : e.getClass().getSimpleName();

            return CommitResult.fail(commitJson, endpoint, null, durationMs,
                    new Why("LLMDD-REST-call_failed", msg));
        }
    }
}
