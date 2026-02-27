package io.braineous.dd.llm.transaction.model;

import io.braineous.dd.llm.core.model.LLMDDBaseModel;
import io.braineous.dd.llm.query.client.QueryResult;

/**
 * Post-execution step result within a Transaction boundary.
 *
 * <p>
 * TxStepResult captures the outcome of executing a single TxStepExecutionRequest.
 * It is used to assemble a TxGateRequest for PolicyGate evaluation.
 * </p>
 */
public class TxStepResult extends LLMDDBaseModel {

    private String description;

    private QueryResult queryResult;

    public TxStepResult() {
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

    public QueryResult getQueryResult() {
        return queryResult;
    }

    public void setQueryResult(QueryResult queryResult) {
        this.queryResult = queryResult;
    }

    // Convenience helpers (optional but useful)
    public boolean isOk() {
        if (this.queryResult == null) {
            return false;
        }
        return this.queryResult.isOk();
    }
}
