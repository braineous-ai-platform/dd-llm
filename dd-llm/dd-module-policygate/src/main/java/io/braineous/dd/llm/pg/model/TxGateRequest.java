package io.braineous.dd.llm.pg.model;

import io.braineous.dd.llm.core.model.LLMDDBaseModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Post-execution transaction envelope evaluated by PolicyGate.
 *
 * <p>
 * TxGateRequest represents the complete staged results of a transaction
 * after all steps have executed. It is submitted to the PolicyGate to
 * determine commit or rejection.
 * </p>
 *
 * <p>
 * This object does NOT execute steps. It represents outcome state only.
 * </p>
 */
public class TxGateRequest extends LLMDDBaseModel {

    private String description;

    private List<TxStepResult> stepResults = new ArrayList<TxStepResult>();

    private List<String> commitOrder = new ArrayList<String>();

    private String policyRef;

    public TxGateRequest() {
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<TxStepResult> getStepResults() {
        return stepResults;
    }

    public void setStepResults(List<TxStepResult> stepResults) {
        this.stepResults = stepResults;
    }

    public List<String> getCommitOrder() {
        return commitOrder;
    }

    public void setCommitOrder(List<String> commitOrder) {
        this.commitOrder = commitOrder;
    }

    public String getPolicyRef() {
        return policyRef;
    }

    public void setPolicyRef(String policyRef) {
        this.policyRef = policyRef;
    }
}