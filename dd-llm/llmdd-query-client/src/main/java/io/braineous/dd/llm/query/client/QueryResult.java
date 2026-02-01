package io.braineous.dd.llm.query.client;

import com.google.gson.JsonObject;
import io.braineous.dd.llm.core.model.Why;

public class QueryResult {

    // field init helper (thread-safe)
    private static final java.util.concurrent.atomic.AtomicLong EVENT_SEQ =
            new java.util.concurrent.atomic.AtomicLong();

    public static final String ID_PREFIX = "DD-LLM-QUERY";
    public static final String WHY_CODE_NULL_EVENT = ID_PREFIX + "-FAIL-requestJson_null";
    public static final String WHY_MSG_NULL_EVENT  = "requestJson cannot be null for ok=true";

    private String id;
    private JsonObject requestJson;
    private JsonObject queryExecutionJson;
    private boolean ok;
    private Why why;


    // --------- Canonical constructor (single source of truth) ---------


    public QueryResult() {
    }

    public QueryResult(JsonObject requestJson, JsonObject queryExecutionJson, boolean ok, Why why) {
        long seq = nextSeq();
        this.id = nextEventId(seq);

        // Invariants:
        // 1) ok=true => ddEventJson must be non-null, why must be null
        // 2) ok=false => why must be non-null (ddEventJson may be null)
        if (ok) {
            if (requestJson == null || queryExecutionJson == null) {
                // convert to clean failure result (DLQ-friendly, no throw)
                this.requestJson = null;
                this.ok = false;
                this.why = new Why(WHY_CODE_NULL_EVENT, WHY_CODE_NULL_EVENT);
                return;
            }
            this.requestJson = requestJson;
            this.queryExecutionJson = queryExecutionJson;
            this.ok = true;
            this.why = null; // enforce: success carries no failure why
            return;
        }

        // ok == false
        this.requestJson = requestJson; // allowed null for failure
        this.queryExecutionJson = queryExecutionJson; // <-- MISSING: preserve execution json on failures too
        this.ok = false;
        this.why = (why != null)
                ? why
                : new Why(ID_PREFIX+"-missing_why", "ok=false requires a non-null why");

    }

    // Convenience 2-arg ctor (keeps your old call sites alive)
    public QueryResult(JsonObject requestJson, JsonObject queryExecutionJson, boolean ok) {
        this(requestJson, queryExecutionJson, ok, null);
    }

    // --------- Static factories (preferred) ---------

    public static QueryResult ok(JsonObject requestJson, JsonObject queryExecutionJson) {
        return new QueryResult(requestJson, queryExecutionJson, true, null);
    }

    public static QueryResult fail(JsonObject requestJson, JsonObject queryExecutionJson, Why why) {
        return new QueryResult(requestJson, queryExecutionJson, false, why);
    }

    public static QueryResult fail(Why why) {
        return new QueryResult(null, null, false, why);
    }

    // --------- Getters ---------


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public JsonObject getRequestJson() {
        return requestJson;
    }

    public void setRequestJson(JsonObject requestJson) {
        this.requestJson = requestJson;
    }

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public Why getWhy() {
        return why;
    }

    public void setWhy(Why why) {
        this.why = why;
    }

    public JsonObject getQueryExecutionJson() {
        return queryExecutionJson;
    }

    public void setQueryExecutionJson(JsonObject queryExecutionJson) {
        this.queryExecutionJson = queryExecutionJson;
    }

    @Override
    public String toString() {
        return "QueryResult{" +
                "id='" + id + '\'' +
                ", requestJson=" + requestJson +
                ", queryExecutionJson=" + queryExecutionJson +
                ", ok=" + ok +
                ", why=" + why +
                '}';
    }

    // --------- Internal helpers ---------
    private static long nextSeq() {
        return EVENT_SEQ.incrementAndGet();
    }

    private static String nextEventId(long seq) {
        return ID_PREFIX + seq;
    }

    //----------------------------------------------------------------------
    public JsonObject toJson() {

        JsonObject out = new JsonObject();

        if (this.id != null) {
            out.addProperty("id", this.id);
        }

        out.addProperty("ok", this.ok);

        if (this.why != null) {
            JsonObject w = new JsonObject();

            String r = this.why.reason();
            if (r != null) {
                w.addProperty("reason", r);
            }

            String d = this.why.getDetails();
            if (d != null) {
                w.addProperty("details", d);
            }

            out.add("why", w);
        } else {
            out.add("why", null);
        }

        if (this.requestJson != null) {
            out.add("requestJson", this.requestJson);
        } else {
            out.add("requestJson", null);
        }

        if (this.queryExecutionJson != null) {
            out.add("queryExecutionJson", this.queryExecutionJson);
        } else {
            out.add("queryExecutionJson", null);
        }

        return out;
    }

    public static QueryResult fromJson(JsonObject json) {

        if (json == null) {
            throw new IllegalArgumentException("QueryResult JSON cannot be null");
        }

        String id = null;
        if (json.has("id") && !json.get("id").isJsonNull()) {
            id = json.get("id").getAsString();
        }

        boolean ok = false;
        if (json.has("ok") && !json.get("ok").isJsonNull()) {
            ok = json.get("ok").getAsBoolean();
        }

        JsonObject requestJson = null;
        if (json.has("requestJson") && !json.get("requestJson").isJsonNull()) {
            requestJson = json.getAsJsonObject("requestJson");
        }

        JsonObject queryExecutionJson = null;
        if (json.has("queryExecutionJson") && !json.get("queryExecutionJson").isJsonNull()) {
            queryExecutionJson = json.getAsJsonObject("queryExecutionJson");
        }

        Why why = null;
        if (json.has("why") && !json.get("why").isJsonNull() && json.get("why").isJsonObject()) {
            JsonObject w = json.getAsJsonObject("why");

            String reason = null;
            if (w.has("reason") && !w.get("reason").isJsonNull()) {
                reason = w.get("reason").getAsString();
            }

            String details = null;
            if (w.has("details") && !w.get("details").isJsonNull()) {
                details = w.get("details").getAsString();
            }

            // If Why has a different ctor signature in your code, adjust here.
            why = new Why(reason, details);
        }

        // Build via canonical ctor to enforce invariants (ok=true needs requestJson, etc.)
        QueryResult out = new QueryResult(requestJson, queryExecutionJson, ok, why);

        // Preserve id if provided (JSON should be source of truth on replay/rehydrate)
        if (id != null) {
            out.setId(id);
        }

        return out;
    }


}
