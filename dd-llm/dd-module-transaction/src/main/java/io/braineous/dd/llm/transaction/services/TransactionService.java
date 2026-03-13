package io.braineous.dd.llm.transaction.services;

import ai.braineous.rag.prompt.cgo.query.QueryRequest;
import ai.braineous.rag.prompt.observe.Console;
import io.braineous.dd.llm.pg.model.PolicyGateResult;
import io.braineous.dd.llm.pg.model.TxGateRequest;
import io.braineous.dd.llm.pg.model.TxStepResult;
import io.braineous.dd.llm.pg.services.PolicyGateOrchestrator;
import io.braineous.dd.llm.query.client.QueryExecutor;
import io.braineous.dd.llm.query.client.QueryResult;
import io.braineous.dd.llm.transaction.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class TransactionService {

    private static final String INVALID_REQUEST_REASON = "INVALID_REQUEST";

    private TxQueryRequestTranslator translator;

    private QueryExecutor queryExecutor;

    @Inject
    private PolicyGateOrchestrator policyGateOrchestrator;

    public TransactionService() {
    }

    // UT seam
    public TransactionService(TxQueryRequestTranslator translator,
                              QueryExecutor queryExecutor,
                              PolicyGateOrchestrator policyGateOrchestrator) {
        this.translator = translator;
        this.queryExecutor = queryExecutor;
        this.policyGateOrchestrator = policyGateOrchestrator;
    }

    public TxExecutionResult execute(TxExecutionRequest request) {

        // ---------------------------------------------------------
        // STEP 1 — Basic validation (v0)
        // ---------------------------------------------------------

        if (request == null) {

            Console.log("tx.validation.fail", "request is null");

            TxExecutionResult result = new TxExecutionResult();

            PolicyGateResult gate = PolicyGateResult.fail(
                    null,
                    INVALID_REQUEST_REASON
            );

            result.setGateResult(gate);

            Console.log("tx.execution.result", result.toJson());

            return result;
        }

        // ---------------------------------------------------------
        // STEP 2 — Execute steps in order (v0)
        // ---------------------------------------------------------
        TxExecutionResult result = this.executeTxSteps(request);

        //---------------------------------------------------
        // STEP 3 - Build TxGateRequest
        //---------------------------------------------------
        TxGateRequest gateRequest = this.createPolicyGateRequest(request, result);

        //---------------------------------------------------
        // STEP 4 - PolicyGate invocation
        //---------------------------------------------------
        PolicyGateResult gateResult = this.policyGateOrchestrator.evaluate(gateRequest);
        result.setGateResult(gateResult);
        Console.log("tx.gate.result", gateResult != null ? gateResult.toJsonString() : null);

        //---------------------------------------------------
        // STEP 5 - Final result stabilization
        //---------------------------------------------------

        //---------------------------------------------------
        // STEP 6 - Step result integrity checks
        //---------------------------------------------------
        this.assertTxResultConsistency(request, result);

        // remaining steps (gate + final) not implemented yet in v0

        return result;
    }

    //---------------------------------------------------------------------------------------
    private TxExecutionResult executeTxSteps(
            TxExecutionRequest request
            ){
        TxExecutionResult result = new TxExecutionResult();
        result.setDescription(request.getDescription());
        result.setPolicyRef(request.getPolicyRef());
        result.setCommitOrder(request.getCommitOrder());

        List<TxStepRequest> steps = request.getSteps();

        if (steps == null) {
            // v0: treat as empty (deeper validation later)
            Console.log("tx.steps.null", "steps is null");
            return result;
        }

        for (int i = 0; i < steps.size(); i++) {

            TxStepRequest step = steps.get(i);
            if (step == null) {
                continue;
            }

            Console.log("tx.step.start", step.getId());

            QueryRequest<?> qr = this.translator.translate(step);

            QueryResult qres = this.queryExecutor.execute(qr);

            TxStepResult sr = new TxStepResult();
            sr.setId(step.getId());
            sr.setDescription(step.getDescription());
            sr.setQueryResult(qres);

            result.getStepResults().add(sr);

            Console.log("tx.step.result", qres != null ? qres.toJson() : null);

            // fail-fast
            if (qres == null || !qres.isOk()) {
                Console.log("tx.step.failfast", step.getId());
                break;
            }
        }

        return result;
    }

    private TxGateRequest createPolicyGateRequest(TxExecutionRequest request,
                                                  TxExecutionResult result) {

        TxGateRequest gateRequest = new TxGateRequest();

        gateRequest.setDescription(request.getDescription());
        gateRequest.setCommitOrder(request.getCommitOrder());
        gateRequest.setPolicyRef(request.getPolicyRef());
        gateRequest.setStepResults(result.getStepResults());

        return gateRequest;
    }

    private void assertTxResultConsistency(TxExecutionRequest request,
                                           TxExecutionResult result) {

        if (request == null) {
            throw new IllegalStateException("request cannot be null");
        }

        if (result == null) {
            throw new IllegalStateException("result cannot be null");
        }

        List<TxStepRequest> requestSteps = request.getSteps();
        List<TxStepResult> resultSteps = result.getStepResults();

        if (requestSteps == null) {
            requestSteps = java.util.Collections.emptyList();
        }

        if (resultSteps == null) {
            throw new IllegalStateException("result.stepResults cannot be null");
        }

        if (resultSteps.size() > requestSteps.size()) {
            throw new IllegalStateException("result.stepResults cannot exceed request.steps");
        }

        for (int i = 0; i < resultSteps.size(); i++) {
            TxStepRequest requestStep = requestSteps.get(i);
            TxStepResult resultStep = resultSteps.get(i);

            if (requestStep == null) {
                throw new IllegalStateException("request.steps contains null at index " + i);
            }

            if (resultStep == null) {
                throw new IllegalStateException("result.stepResults contains null at index " + i);
            }

            String requestStepId = requestStep.getId();
            String resultStepId = resultStep.getId();

            if (requestStepId == null && resultStepId == null) {
                continue;
            }

            if (requestStepId == null || resultStepId == null || !requestStepId.equals(resultStepId)) {
                throw new IllegalStateException("result.stepResults out of sync with request.steps at index " + i);
            }
        }

        List<String> requestCommitOrder = request.getCommitOrder();
        List<String> resultCommitOrder = result.getCommitOrder();

        if (requestCommitOrder == null) {
            requestCommitOrder = java.util.Collections.emptyList();
        }

        if (resultCommitOrder == null) {
            throw new IllegalStateException("result.commitOrder cannot be null");
        }

        if (requestCommitOrder.size() != resultCommitOrder.size()) {
            throw new IllegalStateException("result.commitOrder size mismatch");
        }

        for (int i = 0; i < requestCommitOrder.size(); i++) {
            String requestCommitId = requestCommitOrder.get(i);
            String resultCommitId = resultCommitOrder.get(i);

            if (requestCommitId == null && resultCommitId == null) {
                continue;
            }

            if (requestCommitId == null || resultCommitId == null || !requestCommitId.equals(resultCommitId)) {
                throw new IllegalStateException("result.commitOrder out of sync with request.commitOrder at index " + i);
            }
        }
    }
}