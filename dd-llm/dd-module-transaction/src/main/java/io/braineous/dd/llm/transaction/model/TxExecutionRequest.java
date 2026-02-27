package io.braineous.dd.llm.transaction.model;

import io.braineous.dd.llm.core.model.LLMDDBaseModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Runtime Transaction execution request for LLMDD.
 *
 * <p>
 * TxRequest represents a single coarse-grained deterministic execution boundary.
 * Steps execute in declared order, results are staged, and the entire transaction
 * is evaluated under a single policy gate before any commit occurs.
 * </p>
 *
 * <p>
 * The {@code commitOrder} list must contain exactly all step IDs once each.
 * It defines the explicit commit sequence if the policy gate approves.
 * </p>
 *
 * <p>
 * Policy is referenced opaquely via {@code policyRef}. PolicyGate remains an
 * abstraction and interprets this reference.
 * </p>
 */
public class TxExecutionRequest extends LLMDDBaseModel {

    private String description;

    private List<TxStepRequest> steps = new ArrayList<TxStepRequest>();

    private List<String> commitOrder = new ArrayList<String>();

    private String policyRef;

    public TxExecutionRequest() {
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<TxStepRequest> getSteps() {
        return steps;
    }

    public void setSteps(List<TxStepRequest> steps) {
        this.steps = steps;
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