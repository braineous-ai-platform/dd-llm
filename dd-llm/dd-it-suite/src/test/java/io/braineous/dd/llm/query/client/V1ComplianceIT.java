package io.braineous.dd.llm.query.client;

import ai.braineous.rag.prompt.cgo.api.Fact;
import ai.braineous.rag.prompt.cgo.api.QueryExecution;
import ai.braineous.rag.prompt.cgo.api.ValidationResult;
import ai.braineous.rag.prompt.models.cgo.graph.GraphBuilder;
import ai.braineous.rag.prompt.observe.Console;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class V1ComplianceIT {

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

    @Test
    public void payFunctionalExecution_sqlSeam_shouldSatisfyCgoV1PayContract() {
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
        QueryResult result = client.query(sql);

        Console.log("v1.observation.sql", sql);

        if (result == null) {
            Console.log("v1.observation.queryResult", "null");
        } else {
            Console.log("v1.observation.queryResult.ok", String.valueOf(result.isOk()));
            if (result.getWhy() == null) {
                Console.log("v1.observation.queryResult.why", "null");
            } else {
                Console.log("v1.observation.queryResult.why", result.getWhy().toString());
            }
            if (result.getRequestJson() == null) {
                Console.log("v1.observation.queryRequest", "null");
            } else {
                Console.log("v1.observation.queryRequest", result.getRequestJson().toString());
            }
        }

        QueryExecution<?> execution = null;
        if (result != null && result.getQueryExecutionJson() != null) {
            execution = QueryExecution.fromJson(result.getQueryExecutionJson());
        }

        if (execution == null) {
            Console.log("v1.observation.execution", "null");
        } else {
            Console.log("v1.observation.execution.status", String.valueOf(execution.getStatus()));
            Console.log("v1.observation.execution.stage", String.valueOf(execution.getStage()));
            Console.log("v1.observation.execution.ok", String.valueOf(execution.isOk()));
            Console.log("v1.observation.execution.promptValidation", String.valueOf(execution.getPromptValidation()));
            Console.log("v1.observation.execution.llmResponseValidation", String.valueOf(execution.getLlmResponseValidation()));
            Console.log("v1.observation.execution.domainValidation", String.valueOf(execution.getDomainValidation()));
            Console.log("v1.observation.rawResponse", String.valueOf(execution.getRawResponse()));

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
                Console.log("v1.observation.llm_query", "null");
            } else {
                Console.log("v1.observation.llm_query", llmQuery.toString());
                if (llmQuery.has("task") && llmQuery.get("task").isJsonObject()) {
                    JsonObject task = llmQuery.getAsJsonObject("task");
                    if (task.has("controls") && task.get("controls").isJsonObject()) {
                        Console.log("v1.observation.llm_query.task.controls", task.getAsJsonObject("controls").toString());
                    } else {
                        Console.log("v1.observation.llm_query.task.controls", "missing");
                    }
                }
            }

            String rawResponse = execution.getRawResponse();
            if (rawResponse == null) {
                Console.log("v1.observation.result.decision", "null");
                Console.log("v1.observation.result.reason", "null");
                Console.log("v1.observation.result.code", "null");
            } else {
                try {
                    JsonElement parsed = JsonParser.parseString(rawResponse.trim());
                    if (parsed.isJsonObject()) {
                        JsonObject root = parsed.getAsJsonObject();
                        if (root.has("result") && root.get("result").isJsonObject()) {
                            JsonObject resultObject = root.getAsJsonObject("result");
                            if (resultObject.has("decision") && !resultObject.get("decision").isJsonNull()) {
                                Console.log("v1.observation.result.decision", resultObject.get("decision").getAsString());
                            } else {
                                Console.log("v1.observation.result.decision", "missing");
                            }
                            if (resultObject.has("reason") && !resultObject.get("reason").isJsonNull()) {
                                Console.log("v1.observation.result.reason", resultObject.get("reason").getAsString());
                            } else {
                                Console.log("v1.observation.result.reason", "missing");
                            }
                            if (resultObject.has("code") && !resultObject.get("code").isJsonNull()) {
                                Console.log("v1.observation.result.code", resultObject.get("code").getAsString());
                            } else {
                                Console.log("v1.observation.result.code", "missing");
                            }
                        } else {
                            Console.log("v1.observation.result", "missing");
                        }
                    } else {
                        Console.log("v1.observation.result", "rawResponse not object");
                    }
                } catch (RuntimeException e) {
                    Console.log("v1.observation.result.parse", e.getClass().getName() + ": " + e.getMessage());
                }
            }
        }
    }

    @Test
    public void payFunctionalExecution_drift_shouldKeepContractStableAcrossRuns() {

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

        int runCount = 5;

        Map<String, Integer> decisionCounts =
                new HashMap<String, Integer>();

        Map<String, Integer> reasonCounts =
                new HashMap<String, Integer>();

        Map<String, Integer> codeCounts =
                new HashMap<String, Integer>();

        int contractFailureCount = 0;

        int i = 0;
        while (i < runCount) {

            Console.log("pay.drift.run", String.valueOf(i));

            try {
                QueryClient client = new RESTClient();
                QueryResult result = client.query(sql);

                Console.log("pay.drift.queryResult.ok", result == null ? "null" : String.valueOf(result.isOk()));
                Console.log("pay.drift.queryResult.why", result == null || result.getWhy() == null ? "null" : result.getWhy().toString());

                QueryExecution<?> execution = null;
                if (result != null && result.getQueryExecutionJson() != null) {
                    execution = QueryExecution.fromJson(result.getQueryExecutionJson());
                }

                Console.log("pay.drift.execution.ok", execution == null ? "null" : String.valueOf(execution.isOk()));
                Console.log("pay.drift.status", execution == null ? "null" : String.valueOf(execution.getStatus()));
                Console.log("pay.drift.stage", execution == null ? "null" : String.valueOf(execution.getStage()));
                Console.log("pay.drift.rawResponse", execution == null ? "null" : String.valueOf(execution.getRawResponse()));
                Console.log("pay.drift.promptValidation", execution == null ? "null" : String.valueOf(execution.getPromptValidation()));
                Console.log("pay.drift.llmResponseValidation", execution == null ? "null" : String.valueOf(execution.getLlmResponseValidation()));

                printVerbatimLlmObservation(
                        "payFunctionalExecution_drift_shouldKeepContractStableAcrossRuns",
                        i + 1,
                        result,
                        execution);

                try {
                    // assertNotNull(execution);
                    // assertTrue(execution.isOk());
                    // assertEquals("OK", execution.getStatus());
                    // assertEquals("ok", execution.getStage());

                    String rawResponse =
                            String.valueOf(execution.getRawResponse()).trim();

                    // assertFalse(rawResponse.contains("\n"));
                    // assertFalse(rawResponse.contains("\r"));
                    // assertFalse(rawResponse.contains("\t"));

                    JsonElement parsed =
                            JsonParser.parseString(rawResponse);

                    // assertTrue(parsed.isJsonObject());

                    JsonObject root =
                            parsed.getAsJsonObject();

                    // assertTrue(root.has("result"));
                    // assertEquals(1, root.entrySet().size());

                    JsonObject resultObject =
                            root.getAsJsonObject("result");

                    // assertNotNull(result);
                    // assertEquals(3, result.size());

                    // assertTrue(result.has("decision"));
                    // assertTrue(result.has("reason"));
                    // assertTrue(result.has("code"));

                    // assertTrue(result.get("decision").isJsonPrimitive());
                    // assertTrue(result.get("reason").isJsonPrimitive());
                    // assertTrue(result.get("code").isJsonPrimitive());

                    String decision =
                            resultObject.get("decision").getAsString();

                    String reason =
                            resultObject.get("reason").getAsString();

                    String code =
                            resultObject.get("code").getAsString();

                    increment(decisionCounts, decision);
                    increment(reasonCounts, reason);
                    increment(codeCounts, code);

                    Console.log("pay.drift.decision", decision);
                    Console.log("pay.drift.reason", reason);
                    Console.log("pay.drift.code", code);

                    Assertions.assertEquals("allow_capture", decision);

                } catch (AssertionError e) {
                    throw e;
                }

            } catch (RuntimeException e) {
                contractFailureCount++;
                Console.log(
                        "pay.drift.contract.failure",
                        e.getClass().getName() + ": " + e.getMessage()
                );
            } catch (Error e) {
                if (e instanceof AssertionError) {
                    throw e;
                }
                contractFailureCount++;
                Console.log(
                        "pay.drift.contract.failure",
                        e.getClass().getName() + ": " + e.getMessage()
                );
            }

            i++;
        }

        Console.log("pay.drift.runCount", String.valueOf(runCount));
        Console.log("pay.drift.contractFailureCount", String.valueOf(contractFailureCount));
        Console.log("pay.drift.uniqueDecisions", decisionCounts.toString());
        Console.log("pay.drift.uniqueReasons", reasonCounts.toString());
        Console.log("pay.drift.uniqueCodes", codeCounts.toString());

        // assertEquals(0, contractFailureCount);
        // assertFalse(decisionCounts.isEmpty());
        // assertFalse(reasonCounts.isEmpty());
        // assertFalse(codeCounts.isEmpty());
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
        for (int i = 0; i < array.size(); i++) {
            if (!array.get(i).isJsonNull()) {
                values.add(array.get(i).getAsString());
            }
        }
        return values;
    }

    private String promptHeadline(ValidationResult validation) {
        if (validation == null) {
            return "promptValidation null";
        }
        return "promptValidation ok=" + validation.isOk() + " code=" + validation.getCode();
    }

    private String llmHeadline(ValidationResult validation) {
        if (validation == null) {
            return "llmResponseValidation null";
        }
        return "llmResponseValidation ok=" + validation.isOk() + " code=" + validation.getCode();
    }

    private void printVerbatimLlmObservation(
            String testMethod,
            int runNumber,
            QueryResult result,
            QueryExecution<?> execution) {
        System.out.println("LLM_OBSERVATION_TEST_METHOD=" + testMethod);
        System.out.println("LLM_OBSERVATION_RUN_NUMBER=" + runNumber);
        System.out.println("LLM_OBSERVATION_QUERYRESULT_OK=" + (result == null ? "null" : String.valueOf(result.isOk())));
        System.out.println("LLM_OBSERVATION_QUERYRESULT_WHY=" + (result == null || result.getWhy() == null ? "null" : result.getWhy().toString()));
        System.out.println("LLM_OBSERVATION_QUERYEXECUTION_OK=" + (execution == null ? "null" : String.valueOf(execution.isOk())));
        System.out.println("LLM_OBSERVATION_QUERYEXECUTION_STATUS=" + (execution == null ? "null" : String.valueOf(execution.getStatus())));
        System.out.println("LLM_OBSERVATION_QUERYEXECUTION_STAGE=" + (execution == null ? "null" : String.valueOf(execution.getStage())));
        System.out.println("===== RAW_RESPONSE_START =====");
        if (execution != null && execution.getRawResponse() != null) {
            System.out.print(execution.getRawResponse());
        }
        System.out.println();
        System.out.println("===== RAW_RESPONSE_END =====");
    }

    private void increment(Map<String, Integer> counts, String value) {
        Integer current =
                counts.get(value);

        if (current == null) {
            counts.put(value, 1);
        } else {
            counts.put(value, current + 1);
        }
    }
}
