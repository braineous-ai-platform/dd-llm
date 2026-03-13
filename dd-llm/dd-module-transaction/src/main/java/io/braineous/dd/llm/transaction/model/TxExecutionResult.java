package io.braineous.dd.llm.transaction.model;

import io.braineous.dd.llm.core.model.LLMDDBaseModel;
import io.braineous.dd.llm.pg.model.PolicyGateResult;
import io.braineous.dd.llm.pg.model.TxStepResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Final transaction-level outcome returned to the caller.
 *
 * <p>
 * TxExecutionResult is the single outward-facing surface for a transaction execution.
 * It includes staged step results and the PolicyGate decision that determines whether
 * commits are eligible to proceed under the declared commit order.
 * </p>
 *
 * <p>
 * This object does NOT perform commits. It represents post-execution state only.
 * </p>
 */
public class TxExecutionResult extends LLMDDBaseModel {

    private String description;

    private List<TxStepResult> stepResults = new ArrayList<TxStepResult>();

    private List<String> commitOrder = new ArrayList<String>();

    private String policyRef;

    private PolicyGateResult gateResult;

    public TxExecutionResult() {
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

    public PolicyGateResult getGateResult() {
        return gateResult;
    }

    public void setGateResult(PolicyGateResult gateResult) {
        this.gateResult = gateResult;
    }

    // ---------------------------------------------------------
    // Helper
    // ---------------------------------------------------------

    public boolean isApproved() {
        if (this.gateResult == null) {
            return false;
        }
        return this.gateResult.isOk();
    }
}