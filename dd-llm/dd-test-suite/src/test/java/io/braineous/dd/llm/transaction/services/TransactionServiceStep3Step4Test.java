package io.braineous.dd.llm.transaction.services;

import ai.braineous.rag.prompt.cgo.query.QueryRequest;
import ai.braineous.rag.prompt.observe.Console;
import io.braineous.dd.llm.core.model.Why;
import io.braineous.dd.llm.pg.model.PolicyGateResult;
import io.braineous.dd.llm.pg.model.TxGateRequest;
import io.braineous.dd.llm.pg.services.PolicyGateOrchestrator;
import io.braineous.dd.llm.query.client.QueryExecutor;
import io.braineous.dd.llm.query.client.QueryResult;
import io.braineous.dd.llm.transaction.model.TxExecutionRequest;
import io.braineous.dd.llm.transaction.model.TxExecutionResult;
import io.braineous.dd.llm.transaction.model.TxStepRequest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TransactionServiceStep3Step4Test {

    @Test
    public void execute_shouldBuildGateRequestAndAttachApprovedGateResult_whenAllStepsOk() {

        final List<String> translated = new ArrayList<String>();
        final List<TxGateRequest> evaluated = new ArrayList<TxGateRequest>();

        TxQueryRequestTranslator translator = new TxQueryRequestTranslator() {
            @Override
            public QueryRequest<?> translate(TxStepRequest step) {
                translated.add(step.getId());
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
                evaluated.add(request);
                return PolicyGateResult.ok(null, "approved");
            }
        };

        TransactionService svc = new TransactionService(translator, queryExecutor, policyGateOrchestrator);

        TxExecutionRequest req = new TxExecutionRequest();
        req.setDescription("tx-desc");
        req.setPolicyRef("policy:v1");
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

        Console.log("ut.tx.step3step4.approved.result", out.toJson());

        assertNotNull(out);
        assertNotNull(out.getGateResult());
        assertTrue(out.getGateResult().isOk());
        assertTrue(out.isApproved());

        assertEquals(2, translated.size());
        assertEquals("s1", translated.get(0));
        assertEquals("s2", translated.get(1));

        assertEquals(1, evaluated.size());

        TxGateRequest gateRequest = evaluated.get(0);
        assertNotNull(gateRequest);
        assertEquals("tx-desc", gateRequest.getDescription());
        assertEquals("policy:v1", gateRequest.getPolicyRef());

        assertNotNull(gateRequest.getCommitOrder());
        assertEquals(2, gateRequest.getCommitOrder().size());
        assertEquals("s1", gateRequest.getCommitOrder().get(0));
        assertEquals("s2", gateRequest.getCommitOrder().get(1));

        assertNotNull(gateRequest.getStepResults());
        assertEquals(2, gateRequest.getStepResults().size());
        assertEquals("s1", gateRequest.getStepResults().get(0).getId());
        assertEquals("s2", gateRequest.getStepResults().get(1).getId());
    }

    @Test
    public void execute_shouldBuildGateRequestAndAttachRejectedGateResult_whenStepFails() {

        final List<TxGateRequest> evaluated = new ArrayList<TxGateRequest>();
        final List<Integer> callNum = new ArrayList<Integer>();

        TxQueryRequestTranslator translator = new TxQueryRequestTranslator() {
            @Override
            public QueryRequest<?> translate(TxStepRequest step) {
                return null;
            }
        };

        QueryExecutor queryExecutor = new QueryExecutor() {
            @Override
            public QueryResult execute(QueryRequest request) {
                callNum.add(Integer.valueOf(callNum.size() + 1));

                if (callNum.size() == 1) {
                    QueryResult ok = new QueryResult();
                    ok.setOk(true);
                    return ok;
                }

                return QueryResult.fail(new Why("Q_FAIL", "boom"));
            }
        };

        PolicyGateOrchestrator policyGateOrchestrator = new PolicyGateOrchestrator() {
            @Override
            public PolicyGateResult evaluate(TxGateRequest request) {
                evaluated.add(request);
                return PolicyGateResult.fail("s2", "transaction step failed");
            }
        };

        TransactionService svc = new TransactionService(translator, queryExecutor, policyGateOrchestrator);

        TxExecutionRequest req = new TxExecutionRequest();
        req.setDescription("tx-fail");
        req.setPolicyRef("policy:v2");
        req.getCommitOrder().add("s1");
        req.getCommitOrder().add("s2");
        req.getCommitOrder().add("s3");

        TxStepRequest s1 = new TxStepRequest();
        s1.setId("s1");
        s1.setDescription("d1");
        s1.setSql("sql1");

        TxStepRequest s2 = new TxStepRequest();
        s2.setId("s2");
        s2.setDescription("d2");
        s2.setSql("sql2");

        TxStepRequest s3 = new TxStepRequest();
        s3.setId("s3");
        s3.setDescription("d3");
        s3.setSql("sql3");

        req.getSteps().add(s1);
        req.getSteps().add(s2);
        req.getSteps().add(s3);

        TxExecutionResult out = svc.execute(req);

        Console.log("ut.tx.step3step4.failed.result", out.toJson());

        assertNotNull(out);
        assertNotNull(out.getGateResult());
        assertFalse(out.getGateResult().isOk());
        assertFalse(out.isApproved());

        assertEquals(2, callNum.size());
        assertEquals(1, evaluated.size());

        TxGateRequest gateRequest = evaluated.get(0);
        assertNotNull(gateRequest);
        assertEquals("tx-fail", gateRequest.getDescription());
        assertEquals("policy:v2", gateRequest.getPolicyRef());

        assertNotNull(gateRequest.getCommitOrder());
        assertEquals(3, gateRequest.getCommitOrder().size());
        assertEquals("s1", gateRequest.getCommitOrder().get(0));
        assertEquals("s2", gateRequest.getCommitOrder().get(1));
        assertEquals("s3", gateRequest.getCommitOrder().get(2));

        assertNotNull(gateRequest.getStepResults());
        assertEquals(2, gateRequest.getStepResults().size());
        assertEquals("s1", gateRequest.getStepResults().get(0).getId());
        assertEquals("s2", gateRequest.getStepResults().get(1).getId());
    }
}