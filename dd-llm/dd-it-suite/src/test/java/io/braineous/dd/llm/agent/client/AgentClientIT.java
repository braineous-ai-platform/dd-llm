package io.braineous.dd.llm.agent.client;

import ai.braineous.rag.prompt.cgo.api.GraphContext;
import ai.braineous.rag.prompt.cgo.api.LlmAdapter;
import ai.braineous.rag.prompt.cgo.api.Meta;
import ai.braineous.rag.prompt.cgo.api.QueryExecution;
import ai.braineous.rag.prompt.cgo.api.ValidateTask;
import ai.braineous.rag.prompt.cgo.query.QueryRequest;
import ai.braineous.rag.prompt.observe.Console;

import com.google.gson.JsonObject;
import io.braineous.dd.llm.pg.model.PolicyGateResult;
import io.braineous.dd.llm.pg.model.TxStepResult;
import io.braineous.dd.llm.pg.services.PolicyGateOrchestrator;
import io.braineous.dd.llm.query.client.QueryExecutor;
import io.braineous.dd.llm.query.client.QueryOrchestrator;
import io.braineous.dd.llm.query.client.QueryResult;
import io.braineous.dd.llm.transaction.model.TxExecutionRequest;
import io.braineous.dd.llm.transaction.model.TxExecutionResult;
import io.braineous.dd.llm.transaction.model.TxStepRequest;
import io.braineous.dd.llm.transaction.services.TransactionService;
import io.braineous.dd.llm.transaction.services.TxQueryRequestTranslator;
import io.quarkus.test.junit.QuarkusTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

@QuarkusTest
public class AgentClientIT {

