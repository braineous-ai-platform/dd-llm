package io.braineous.dd.llm.transaction.services;

import ai.braineous.rag.prompt.cgo.api.Fact;
import ai.braineous.rag.prompt.cgo.api.QueryExecution;
import ai.braineous.rag.prompt.cgo.api.ValidateTask;
import ai.braineous.rag.prompt.models.cgo.graph.GraphBuilder;
import ai.braineous.rag.prompt.observe.Console;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.braineous.dd.llm.pg.model.TxStepResult;
import io.braineous.dd.llm.pg.services.PolicyGateOrchestrator;
import io.braineous.dd.llm.query.client.QueryClient;
import io.braineous.dd.llm.query.client.QueryResult;
import io.braineous.dd.llm.query.client.RESTClient;

import io.braineous.dd.llm.transaction.model.TxExecutionRequest;
import io.braineous.dd.llm.transaction.model.TxExecutionResult;
import io.braineous.dd.llm.transaction.model.TxStepRequest;
import io.quarkus.test.junit.QuarkusTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

@QuarkusTest
public class TransactionServiceIT {

    private static final String PAY_1001 = "PaymentRequest:PAY-1001";
    private static final String CUST_2001 = "CustomerAccount:CUST-2001";
    private static final String PM_3001 = "PaymentMethod:PM-3001";
    private static final String RISK_4001 = "RiskProfile:RISK-4001";
    private static final String POL_5001 = "MerchantPolicy:POL-5001";

    private static final String PAY_1001_TEXT =
            "{\"id\":\"PaymentRequest:PAY-1001\",\"kind\":\"PaymentRequest\",\"mode\":\"atomic\",\"amount\":\"125.00\",\"currency\":\"USD\"}";
    private static final String CUST_2001_TEXT =
            "{\"id\":\"CustomerAccount:CUST-2001\",\"kind\":\"CustomerAccount\",\"mode\":\"atomic\",\"status\":\"ACTIVE\"}";
    private static final String PM_3001_TEXT =
            "{\"id\":\"PaymentMethod:PM-3001\",\"kind\":\"PaymentMethod\",\"mode\":\"atomic\",\"type\":\"CARD\"}";
    private static final String RISK_4001_TEXT =
            "{\"id\":\"RiskProfile:RISK-4001\",\"kind\":\"RiskProfile\",\"mode\":\"atomic\",\"level\":\"LOW\"}";
    private static final String POL_5001_TEXT =
            "{\"id\":\"MerchantPolicy:POL-5001\",\"kind\":\"MerchantPolicy\",\"mode\":\"atomic\",\"capture\":\"AUTO\"}";

    private static final String PAY_SQL = ""
            + "select decision, reason, code "
            + "from llm "
            + "where factId = '" + PAY_1001 + "' "
            + "and relatedFactIds = '"
            + CUST_2001 + ","
            + PM_3001 + ","
            + RISK_4001 + ","
            + POL_5001 + "' "
            + "control intent = 'decide_payment_capture', "
            + "action = 'determine', "
            + "subject = 'primary_payment_request', "
            + "decision = 'allow_capture', "
            + "basis = 'related_system_facts', "
            + "goal = 'decision'";

