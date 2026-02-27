package io.braineous.dd.llm.transaction.model;

import io.braineous.dd.llm.core.model.LLMDDBaseModel;

/**
 * Runtime Transaction step request for LLMDD.
 *
 * <p>
 * TxStepRequest represents a single deterministic LLM decision unit
 * executed as part of a Transaction boundary.
 * </p>
 *
 * <p>
 * Steps execute in declared order within the transaction. Results are
 * staged and evaluated collectively by the PolicyGate before commit.
 * </p>
 *
 * <p>
 * The {@code id} must be unique within a TxRequest and must appear
 * exactly once in the parent {@code commitOrder}.
 * </p>
 */
public class TxStepRequest extends LLMDDBaseModel {

    private String description;

    private String sql;

    public TxStepRequest() {
    }

    // Deterministic step identity used for commit binding.
    @Override
    public String getId() {
        return super.getId();
    }

    @Override
    public void setId(String id) {
        super.setId(id);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }
}