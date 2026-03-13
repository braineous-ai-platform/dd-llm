package io.braineous.dd.llm.transaction.services;

import ai.braineous.rag.prompt.cgo.query.QueryRequest;
import ai.braineous.rag.prompt.observe.Console;
import io.braineous.dd.llm.core.model.Why;
import io.braineous.dd.llm.pg.services.PolicyGateOrchestrator;
import io.braineous.dd.llm.query.client.QueryExecutor;
import io.braineous.dd.llm.query.client.QueryResult;
import io.braineous.dd.llm.transaction.model.TxExecutionRequest;
import io.braineous.dd.llm.transaction.model.TxExecutionResult;
import io.braineous.dd.llm.transaction.model.TxStepRequest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TransactionServiceStep2Test {

    @Test
    public void execute_shouldStageResultsInStepOrder_whenAllStepsOk() {

        final List<String> translated = new ArrayList<String>();
        final List<String> executed = new ArrayList<String>();

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
                executed.add("exec");
                QueryResult r = new QueryResult();
                r.setOk(true);
                return r;
            }
        };

        PolicyGateOrchestrator policyGateOrchestrator = new PolicyGateOrchestrator();

        TransactionService svc = new TransactionService(translator, queryExecutor, policyGateOrchestrator);

        TxExecutionRequest req = new TxExecutionRequest();
        req.setDescription("tx");
        req.setPolicyRef("policy:x");
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

        Console.log("ut.tx.step2.all_ok.result", out.toJson());

        assertNotNull(out);
        assertEquals(2, out.getStepResults().size());
        assertEquals("s1", out.getStepResults().get(0).getId());
        assertEquals("s2", out.getStepResults().get(1).getId());

        assertEquals(2, translated.size());
        assertEquals("s1", translated.get(0));
        assertEquals("s2", translated.get(1));

        assertEquals(2, executed.size());
    }

    @Test
    public void execute_shouldFailFast_whenAStepReturnsNotOk() {

        final List<String> translated = new ArrayList<String>();
        final List<Integer> callNum = new ArrayList<Integer>();

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
                callNum.add(Integer.valueOf(callNum.size() + 1));

                if (callNum.size() == 1) {
                    QueryResult ok = new QueryResult();
                    ok.setOk(true);
                    return ok;
                }

                return QueryResult.fail(new Why("Q_FAIL", "boom"));
            }
        };

        PolicyGateOrchestrator policyGateOrchestrator = new PolicyGateOrchestrator();

        TransactionService svc = new TransactionService(translator, queryExecutor, policyGateOrchestrator);

        TxExecutionRequest req = new TxExecutionRequest();

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

        Console.log("ut.tx.step2.failfast.result", out.toJson());

        assertNotNull(out);

        assertEquals(2, out.getStepResults().size());
        assertEquals("s1", out.getStepResults().get(0).getId());
        assertEquals("s2", out.getStepResults().get(1).getId());

        assertEquals(2, translated.size());
        assertEquals("s1", translated.get(0));
        assertEquals("s2", translated.get(1));

        assertEquals(2, callNum.size());
    }

    @Test
    public void execute_shouldReturnResultWithCopiedEnvelopeFields_whenRequestNonNull() {

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

        PolicyGateOrchestrator policyGateOrchestrator = new PolicyGateOrchestrator();

        TransactionService svc = new TransactionService(translator, queryExecutor, policyGateOrchestrator);

        TxExecutionRequest req = new TxExecutionRequest();
        req.setDescription("d");
        req.setPolicyRef("p");
        req.getCommitOrder().add("s1");

        TxStepRequest s1 = new TxStepRequest();
        s1.setId("s1");
        s1.setDescription("d1");
        s1.setSql("sql1");
        req.getSteps().add(s1);

        TxExecutionResult out = svc.execute(req);

        Console.log("ut.tx.step2.envelope.result", out.toJson());

        assertNotNull(out);
        assertEquals("d", out.getDescription());
        assertEquals("p", out.getPolicyRef());
        assertNotNull(out.getCommitOrder());
        assertEquals(1, out.getCommitOrder().size());
        assertEquals("s1", out.getCommitOrder().get(0));
    }
}