package io.braineous.dd.llm.transaction.services;

import ai.braineous.rag.prompt.cgo.api.Fact;
import ai.braineous.rag.prompt.models.cgo.graph.GraphBuilder;
import ai.braineous.rag.prompt.observe.Console;
import io.braineous.dd.llm.pg.model.TxStepResult;
import io.braineous.dd.llm.query.client.QueryClient;
import io.braineous.dd.llm.query.client.QueryResult;
import io.braineous.dd.llm.query.client.RESTClient;
import io.braineous.dd.llm.transaction.model.TxExecutionRequest;
import io.braineous.dd.llm.transaction.model.TxExecutionResult;
import io.braineous.dd.llm.transaction.model.TxStepRequest;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

@QuarkusTest
public class TransactionServiceCdiIT {

    private static final String PAY_1001 = "PaymentRequest:PAY-1001";
    private static final String CUST_2001 = "CustomerAccount:CUST-2001";
    private static final String PM_3001 = "PaymentMethod:PM-3001";
    private static final String RISK_4001 = "RiskProfile:RISK-4001";
    private static final String POL_5001 = "MerchantPolicy:POL-5001";

    private static final String PAY_SQL = ""
            + "select decision, reason, code "
            + "from llm "
            + "where factId = '" + PAY_1001 + "' "
            + "and relatedFactIds = '"
            + CUST_2001 + ","
            + PM_3001 + ","
            + RISK_4001 + ","
            + POL_5001 + "' "
            + "control intent = 'decide_payment_capture'";

    @Inject
    QueryClient queryClient;

    @Inject
    TransactionService transactionService;

    @Test
    void cdi_shouldWireQueryOrchestrator_andPreserveInvalidRequestGate() {

        Console.log("IT", "cdi_shouldWireQueryOrchestrator_andPreserveInvalidRequestGate");

        Assertions.assertNotNull(queryClient);
        Assertions.assertTrue(queryClient instanceof RESTClient);
        Console.log("IT", "queryClient.class=" + queryClient.getClass().getName());

        Assertions.assertNotNull(transactionService);
        Console.log("IT", "transactionService.class=" + transactionService.getClass().getName());

        TxExecutionResult result = transactionService.execute(null);
        if (result != null) {
            Console.log("IT", result.toJson());
        } else {
            Console.log("IT", "null");
        }

        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getGateResult());
        Assertions.assertFalse(result.getGateResult().isOk());
        Assertions.assertEquals("INVALID_REQUEST", result.getGateResult().getWhy());
    }

    @Test
    void cdi_shouldExecuteSeededSqlStepThroughRestClient() {

        Console.log("IT", "cdi_shouldExecuteSeededSqlStepThroughRestClient");

        seedPayGraph();

        TxExecutionRequest req = new TxExecutionRequest();
        req.setDescription("it.tx.cdi.sql");
        req.setPolicyRef("policy:cdi");

        TxStepRequest s1 = new TxStepRequest();
        s1.setId("s1");
        s1.setDescription("step1");
        s1.setSql(PAY_SQL);
        req.getSteps().add(s1);
        req.getCommitOrder().add("s1");

        TxExecutionResult out = transactionService.execute(req);
        Console.log("IT", out.toJson());

        Assertions.assertNotNull(out);
        List<TxStepResult> steps = out.getStepResults();
        Assertions.assertNotNull(steps);
        Assertions.assertEquals(1, steps.size());
        Assertions.assertEquals("s1", steps.get(0).getId());

        QueryResult qr0 = steps.get(0).getQueryResult();
        if (qr0 != null) {
            Console.log("IT", "cdi.step0.queryResult=" + qr0.toJson());
        } else {
            Console.log("IT", "cdi.step0.queryResult=null");
        }
        Assertions.assertNotNull(qr0);
        Assertions.assertNotNull(qr0.getRequestJson());
        Assertions.assertNotNull(qr0.getQueryExecutionJson());
    }

    private void seedPayGraph() {
        GraphBuilder graphBuilder = GraphBuilder.getInstance();
        graphBuilder.clear();

        graphBuilder.addNode(payFact(PAY_1001,
                "{\"id\":\"PaymentRequest:PAY-1001\",\"kind\":\"PaymentRequest\",\"mode\":\"atomic\",\"amount\":\"125.00\",\"currency\":\"USD\"}"));
        graphBuilder.addNode(payFact(CUST_2001,
                "{\"id\":\"CustomerAccount:CUST-2001\",\"kind\":\"CustomerAccount\",\"mode\":\"atomic\",\"status\":\"ACTIVE\"}"));
        graphBuilder.addNode(payFact(PM_3001,
                "{\"id\":\"PaymentMethod:PM-3001\",\"kind\":\"PaymentMethod\",\"mode\":\"atomic\",\"type\":\"CARD\"}"));
        graphBuilder.addNode(payFact(RISK_4001,
                "{\"id\":\"RiskProfile:RISK-4001\",\"kind\":\"RiskProfile\",\"mode\":\"atomic\",\"level\":\"LOW\"}"));
        graphBuilder.addNode(payFact(POL_5001,
                "{\"id\":\"MerchantPolicy:POL-5001\",\"kind\":\"MerchantPolicy\",\"mode\":\"atomic\",\"capture\":\"AUTO\"}"));
    }

    private Fact payFact(String id, String text) {
        return new Fact(id, text, new HashSet<String>(), "atomic");
    }
}