    @Test
    void transaction_service_query_orchestrator_end_to_end() {

        Console.log("IT", "transaction_service_query_orchestrator_end_to_end");

        GraphBuilder graphBuilder = GraphBuilder.getInstance();
        graphBuilder.clear();

        graphBuilder.addNode(payFact(PAY_1001, PAY_1001_TEXT));
        graphBuilder.addNode(payFact(CUST_2001, CUST_2001_TEXT));
        graphBuilder.addNode(payFact(PM_3001, PM_3001_TEXT));
        graphBuilder.addNode(payFact(RISK_4001, RISK_4001_TEXT));
        graphBuilder.addNode(payFact(POL_5001, POL_5001_TEXT));

        String sql = ""
                + "select decision, reason, code "
                + "from llm "
                + "where factId = '" + PAY_1001 + "' "
                + "and relatedFactIds = '"
                + CUST_2001 + ","
                + PM_3001 + ","
                + RISK_4001 + ","
                + POL_5001 + "' "
                + "control intent = 'decide_payment_capture', "
                + "action = 'determine', "
                + "subject = 'primary_payment_request', "
                + "decision = 'allow_capture', "
                + "basis = 'related_system_facts', "
                + "goal = 'decision'";

        QueryClient client = new RESTClient();
        PolicyGateOrchestrator gate = new PolicyGateOrchestrator();
        TransactionService service = new TransactionService(client, gate);

        TxExecutionRequest req = new TxExecutionRequest();
        req.setDescription("it.tx.queryorch.ok");
        req.setPolicyRef("policy:it");

        int stepNo = 1;
        while (stepNo <= 10) {
            TxStepRequest step = new TxStepRequest();
            step.setId("s" + stepNo);
            step.setDescription("step" + stepNo);
            step.setSql(sql);
            req.getSteps().add(step);
            req.getCommitOrder().add("s" + stepNo);
            stepNo = stepNo + 1;
        }

        Console.log("IT", "request=" + req.getDescription());
        Console.log("IT", "sql=" + sql);

        TxExecutionResult out = service.execute(req);

        Console.log("IT", out.toJson());

        Assertions.assertNotNull(out);
        Assertions.assertNotNull(out.getGateResult());

        Assertions.assertEquals("it.tx.queryorch.ok", out.getDescription());
        Assertions.assertEquals("policy:it", out.getPolicyRef());

        List<TxStepResult> steps = out.getStepResults();

        Assertions.assertNotNull(steps);
        Assertions.assertEquals(10, steps.size());

        Assertions.assertNotNull(out.getCommitOrder());
        Assertions.assertEquals(10, out.getCommitOrder().size());

        int expectedStep = 0;
        while (expectedStep < 10) {
            String expectedId = "s" + (expectedStep + 1);
            String expectedDescription = "step" + (expectedStep + 1);
            Assertions.assertEquals(expectedId, steps.get(expectedStep).getId());
            Assertions.assertEquals(expectedDescription, steps.get(expectedStep).getDescription());
            Assertions.assertEquals(expectedId, out.getCommitOrder().get(expectedStep));
            expectedStep = expectedStep + 1;
        }

        int i = 0;
        while (i < steps.size()) {
            observeStepDecision(steps.get(i), i);
            i = i + 1;
        }
    }

    @Test
    void transaction_service_failfast_skips_later_sql_when_a_step_returns_null() {

        Console.log("IT", "transaction_service_failfast_skips_later_sql_when_a_step_returns_null");

        seedPayGraph();

        QueryClient queryClient = new RESTClient();
        PolicyGateOrchestrator gate = new PolicyGateOrchestrator();
        TransactionService service = new TransactionService(queryClient, gate);

        TxExecutionRequest req = new TxExecutionRequest();
        req.setDescription("it.tx.queryorch.failfast");
        req.setPolicyRef("policy:it");

        TxStepRequest s1 = new TxStepRequest();
        s1.setId("s1");
        s1.setDescription("step1");
        s1.setSql(PAY_SQL);

        TxStepRequest s2 = new TxStepRequest();
        s2.setId("s2");
        s2.setDescription("step2");
        s2.setSql("   ");

        TxStepRequest s3 = new TxStepRequest();
        s3.setId("s3");
        s3.setDescription("step3");
        s3.setSql(PAY_SQL);

        req.getSteps().add(s1);
        req.getSteps().add(s2);
        req.getSteps().add(s3);

        req.getCommitOrder().add("s1");
        req.getCommitOrder().add("s2");
        req.getCommitOrder().add("s3");

        TxExecutionResult out = service.execute(req);

        Console.log("IT", out.toJson());

        Assertions.assertNotNull(out);

        List<TxStepResult> steps = out.getStepResults();
        Assertions.assertNotNull(steps);
        Assertions.assertEquals(2, steps.size());
        Assertions.assertEquals("s1", steps.get(0).getId());
        Assertions.assertEquals("s2", steps.get(1).getId());

        Assertions.assertNotNull(steps.get(0).getQueryResult());
        Assertions.assertNull(steps.get(1).getQueryResult());
    }

