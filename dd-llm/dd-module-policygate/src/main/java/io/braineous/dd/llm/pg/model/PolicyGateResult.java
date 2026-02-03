package io.braineous.dd.llm.pg.model;

import com.google.gson.JsonObject;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class PolicyGateResult {

    private boolean ok;
    private String why;
    private String commitId;

    public PolicyGateResult() {
    }

    // -------------------------
    // Getters / setters
    // -------------------------

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public String getWhy() {
        return why;
    }

    public void setWhy(String why) {
        this.why = why;
    }

    public String getCommitId() {
        return commitId;
    }

    public void setCommitId(String commitId) {
        this.commitId = commitId;
    }

    // -------------------------
    // Safe getters
    // -------------------------

    public String safeWhy() {
        return safe(this.why);
    }

    public String safeCommitId() {
        return safe(this.commitId);
    }

    // -------------------------
    // Factories (optional but handy)
    // -------------------------

    public static PolicyGateResult ok(String commitId, String why) {
        PolicyGateResult r = new PolicyGateResult();
        r.setOk(true);
        r.setCommitId(commitId);
        r.setWhy(why);
        return r;
    }

    public static PolicyGateResult fail(String commitId, String why) {
        PolicyGateResult r = new PolicyGateResult();
        r.setOk(false);
        r.setCommitId(commitId);
        r.setWhy(why);
        return r;
    }

    // -------------------------
    // JSON
    // -------------------------

    public JsonObject toJson() {
        JsonObject root = new JsonObject();

        root.addProperty("ok", this.ok);

        String w = safe(this.why);
        if (w != null) {
            root.addProperty("why", w);
        } else {
            root.add("why", null);
        }

        String id = safe(this.commitId);
        if (id != null) {
            root.addProperty("commitId", id);
        } else {
            root.add("commitId", null);
        }

        return root;
    }

    public String toJsonString() {
        return toJson().toString();
    }

    public static PolicyGateResult fromJson(JsonObject json) {
        if (json == null) {
            return null;
        }

        PolicyGateResult r = new PolicyGateResult();

        // ok (default false if missing/unparseable)
        boolean ok = false;
        try {
            if (json.has("ok") && !json.get("ok").isJsonNull()) {
                ok = json.get("ok").getAsBoolean();
            }
        } catch (RuntimeException re) {
            ok = false;
        }
        r.setOk(ok);

        // why (optional)
        String why = null;
        try {
            if (json.has("why") && !json.get("why").isJsonNull()) {
                why = safe(json.get("why").getAsString());
            }
        } catch (RuntimeException re) {
            why = null;
        }
        r.setWhy(why);

        // commitId (optional)
        String commitId = null;
        try {
            if (json.has("commitId") && !json.get("commitId").isJsonNull()) {
                commitId = safe(json.get("commitId").getAsString());
            }
        } catch (RuntimeException re) {
            commitId = null;
        }
        r.setCommitId(commitId);

        return r;
    }

    public static PolicyGateResult fromJsonString(String json) {
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

    // -------------------------
    // helpers
    // -------------------------

    private static String safe(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return null;
        }
        return t;
    }
}



