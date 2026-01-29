package io.braineous.dd.llm.cr.model;

import com.google.gson.JsonPrimitive;
import io.braineous.dd.llm.core.model.Why;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class CommitReceipt {

    private String commitId;
    private boolean accepted;
    private Why whyCode;     // optional: validation fail / system fail etc
    private String message;     // optional human-friendly

    public CommitReceipt() {
    }

    public String getCommitId() { return commitId; }
    public void setCommitId(String commitId) { this.commitId = commitId; }

    public boolean isAccepted() { return accepted; }
    public void setAccepted(boolean accepted) { this.accepted = accepted; }

    public Why getWhyCode() {
        return whyCode;
    }

    public void setWhyCode(Why whyCode) {
        this.whyCode = whyCode;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String safeCommitId() {
        if (this.commitId == null) { return null; }
        String t = this.commitId.trim();
        if (t.isEmpty()) { return null; }
        return t;
    }

    private Why safeWhyCode() {
        if (this.whyCode == null) { return null; }
        return this.whyCode;
    }

    private String safeMessage() {
        if (this.message == null) { return null; }
        String t = this.message.trim();
        if (t.isEmpty()) { return null; }
        return t;
    }

    public JsonObject toJson() {
        JsonObject root = new JsonObject();

        String id = safeCommitId();
        if (id != null) {
            root.addProperty("commitId", id);
        }

        root.addProperty("accepted", this.accepted);

        Why wc = this.whyCode;
        if (wc != null) {
            // Contract: Why.toJson() must return a JSON object string
            root.add("whyCode",
                    JsonParser.parseString(wc.toJson()).getAsJsonObject());
        }

        String m = safeMessage();
        if (m != null) {
            root.addProperty("message", m);
        }

        return root;
    }


    public String toJsonString() {
        return toJson().toString();
    }

    public static CommitReceipt fromJson(JsonObject json) {
        if (json == null) {
            return null;
        }

        CommitReceipt r = new CommitReceipt();

        if (json.has("commitId") && !json.get("commitId").isJsonNull()) {
            try {
                r.setCommitId(json.get("commitId").getAsString());
            } catch (RuntimeException re) { }
        }

        if (json.has("accepted") && !json.get("accepted").isJsonNull()) {
            try {
                r.setAccepted(json.get("accepted").getAsBoolean());
            } catch (RuntimeException re) { }
        }

        if (json.has("whyCode") && !json.get("whyCode").isJsonNull()) {
            try {
                JsonElement el = json.get("whyCode");
                if (el != null && el.isJsonObject()) {
                    JsonObject wcJson = el.getAsJsonObject();

                    String reason = null;
                    String details = null;

                    // reason must be a STRING primitive
                    if (wcJson.has("reason") && !wcJson.get("reason").isJsonNull()) {
                        JsonElement re = wcJson.get("reason");
                        if (re != null && re.isJsonPrimitive()) {
                            JsonPrimitive rp = re.getAsJsonPrimitive();
                            if (rp.isString()) {
                                String s = rp.getAsString();
                                if (s != null) {
                                    String t = s.trim();
                                    if (!t.isEmpty()) {
                                        reason = t;
                                    }
                                }
                            }
                        }
                    }

                    // details is optional; if present must be STRING primitive
                    if (wcJson.has("details") && !wcJson.get("details").isJsonNull()) {
                        JsonElement de = wcJson.get("details");
                        if (de != null && de.isJsonPrimitive()) {
                            JsonPrimitive dp = de.getAsJsonPrimitive();
                            if (dp.isString()) {
                                String s = dp.getAsString();
                                if (s != null) {
                                    String t = s.trim();
                                    if (!t.isEmpty()) {
                                        details = t;
                                    }
                                }
                            }
                        }
                    }

                    if (reason != null) {
                        r.setWhyCode(new Why(reason, details));
                    }
                }
            } catch (RuntimeException re) { }
        }


        if (json.has("message") && !json.get("message").isJsonNull()) {
            try {
                r.setMessage(json.get("message").getAsString());
            } catch (RuntimeException re) { }
        }

        return r;
    }


    public static CommitReceipt fromJsonString(String json) {
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