    private void observeStepDecision(TxStepResult stepResult, int stepIndex) {
        Console.log("IT", "v1.step.index=" + String.valueOf(stepIndex));
        Console.log("IT", "v1.step.id=" + stepResult.getId());
        Console.log("IT", "v1.step.description=" + stepResult.getDescription());

        QueryResult result = stepResult.getQueryResult();
        QueryExecution<?> execution = null;
        String decision = null;

        if (result == null) {
            Console.log("IT", "v1.step.queryResult=null");
        } else {
            Console.log("IT", "v1.step.queryResult=" + result.toJson());
            Console.log("IT", "v1.step.queryResult.ok=" + String.valueOf(result.isOk()));
            if (result.getWhy() == null) {
                Console.log("IT", "v1.step.queryResult.why=null");
            } else {
                Console.log("IT", "v1.step.queryResult.why=" + result.getWhy().toString());
            }
            if (result.getQueryExecutionJson() != null) {
                execution = QueryExecution.fromJson(result.getQueryExecutionJson());
            }
        }

        if (execution == null) {
            Console.log("IT", "v1.step.execution=null");
            Console.log("IT", "v1.step.result.decision=null");
            Console.log("IT", "v1.step.result.reason=null");
            Console.log("IT", "v1.step.result.code=null");
        } else {
            Console.log("IT", "v1.step.execution.status=" + String.valueOf(execution.getStatus()));
            Console.log("IT", "v1.step.execution.stage=" + String.valueOf(execution.getStage()));
            Console.log("IT", "v1.step.execution.ok=" + String.valueOf(execution.isOk()));
            Console.log("IT", "v1.step.execution.promptValidation=" + String.valueOf(execution.getPromptValidation()));
            Console.log("IT", "v1.step.execution.llmResponseValidation=" + String.valueOf(execution.getLlmResponseValidation()));
            Console.log("IT", "v1.step.execution.domainValidation=" + String.valueOf(execution.getDomainValidation()));
            Console.log("IT", "v1.step.rawResponse=" + String.valueOf(execution.getRawResponse()));

            String rawResponse = execution.getRawResponse();
            if (rawResponse == null) {
                Console.log("IT", "v1.step.result.decision=null");
                Console.log("IT", "v1.step.result.reason=null");
                Console.log("IT", "v1.step.result.code=null");
            } else {
                try {
                    JsonElement parsed = JsonParser.parseString(rawResponse.trim());
                    if (parsed.isJsonObject()) {
                        JsonObject root = parsed.getAsJsonObject();
                        if (root.has("result") && root.get("result").isJsonObject()) {
                            JsonObject resultObject = root.getAsJsonObject("result");
                            if (resultObject.has("decision") && !resultObject.get("decision").isJsonNull()) {
                                decision = resultObject.get("decision").getAsString();
                            }
                            if (resultObject.has("reason") && !resultObject.get("reason").isJsonNull()) {
                                Console.log("IT", "v1.step.result.reason=" + resultObject.get("reason").getAsString());
                            } else {
                                Console.log("IT", "v1.step.result.reason=missing");
                            }
                            if (resultObject.has("code") && !resultObject.get("code").isJsonNull()) {
                                Console.log("IT", "v1.step.result.code=" + resultObject.get("code").getAsString());
                            } else {
                                Console.log("IT", "v1.step.result.code=missing");
                            }
                        } else {
                            Console.log("IT", "v1.step.result=missing");
                        }
                    } else {
                        Console.log("IT", "v1.step.result=rawResponse not object");
                    }
                } catch (RuntimeException e) {
                    Console.log("IT", "v1.step.result.parse=" + e.getClass().getName() + ": " + e.getMessage());
                }
                Console.log("IT", "v1.step.result.decision=" + String.valueOf(decision));
            }
        }

        Assertions.assertEquals("allow_capture", decision);
    }

    private void seedPayGraph() {
        GraphBuilder graphBuilder = GraphBuilder.getInstance();
        graphBuilder.clear();

        graphBuilder.addNode(payFact(PAY_1001, PAY_1001_TEXT));
        graphBuilder.addNode(payFact(CUST_2001, CUST_2001_TEXT));
        graphBuilder.addNode(payFact(PM_3001, PM_3001_TEXT));
        graphBuilder.addNode(payFact(RISK_4001, RISK_4001_TEXT));
        graphBuilder.addNode(payFact(POL_5001, POL_5001_TEXT));
    }

    private Fact payFact(String id, String text) {
        return new Fact(id, text, new HashSet<String>(), "atomic");
    }

