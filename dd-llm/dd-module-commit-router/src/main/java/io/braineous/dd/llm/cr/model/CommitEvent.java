package io.braineous.dd.llm.cr.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class CommitEvent {

    private String commitId;
    private String createdAt;
    private CommitRequest request;

    public CommitEvent() {
    }

    public String getCommitId() {
        return commitId;
    }

    public void setCommitId(String commitId) {
        this.commitId = commitId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public CommitRequest getRequest() {
        return request;
    }

    public void setRequest(CommitRequest request) {
        this.request = request;
    }

    // -------------------------
    // Hardening helpers
    // -------------------------

    public String safeCommitId() {
        if (this.commitId == null) {
            return null;
        }
        String t = this.commitId.trim();
        if (t.isEmpty()) {
            return null;
        }
        return t;
    }

    public String safeCreatedAt() {
        if (this.createdAt == null) {
            return null;
        }
        String t = this.createdAt.trim();
        if (t.isEmpty()) {
            return null;
        }
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

        String ca = safeCreatedAt();
        if (ca != null) {
            root.addProperty("createdAt", ca);
        }

        if (this.request != null) {
            JsonObject rj = this.request.toJson();
            root.add("request", JsonParser.parseString(rj.toString()).getAsJsonObject());
        }

        return root;
    }

    public String toJsonString() {
        return toJson().toString();
    }

    public static CommitEvent fromJson(JsonObject json) {
        if (json == null) {
            return null;
        }

        CommitEvent e = new CommitEvent();

        if (json.has("commitId") && !json.get("commitId").isJsonNull()) {
            try {
                e.setCommitId(json.get("commitId").getAsString());
            } catch (RuntimeException re) {
                // ignore
            }
        }

        if (json.has("createdAt") && !json.get("createdAt").isJsonNull()) {
            try {
                e.setCreatedAt(json.get("createdAt").getAsString());
            } catch (RuntimeException re) {
                // ignore
            }
        }

        if (json.has("request") && !json.get("request").isJsonNull()) {
            try {
                JsonElement el = json.get("request");
                if (el != null && el.isJsonObject()) {
                    CommitRequest r = CommitRequest.fromJson(el.getAsJsonObject());
                    e.setRequest(r);
                }
            } catch (RuntimeException re) {
                // ignore
            }
        }

        return e;
    }

    public static CommitEvent fromJsonString(String json) {
        if (json == null) {
            return null;
        }
        String t = json.trim();
        if (t.isEmpty()) {
            return null;
        }

        try {
            JsonElement el = JsonParser.parseString(t);
            if (el == null || !el.isJsonObject()) {
                return null;
            }
            return fromJson(el.getAsJsonObject());
        } catch (RuntimeException re) {
            return null;
        }
    }
}
