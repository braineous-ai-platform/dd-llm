package io.braineous.dd.llm.agent.client;

import ai.braineous.rag.prompt.cgo.api.Fact;
import ai.braineous.rag.prompt.cgo.api.QueryExecution;
import ai.braineous.rag.prompt.cgo.api.ValidateTask;
import ai.braineous.rag.prompt.models.cgo.graph.GraphBuilder;
import ai.braineous.rag.prompt.observe.Console;

import io.braineous.dd.llm.pg.model.TxStepResult;
import io.braineous.dd.llm.pg.services.PolicyGateOrchestrator;
import io.braineous.dd.llm.query.client.QueryClient;
import io.braineous.dd.llm.query.client.QueryResult;
import io.braineous.dd.llm.query.client.RESTClient;
import io.braineous.dd.llm.transaction.model.TxExecutionRequest;
import io.braineous.dd.llm.transaction.model.TxExecutionResult;
import io.braineous.dd.llm.transaction.model.TxStepRequest;
import io.braineous.dd.llm.transaction.services.TransactionService;
import io.quarkus.test.junit.QuarkusTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

@QuarkusTest
public class AgentClientIT {

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

    @Test
    void agent_client_transaction_service_query_orchestrator_end_to_end() {

        Console.log("IT", "agent_client_transaction_service_query_orchestrator_end_to_end");

        seedPayGraph();

        QueryClient queryClient = new RESTClient();
        PolicyGateOrchestrator gate = new PolicyGateOrchestrator();
        TransactionService service = new TransactionService(queryClient, gate);
        AgentClient client = new AgentClient(service);

        TxExecutionRequest req = new TxExecutionRequest();
        req.setDescription("it.agent.ok");
        req.setPolicyRef("policy:agent:it");

        TxStepRequest s1 = new TxStepRequest();
        s1.setId("s1");
        s1.setDescription("step1");
        s1.setSql(PAY_SQL);

        TxStepRequest s2 = new TxStepRequest();
        s2.setId("s2");
        s2.setDescription("step2");
        s2.setSql(PAY_SQL);

        req.getSteps().add(s1);
        req.getSteps().add(s2);

        req.getCommitOrder().add("s1");
        req.getCommitOrder().add("s2");

        Console.log("IT", "request=" + req.getDescription());
        Console.log("IT", "sql=" + PAY_SQL);

        TxExecutionResult out = client.execute(req);

        Console.log("IT", out.toJson());

        Assertions.assertNotNull(out);
        Assertions.assertNotNull(out.getGateResult());

        Assertions.assertEquals("it.agent.ok", out.getDescription());
        Assertions.assertEquals("policy:agent:it", out.getPolicyRef());

        List<TxStepResult> steps = out.getStepResults();

        Assertions.assertNotNull(steps);
        Assertions.assertEquals(2, steps.size());

        Assertions.assertEquals("s1", steps.get(0).getId());
        Assertions.assertEquals("s2", steps.get(1).getId());

        Assertions.assertNotNull(out.getCommitOrder());
        Assertions.assertEquals(2, out.getCommitOrder().size());
        Assertions.assertEquals("s1", out.getCommitOrder().get(0));
        Assertions.assertEquals("s2", out.getCommitOrder().get(1));

        QueryResult qr0 = steps.get(0).getQueryResult();
        QueryResult qr1 = steps.get(1).getQueryResult();

        if (qr0 != null) {
            Console.log("IT", "step0.queryResult=" + qr0.toJson());
        } else {
            Console.log("IT", "step0.queryResult=null");
        }
        if (qr1 != null) {
            Console.log("IT", "step1.queryResult=" + qr1.toJson());
        } else {
            Console.log("IT", "step1.queryResult=null");
        }

        Assertions.assertNotNull(qr0);
        Assertions.assertNotNull(qr1);

        Assertions.assertNotNull(qr0.getRequestJson());
        Assertions.assertNotNull(qr1.getRequestJson());

        Assertions.assertNotNull(qr0.getQueryExecutionJson());
        Assertions.assertNotNull(qr1.getQueryExecutionJson());

        QueryExecution<?> ex0 = QueryExecution.fromJson(qr0.getQueryExecutionJson());
        QueryExecution<?> ex1 = QueryExecution.fromJson(qr1.getQueryExecutionJson());

        if (ex0 != null) {
            Console.log("IT", "step0.exec=" + ex0.toJson());
        } else {
            Console.log("IT", "step0.exec=null");
        }
        if (ex1 != null) {
            Console.log("IT", "step1.exec=" + ex1.toJson());
        } else {
            Console.log("IT", "step1.exec=null");
        }

        Assertions.assertNotNull(ex0);
        Assertions.assertNotNull(ex1);

        Assertions.assertNotNull(ex0.getRequest());
        Assertions.assertNotNull(ex1.getRequest());

        Assertions.assertNotNull(ex0.getRequest().getMeta());
        Assertions.assertNotNull(ex1.getRequest().getMeta());

        Assertions.assertNotNull(ex0.getRequest().getMeta().getQueryKind());
        Assertions.assertNotNull(ex1.getRequest().getMeta().getQueryKind());

        Assertions.assertNotNull(ex0.getRequest().getTask());
        Assertions.assertNotNull(ex1.getRequest().getTask());

        Assertions.assertTrue(ex0.getRequest().getTask() instanceof ValidateTask);
        Assertions.assertTrue(ex1.getRequest().getTask() instanceof ValidateTask);

        ValidateTask t0 = (ValidateTask) ex0.getRequest().getTask();
        ValidateTask t1 = (ValidateTask) ex1.getRequest().getTask();

        Assertions.assertEquals(PAY_1001, t0.getFactId());
        Assertions.assertEquals(PAY_1001, t1.getFactId());

        Assertions.assertNotNull(ex0.getLlmResponseValidation());
        Assertions.assertNotNull(ex1.getLlmResponseValidation());
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