    private List<String> compareDeterministicPaySpecimen(JsonObject requestJson, QueryExecution<?> execution) {
        List<String> diffs = new ArrayList<String>();

        JsonObject meta = requestJson.getAsJsonObject("meta");
        if (meta == null) {
            diffs.add("Meta missing on requestJson");
        } else {
            assertString(diffs, "Meta.version", "v1", readString(meta, "version"));
            assertString(diffs, "Meta.queryKind", "decision", readString(meta, "queryKind"));
        }

        assertString(diffs, "QueryRequest.factId", PAY_1001, readString(requestJson, "factId"));

        JsonObject task = requestJson.getAsJsonObject("task");
        if (task == null) {
            diffs.add("ValidateTask missing on requestJson");
        } else {
            JsonObject intent = task.getAsJsonObject("intent");
            if (intent == null) {
                diffs.add("ValidateTask.intent missing");
            } else {
                assertString(diffs, "ValidateTask.intent.goal", "decision", readString(intent, "goal"));
            }
            assertString(diffs, "ValidateTask.factId", PAY_1001, readString(task, "factId"));
            assertStringArray(
                    diffs,
                    "ValidateTask.relatedFactIds",
                    Arrays.asList(CUST_2001, PM_3001, RISK_4001, POL_5001),
                    readStringArray(task, "relatedFactIds")
            );
            assertStringArray(
                    diffs,
                    "ValidateTask.select",
                    Arrays.asList("decision", "reason", "code"),
                    readStringArray(task, "select")
            );
            JsonObject controls = task.getAsJsonObject("controls");
            if (controls == null) {
                diffs.add("ValidateTask.controls missing");
            } else {
                assertEqualsSize(diffs, "ValidateTask.controls.size", 6, controls.size());
                assertString(diffs, "ValidateTask.controls.intent", "decide_payment_capture", readString(controls, "intent"));
                assertString(diffs, "ValidateTask.controls.action", "determine", readString(controls, "action"));
                assertString(diffs, "ValidateTask.controls.subject", "primary_payment_request", readString(controls, "subject"));
                assertString(diffs, "ValidateTask.controls.decision", "allow_capture", readString(controls, "decision"));
                assertString(diffs, "ValidateTask.controls.basis", "related_system_facts", readString(controls, "basis"));
                assertString(diffs, "ValidateTask.controls.goal", "decision", readString(controls, "goal"));
            }
        }

        JsonObject context = requestJson.getAsJsonObject("context");
        if (context == null || !context.has("nodes") || !context.get("nodes").isJsonObject()) {
            diffs.add("GraphContext.nodes missing");
        } else {
            JsonObject nodes = context.getAsJsonObject("nodes");
            assertEqualsSize(diffs, "GraphContext.nodes.size", 5, nodes.size());
            assertPayNode(diffs, nodes, PAY_1001, PAY_1001_TEXT);
            assertPayNode(diffs, nodes, CUST_2001, CUST_2001_TEXT);
            assertPayNode(diffs, nodes, PM_3001, PM_3001_TEXT);
            assertPayNode(diffs, nodes, RISK_4001, RISK_4001_TEXT);
            assertPayNode(diffs, nodes, POL_5001, POL_5001_TEXT);
        }

        JsonObject llmQuery = null;
        if (execution.getLlmResponse() != null
                && execution.getLlmResponse().getLlmRequest() != null) {
            llmQuery = execution.getLlmResponse().getLlmRequest().getLlmQuery();
        }
        if (llmQuery == null && execution.toJson() != null) {
            JsonObject executionJson = execution.toJson();
            if (executionJson.has("llmResponse")
                    && executionJson.get("llmResponse").isJsonObject()) {
                JsonObject llmResponseJson = executionJson.getAsJsonObject("llmResponse");
                if (llmResponseJson.has("llmRequest")
                        && llmResponseJson.get("llmRequest").isJsonObject()) {
                    JsonObject llmRequestJson = llmResponseJson.getAsJsonObject("llmRequest");
                    if (llmRequestJson.has("llmQuery")
                            && llmRequestJson.get("llmQuery").isJsonObject()) {
                        llmQuery = llmRequestJson.getAsJsonObject("llmQuery");
                    }
                }
            }
        }
        if (llmQuery == null) {
            diffs.add("llmQuery missing on QueryExecution.llmResponse.llmRequest");
            return diffs;
        }

        JsonObject outputTemplate = llmQuery.getAsJsonObject("output_template");
        if (outputTemplate == null || !outputTemplate.has("result") || !outputTemplate.get("result").isJsonObject()) {
            diffs.add("output_template.result missing");
        } else {
            JsonObject outputResult = outputTemplate.getAsJsonObject("result");
            assertEqualsSize(diffs, "output_template.result.size", 3, outputResult.size());
            assertString(diffs, "output_template.result.decision", "", readString(outputResult, "decision"));
            assertString(diffs, "output_template.result.reason", "", readString(outputResult, "reason"));
            assertString(diffs, "output_template.result.code", "", readString(outputResult, "code"));
        }

        JsonObject llmInstructions = llmQuery.getAsJsonObject("llm_instructions");
        if (llmInstructions == null || !llmInstructions.get("instructions").isJsonArray()) {
            diffs.add("llm_instructions.instructions missing");
        } else {
            List<String> expected = expectedV1PayInstructions();
            List<String> actual = readStringArray(llmInstructions, "instructions");
            assertStringArray(diffs, "v1 llm_instructions", expected, actual);
        }

        return diffs;
    }

