package io.braineous.dd.llm.core.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * V0 Semantic Spine Base Model.
 *
 * This class represents the minimal deterministic metadata contract
 * shared across Orchestra entities.
 *
 * Fields:
 * - id            : Unique identifier for the entity instance.
 * - version       : Version of the entity definition or contract.
 * - createdAt     : ISO-8601 UTC timestamp when created.
 * - updatedAt     : ISO-8601 UTC timestamp when last modified.
 * - status        : Execution or lifecycle status (must use stable vocabulary).
 * - correlationId : External correlation key for tracing across systems.
 * - snapshotHash  : Deterministic hash of contextual snapshot (if applicable).
 *
 * Notes:
 * - No business logic is allowed in this class.
 * - Timestamps must follow ISO-8601 UTC format.
 * - Status values must remain stable and documented elsewhere.
 *
 * This class is intentionally simple and deterministic.
 */

public class LLMDDBaseModel {

    private static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .disableHtmlEscaping()
            .create();

    private String id;

    private String version;

    private String createdAt;

    private String updatedAt;

    private String status;

    private String correlationId;

    private String snapshotHash;

    public LLMDDBaseModel() {
    }

    //---------------------------------------------

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getSnapshotHash() {
        return snapshotHash;
    }

    public void setSnapshotHash(String snapshotHash) {
        this.snapshotHash = snapshotHash;
    }

    //---------------------------------------------
    public String toJson() {
        return GSON.toJson(this);
    }

    //---------------------------------------------
    public static <T> T fromJson(String json, Class<T> type) {
        if (json == null) {
            throw new IllegalArgumentException("json cannot be null");
        }
        if (json.trim().isEmpty()) {
            throw new IllegalArgumentException("json cannot be blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        return GSON.fromJson(json, type);
    }

}