    @Test
    void agent_client_transaction_service_query_orchestrator_end_to_end() {

        Console.log("IT", "agent_client_transaction_service_query_orchestrator_end_to_end");

        QueryOrchestrator orchestrator = new QueryOrchestrator();

        QueryExecutor executor = new QueryExecutor() {
            @Override
            public QueryResult execute(QueryRequest req) {
                return orchestrator.execute(req);
            }
        };

        TxQueryRequestTranslator translator = new TxQueryRequestTranslator() {

            @Override
            public QueryRequest translate(TxStepRequest step) {

                String stepId = step.getId();

                String qk = "it_agent_qk_" + stepId;
                String factId = "Flight:AGENT_" + stepId;

                Meta meta = new Meta("v1", qk, "agent client it");
                GraphContext ctx = new GraphContext(Map.of());
                ValidateTask task = new ValidateTask("validate agent step", factId);

                QueryRequest req = new QueryRequest(meta, ctx, task);
                req.setAdapter(new OkAdapter(factId));

                return req;
            }
        };

        PolicyGateOrchestrator gate = new PolicyGateOrchestrator() {

            @Override
            public PolicyGateResult evaluate(io.braineous.dd.llm.pg.model.TxGateRequest req) {

                List<TxStepResult> steps = req.getStepResults();

                for (int i = 0; i < steps.size(); i++) {
                    TxStepResult r = steps.get(i);
                    if (r == null || !r.isOk()) {
                        return PolicyGateResult.fail(r != null ? r.getId() : null, "step failed");
                    }
                }

                return PolicyGateResult.ok(null, "approved");
            }
        };

        TransactionService service = new TransactionService(translator, executor, gate);
        AgentClient client = new AgentClient(service);

        TxExecutionRequest req = new TxExecutionRequest();

        req.setDescription("it.agent.ok");
        req.setPolicyRef("policy:agent:it");

        TxStepRequest s1 = new TxStepRequest();
        s1.setId("s1");
        s1.setDescription("step1");

        TxStepRequest s2 = new TxStepRequest();
        s2.setId("s2");
        s2.setDescription("step2");

        req.getSteps().add(s1);
        req.getSteps().add(s2);

        req.getCommitOrder().add("s1");
        req.getCommitOrder().add("s2");

        Console.log("IT", "request=" + req.getDescription());

        TxExecutionResult out = client.execute(req);

        Console.log("IT", out.toJson());

        Assertions.assertNotNull(out);
        Assertions.assertNotNull(out.getGateResult());
        Assertions.assertTrue(out.getGateResult().isOk());
        Assertions.assertTrue(out.isApproved());

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

        Console.log("IT", "step0.queryResult=" + (qr0 != null ? qr0.toJson() : null));
        Console.log("IT", "step1.queryResult=" + (qr1 != null ? qr1.toJson() : null));

        Assertions.assertNotNull(qr0);
        Assertions.assertNotNull(qr1);

        Assertions.assertTrue(qr0.isOk());
        Assertions.assertTrue(qr1.isOk());

        Assertions.assertNotNull(qr0.getRequestJson());
        Assertions.assertNotNull(qr1.getRequestJson());

        Assertions.assertNotNull(qr0.getQueryExecutionJson());
        Assertions.assertNotNull(qr1.getQueryExecutionJson());

        QueryExecution<?> ex0 = QueryExecution.fromJson(qr0.getQueryExecutionJson());
        QueryExecution<?> ex1 = QueryExecution.fromJson(qr1.getQueryExecutionJson());

        Console.log("IT", "step0.exec=" + (ex0 != null ? ex0.toJson() : null));
        Console.log("IT", "step1.exec=" + (ex1 != null ? ex1.toJson() : null));

        Assertions.assertNotNull(ex0);
        Assertions.assertNotNull(ex1);

        Assertions.assertTrue(ex0.isOk());
        Assertions.assertTrue(ex1.isOk());

        Assertions.assertNotNull(ex0.getRequest());
        Assertions.assertNotNull(ex1.getRequest());

        Assertions.assertNotNull(ex0.getRequest().getMeta());
        Assertions.assertNotNull(ex1.getRequest().getMeta());

        Assertions.assertEquals("it_agent_qk_s1", ex0.getRequest().getMeta().getQueryKind());
        Assertions.assertEquals("it_agent_qk_s2", ex1.getRequest().getMeta().getQueryKind());

        Assertions.assertNotNull(ex0.getRequest().getTask());
        Assertions.assertNotNull(ex1.getRequest().getTask());

        Assertions.assertTrue(ex0.getRequest().getTask() instanceof ValidateTask);
        Assertions.assertTrue(ex1.getRequest().getTask() instanceof ValidateTask);

        ValidateTask t0 = (ValidateTask) ex0.getRequest().getTask();
        ValidateTask t1 = (ValidateTask) ex1.getRequest().getTask();

        Assertions.assertEquals("Flight:AGENT_s1", t0.getFactId());
        Assertions.assertEquals("Flight:AGENT_s2", t1.getFactId());

        Assertions.assertNotNull(ex0.getLlmResponseValidation());
        Assertions.assertNotNull(ex1.getLlmResponseValidation());

        Assertions.assertTrue(ex0.getLlmResponseValidation().isOk());
        Assertions.assertTrue(ex1.getLlmResponseValidation().isOk());

        Assertions.assertEquals("queryresult.contract.ok", ex0.getLlmResponseValidation().getCode());
        Assertions.assertEquals("queryresult.contract.ok", ex1.getLlmResponseValidation().getCode());

        Assertions.assertEquals("llm_response_validation", ex0.getLlmResponseValidation().getStage());
        Assertions.assertEquals("llm_response_validation", ex1.getLlmResponseValidation().getStage());

        Assertions.assertNull(ex0.getLlmResponseValidation().getAnchorId());
        Assertions.assertNull(ex1.getLlmResponseValidation().getAnchorId());

        Assertions.assertNull(ex0.getRequest().getAdapter());
        Assertions.assertNull(ex1.getRequest().getAdapter());
    }

    private static class OkAdapter extends LlmAdapter {

        private final String anchor;

        OkAdapter(String anchor) {
            this.anchor = anchor;
        }

        @Override
        public String invokeLlm(QueryRequest request, JsonObject prompt) {
            return "{\"result\":{\"ok\":\"true\","
                    + "\"code\":\"response.contract.ok\","
                    + "\"message\":\"ok\","
                    + "\"stage\":\"llm_response_validation\","
                    + "\"anchorId\":\"" + anchor + "\","
                    + "\"metadata\":{}}}";
        }
    }
}