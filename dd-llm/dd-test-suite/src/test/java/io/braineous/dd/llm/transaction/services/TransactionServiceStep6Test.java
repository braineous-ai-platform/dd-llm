package io.braineous.dd.llm.transaction.services;

import ai.braineous.rag.prompt.cgo.query.QueryRequest;
import ai.braineous.rag.prompt.observe.Console;
import io.braineous.dd.llm.pg.model.PolicyGateResult;
import io.braineous.dd.llm.pg.model.TxGateRequest;
import io.braineous.dd.llm.pg.services.PolicyGateOrchestrator;
import io.braineous.dd.llm.query.client.QueryExecutor;
import io.braineous.dd.llm.query.client.QueryResult;
import io.braineous.dd.llm.transaction.model.TxExecutionRequest;
import io.braineous.dd.llm.transaction.model.TxExecutionResult;
import io.braineous.dd.llm.transaction.model.TxStepRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TransactionServiceStep6Test {

    @Test
    public void execute_shouldPassConsistencyChecks_whenRequestAndResultStayInSync() {

        TxQueryRequestTranslator translator = new TxQueryRequestTranslator() {
            @Override
            public QueryRequest<?> translate(TxStepRequest step) {
                return null;
            }
        };

        QueryExecutor queryExecutor = new QueryExecutor() {
            @Override
            public QueryResult execute(QueryRequest request) {
                QueryResult ok = new QueryResult();
                ok.setOk(true);
                return ok;
            }
        };

        PolicyGateOrchestrator policyGateOrchestrator = new PolicyGateOrchestrator() {
            @Override
            public PolicyGateResult evaluate(TxGateRequest request) {
                return PolicyGateResult.ok(null, "approved");
            }
        };

        TransactionService svc = new TransactionService(translator, queryExecutor, policyGateOrchestrator);

        TxExecutionRequest req = new TxExecutionRequest();
        req.setDescription("tx-step6-ok");
        req.setPolicyRef("policy:step6");
        req.getCommitOrder().add("s1");
        req.getCommitOrder().add("s2");

        TxStepRequest s1 = new TxStepRequest();
        s1.setId("s1");
        s1.setDescription("d1");
        s1.setSql("sql1");

        TxStepRequest s2 = new TxStepRequest();
        s2.setId("s2");
        s2.setDescription("d2");
        s2.setSql("sql2");

        req.getSteps().add(s1);
        req.getSteps().add(s2);

        TxExecutionResult out = svc.execute(req);

        Console.log("ut.tx.step6.sync.result", out.toJson());

        assertNotNull(out);
        assertNotNull(out.getGateResult());
        assertTrue(out.getGateResult().isOk());
        assertTrue(out.isApproved());

        assertNotNull(out.getStepResults());
        assertEquals(2, out.getStepResults().size());
        assertEquals("s1", out.getStepResults().get(0).getId());
        assertEquals("s2", out.getStepResults().get(1).getId());

        assertNotNull(out.getCommitOrder());
        assertEquals(2, out.getCommitOrder().size());
        assertEquals("s1", out.getCommitOrder().get(0));
        assertEquals("s2", out.getCommitOrder().get(1));
    }

    @Test
    public void execute_shouldFailConsistencyCheck_whenStagedStepIdsDriftFromRequestOrder() {

        TxQueryRequestTranslator translator = new TxQueryRequestTranslator() {
            @Override
            public QueryRequest<?> translate(TxStepRequest step) {
                return null;
            }
        };

        QueryExecutor queryExecutor = new QueryExecutor() {
            @Override
            public QueryResult execute(QueryRequest request) {
                QueryResult ok = new QueryResult();
                ok.setOk(true);
                return ok;
            }
        };

        PolicyGateOrchestrator policyGateOrchestrator = new PolicyGateOrchestrator() {
            @Override
            public PolicyGateResult evaluate(TxGateRequest request) {

                if (request.getStepResults() != null && request.getStepResults().size() > 1) {
                    request.getStepResults().get(1).setId("BROKEN_STEP_ID");
                }

                return PolicyGateResult.ok(null, "approved");
            }
        };

        TransactionService svc = new TransactionService(translator, queryExecutor, policyGateOrchestrator);

        TxExecutionRequest req = new TxExecutionRequest();
        req.setDescription("tx-step6-broken");
        req.setPolicyRef("policy:step6");
        req.getCommitOrder().add("s1");
        req.getCommitOrder().add("s2");

        TxStepRequest s1 = new TxStepRequest();
        s1.setId("s1");
        s1.setDescription("d1");
        s1.setSql("sql1");

        TxStepRequest s2 = new TxStepRequest();
        s2.setId("s2");
        s2.setDescription("d2");
        s2.setSql("sql2");

        req.getSteps().add(s1);
        req.getSteps().add(s2);

        IllegalStateException ex = assertThrows(IllegalStateException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                svc.execute(req);
            }
        });

        Console.log("ut.tx.step6.drift.error", ex.getMessage());

        assertEquals("result.stepResults out of sync with request.steps at index 1", ex.getMessage());
    }
}