package io.braineous.dd.llm.pg.model;

import com.google.gson.JsonObject;

public class PolicyGateResult {

    private boolean ok;
    private String why;
    private String commitId;

    public PolicyGateResult() {
    }

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

    public JsonObject toJson() {
        JsonObject root = new JsonObject();

        root.addProperty("ok", this.ok);

        if (this.why != null) {
            String t = this.why.trim();
            if (!t.isEmpty()) {
                root.addProperty("why", t);
            }
        }

        if (this.commitId != null) {
            String t = this.commitId.trim();
            if (!t.isEmpty()) {
                root.addProperty("commitId", t);
            }
        }

        return root;
    }

    public String toJsonString() {
        return toJson().toString();
    }

}


