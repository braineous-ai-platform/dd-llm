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
    public TransactionService(TxQueryRequestTranslator translator, QueryExecutor queryExecutor) {
        this.translator = translator;
        this.queryExecutor = queryExecutor;
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

        //---------------------------------------------------
        // STEP 5 - Final result stabilization
        //---------------------------------------------------

        //---------------------------------------------------
        // STEP 6 - Step result integrity checks
        //---------------------------------------------------

        // remaining steps (gate + final) not implemented yet
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
}