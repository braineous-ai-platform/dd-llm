package io.braineous.dd.llm.cr.model;

import io.braineous.dd.llm.core.model.Why;

public class CommitAuditView {

    private String commitId;

    private CommitEvent event;
    private CommitRequest request;
    private CommitReceipt receipt;

    private CommitAuditStatus status;

    private Why whyCode;
    private String message;
    private String createdAt;

    public CommitAuditView() {
    }

    // -------------------------
    // Factory
    // -------------------------

    public static CommitAuditView from(
            String commitId,
            CommitEvent event,
            CommitRequest request,
            CommitReceipt receipt
    ) {
        String id = safe(commitId);
        if (id == null) {
            return null;
        }

        CommitAuditView v = new CommitAuditView();
        v.setCommitId(id);

        v.setEvent(event);
        v.setRequest(request);
        v.setReceipt(receipt);

        // best-effort: attempt time
        if (event != null) {
            String ca = event.safeCreatedAt();
            if (ca != null) {
                v.setCreatedAt(ca);
            }
        }

        // receipt is the outcome truth surface
        if (receipt != null) {
            v.setWhyCode(receipt.getWhyCode());
            v.setMessage(safe(receipt.getMessage()));
        }

        v.setStatus(resolveStatus(receipt));
        return v;
    }

    // -------------------------
    // Status resolution
    // -------------------------

    public static CommitAuditStatus resolveStatus(CommitReceipt receipt) {
        if (receipt == null) {
            return CommitAuditStatus.PENDING;
        }

        if (receipt.isAccepted()) {
            return CommitAuditStatus.COMPLETED_ACCEPTED;
        }

        return CommitAuditStatus.COMPLETED_REJECTED;
    }

    // -------------------------
    // Getters / setters
    // -------------------------

    public String getCommitId() { return commitId; }
    public void setCommitId(String commitId) { this.commitId = commitId; }

    public CommitEvent getEvent() { return event; }
    public void setEvent(CommitEvent event) { this.event = event; }

    public CommitRequest getRequest() { return request; }
    public void setRequest(CommitRequest request) { this.request = request; }

    public CommitReceipt getReceipt() { return receipt; }
    public void setReceipt(CommitReceipt receipt) { this.receipt = receipt; }

    public CommitAuditStatus getStatus() { return status; }
    public void setStatus(CommitAuditStatus status) { this.status = status; }

    public Why getWhyCode() { return whyCode; }
    public void setWhyCode(Why whyCode) { this.whyCode = whyCode; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

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

    public String safeCommitId() {
        if (this.commitId == null) { return null; }
        String t = this.commitId.trim();
        if (t.isEmpty()) { return null; }
        return t;
    }

    // -------------------------
    // JSON
    // -------------------------

    public com.google.gson.JsonObject toJson() {
        com.google.gson.JsonObject root = new com.google.gson.JsonObject();

        String id = safe(this.commitId);
        if (id != null) {
            root.addProperty("commitId", id);
        }

        if (this.status != null) {
            root.addProperty("status", this.status.name());
        }

        String ca = safe(this.createdAt);
        if (ca != null) {
            root.addProperty("createdAt", ca);
        }

        if (this.message != null) {
            String m = safe(this.message);
            if (m != null) {
                root.addProperty("message", m);
            }
        }

        if (this.whyCode != null) {
            root.add(
                    "whyCode",
                    com.google.gson.JsonParser
                            .parseString(this.whyCode.toJson())
                            .getAsJsonObject()
            );
        }

        if (this.event != null) {
            root.add(
                    "event",
                    com.google.gson.JsonParser
                            .parseString(this.event.toJsonString())
                            .getAsJsonObject()
            );
        }

        if (this.request != null) {
            root.add(
                    "request",
                    com.google.gson.JsonParser
                            .parseString(this.request.toJsonString())
                            .getAsJsonObject()
            );
        }

        if (this.receipt != null) {
            root.add(
                    "receipt",
                    com.google.gson.JsonParser
                            .parseString(this.receipt.toJsonString())
                            .getAsJsonObject()
            );
        }

        return root;
    }

    public String toJsonString() {
        return toJson().toString();
    }

    public static CommitAuditView fromJson(com.google.gson.JsonObject json) {
        if (json == null) {
            return null;
        }

        String id = null;
        try {
            if (json.has("commitId") && !json.get("commitId").isJsonNull()) {
                id = safe(json.get("commitId").getAsString());
            }
        } catch (RuntimeException re) { }

        if (id == null) {
            return null;
        }

        CommitEvent event = null;
        CommitRequest request = null;
        CommitReceipt receipt = null;

        try {
            if (json.has("event") && !json.get("event").isJsonNull()) {
                com.google.gson.JsonElement el = json.get("event");
                if (el != null && el.isJsonObject()) {
                    event = CommitEvent.fromJson(el.getAsJsonObject());
                }
            }
        } catch (RuntimeException re) { }

        try {
            if (json.has("request") && !json.get("request").isJsonNull()) {
                com.google.gson.JsonElement el = json.get("request");
                if (el != null && el.isJsonObject()) {
                    request = CommitRequest.fromJson(el.getAsJsonObject());
                }
            }
        } catch (RuntimeException re) { }

        try {
            if (json.has("receipt") && !json.get("receipt").isJsonNull()) {
                com.google.gson.JsonElement el = json.get("receipt");
                if (el != null && el.isJsonObject()) {
                    receipt = CommitReceipt.fromJson(el.getAsJsonObject());
                }
            }
        } catch (RuntimeException re) { }

        // If all fragments are missing, treat as missing resource.
        if (event == null && request == null && receipt == null) {
            return null;
        }

        // Derive status/createdAt/why/message via factory (single source of truth).
        return CommitAuditView.from(id, event, request, receipt);
    }

    public static CommitAuditView fromJsonString(String json) {
        if (json == null) {
            return null;
        }
        String t = json.trim();
        if (t.isEmpty()) {
            return null;
        }

        try {
            com.google.gson.JsonElement el = com.google.gson.JsonParser.parseString(t);
            if (el == null || !el.isJsonObject()) {
                return null;
            }
            return fromJson(el.getAsJsonObject());
        } catch (RuntimeException re) {
            return null;
        }
    }

}


