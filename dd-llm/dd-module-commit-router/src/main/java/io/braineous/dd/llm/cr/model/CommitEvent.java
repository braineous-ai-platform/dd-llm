package io.braineous.dd.llm.cr.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class CommitEvent {

    // commitId == ingestionId (phase-2 axis)
    private String commitId;

    // attempt number (1..N). 0/empty means unknown/unset.
    private int attempt;

    // created time for this attempt (best-effort, ISO-8601 recommended)
    private String createdAt;

    // frozen intent that was attempted
    private CommitRequest request;

    public CommitEvent() {
    }

    public String getCommitId() { return commitId; }
    public void setCommitId(String commitId) { this.commitId = commitId; }

    public int getAttempt() { return attempt; }
    public void setAttempt(int attempt) { this.attempt = attempt; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public CommitRequest getRequest() { return request; }
    public void setRequest(CommitRequest request) { this.request = request; }

    // -------------------------
    // Hardening helpers
    // -------------------------

    public String safeCommitId() {
        if (this.commitId == null) { return null; }
        String t = this.commitId.trim();
        if (t.isEmpty()) { return null; }
        return t;
    }

    public int safeAttempt() {
        if (this.attempt <= 0) { return 0; }
        return this.attempt;
    }

    public String safeCreatedAt() {
        if (this.createdAt == null) { return null; }
        String t = this.createdAt.trim();
        if (t.isEmpty()) { return null; }
        return t;
    }

    // -------------------------
    // JSON serialization
    // -------------------------

    public JsonObject toJson() {
        JsonObject root = new JsonObject();

        String id = safeCommitId();
        if (id != null) {
            root.addProperty("commitId", id);
        }

        int a = safeAttempt();
        if (a > 0) {
            root.addProperty("attempt", a);
        }

        String ca = safeCreatedAt();
        if (ca != null) {
            root.addProperty("createdAt", ca);
        }

        if (this.request != null) {
            root.add("request", this.request.toJson());
        }

        return root;
    }

    public String toJsonString() {
        return toJson().toString();
    }

    public static CommitEvent fromJson(JsonObject json) {
        if (json == null) { return null; }

        CommitEvent e = new CommitEvent();

        if (json.has("commitId") && !json.get("commitId").isJsonNull()) {
            try { e.setCommitId(json.get("commitId").getAsString()); } catch (RuntimeException re) { }
        }

        if (json.has("attempt") && !json.get("attempt").isJsonNull()) {
            try {
                if (json.get("attempt").isJsonPrimitive()) {
                    e.setAttempt(json.get("attempt").getAsInt());
                }
            } catch (RuntimeException re) { }
        }

        if (json.has("createdAt") && !json.get("createdAt").isJsonNull()) {
            try { e.setCreatedAt(json.get("createdAt").getAsString()); } catch (RuntimeException re) { }
        }

        if (json.has("request") && !json.get("request").isJsonNull()) {
            try {
                JsonElement el = json.get("request");
                if (el != null && el.isJsonObject()) {
                    CommitRequest r = CommitRequest.fromJson(el.getAsJsonObject());
                    e.setRequest(r);
                }
            } catch (RuntimeException re) { }
        }

        return e;
    }

    public static CommitEvent fromJsonString(String json) {
        if (json == null) { return null; }
        String t = json.trim();
        if (t.isEmpty()) { return null; }

        try {
            JsonElement el = JsonParser.parseString(t);
            if (el == null || !el.isJsonObject()) { return null; }
            return fromJson(el.getAsJsonObject());
        } catch (RuntimeException re) {
            return null;
        }
    }
}