    private List<String> expectedV1PayInstructions() {
        return Arrays.asList(
                "You are an execution engine, not a document reader.",
                "Return only JSON.",
                "Use the provided output_template as the final answer format.",
                "Execute this prompt using only the provided context and task.",
                "Do not describe, summarize, explain, or analyze this request.",
                "Use context.nodes as the system state.",
                "Use task.factId as the primary fact.",
                "Use each task.relatedFactId as a system fact related directly to the primary fact identified by task.factId.",
                "Do not infer relationships between related facts unless explicitly provided by context.",
                "Use task.controls only to understand the intent of the task.",
                "Do not treat task.controls as additional facts.",
                "Do not infer missing facts from task.controls.",
                "Do not recompute task.controls from context.",
                "Return compact JSON on a single line.",
                "Do not include spaces, tabs, or newlines outside JSON syntax.",
                "Return exactly the output_template shape.",
                "Do not add, remove, or rename any fields.",
                "Return exactly one JSON object.",
                "Do not wrap the JSON in markdown fences.",
                "Do not include explanation before or after the JSON.",
                "Set result.decision as a string value derived from llm_query execution.",
                "Set result.reason as a string value derived from llm_query execution.",
                "Set result.code as a string value derived from llm_query execution."
        );
    }

    private void assertPayNode(List<String> diffs, JsonObject nodes, String id, String text) {
        if (!nodes.has(id) || !nodes.get(id).isJsonObject()) {
            diffs.add("GraphContext missing node " + id);
            return;
        }
        JsonObject node = nodes.getAsJsonObject(id);
        assertString(diffs, "GraphContext." + id + ".id", id, readString(node, "id"));
        assertString(diffs, "GraphContext." + id + ".text", text, readString(node, "text"));
        assertString(diffs, "GraphContext." + id + ".mode", "ATOMIC", readString(node, "mode"));
        if (!node.has("attributes") || !node.get("attributes").isJsonArray()) {
            diffs.add("GraphContext." + id + ".attributes missing");
            return;
        }
        assertEqualsSize(diffs, "GraphContext." + id + ".attributes.size", 0, node.getAsJsonArray("attributes").size());
    }

    private void assertString(List<String> diffs, String name, String expected, String actual) {
        if (expected == null && actual == null) {
            return;
        }
        if (expected == null || !expected.equals(actual)) {
            diffs.add(name + " expected=[" + expected + "] actual=[" + actual + "]");
        }
    }

    private void assertStringArray(List<String> diffs, String name, List<String> expected, List<String> actual) {
        if (expected.equals(actual)) {
            return;
        }
        diffs.add(name + " expected=" + expected + " actual=" + actual);
    }

    private void assertEqualsSize(List<String> diffs, String name, int expected, int actual) {
        if (expected != actual) {
            diffs.add(name + " expected=" + expected + " actual=" + actual);
        }
    }

    private String readString(JsonObject json, String key) {
        if (json == null || !json.has(key) || json.get(key).isJsonNull()) {
            return null;
        }
        return json.get(key).getAsString();
    }

    private List<String> readStringArray(JsonObject json, String key) {
        List<String> values = new ArrayList<String>();
        if (json == null || !json.has(key) || !json.get(key).isJsonArray()) {
            return values;
        }
        JsonArray array = json.getAsJsonArray(key);
        int i = 0;
        while (i < array.size()) {
            if (!array.get(i).isJsonNull()) {
                values.add(array.get(i).getAsString());
            }
            i = i + 1;
        }
        return values;
    }
}
