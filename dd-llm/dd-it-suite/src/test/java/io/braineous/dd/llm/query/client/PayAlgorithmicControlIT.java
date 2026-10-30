package io.braineous.dd.llm.query.client;

import ai.braineous.rag.prompt.cgo.api.Fact;
import ai.braineous.rag.prompt.cgo.api.QueryExecution;
import ai.braineous.rag.prompt.models.cgo.graph.GraphBuilder;
import ai.braineous.rag.prompt.observe.Console;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

@QuarkusTest
public class PayAlgorithmicControlIT {

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
    public void test_1() {
        GraphBuilder graphBuilder = GraphBuilder.getInstance();
        graphBuilder.clear();

        graphBuilder.addNode(payFact(PAY_1001, PAY_1001_TEXT));
        graphBuilder.addNode(payFact(CUST_2001, CUST_2001_TEXT));
        graphBuilder.addNode(payFact(PM_3001, PM_3001_TEXT));
        graphBuilder.addNode(payFact(RISK_4001, RISK_4001_TEXT));
        graphBuilder.addNode(payFact(POL_5001, POL_5001_TEXT));

        Console.log("v1.observation.fact.PAY-1001", PAY_1001_TEXT);
        Console.log("v1.observation.fact.CUST-2001", CUST_2001_TEXT);
        Console.log("v1.observation.fact.PM-3001", PM_3001_TEXT);
        Console.log("v1.observation.fact.RISK-4001", RISK_4001_TEXT);
        Console.log("v1.observation.fact.POL-5001", POL_5001_TEXT);

        String sql = ""
                + "select decision, reason, code "
                + "from llm "
                + "where factId = '" + PAY_1001 + "' "
                + "and relatedFactIds = '"
                + CUST_2001 + ","
                + PM_3001 + ","
                + RISK_4001 + ","
                + POL_5001 + "' "
                + "control "
                + "method = 'decide_payment_capture', "
                + "parameters = 'CustomerAccount.status, PaymentMethod.type, RiskProfile.level, MerchantPolicy.capture', "
                + "expression = 'CustomerAccount.status is equal to ACTIVE and PaymentMethod.type is equal to CARD and RiskProfile.level is equal to LOW and MerchantPolicy.capture is equal to AUTO', "
                + "spec = 'When the expression is true, the result is allow_capture. Otherwise, the result is deny_capture.', "
                + "result = ''";

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

    private Fact payFact(String id, String text) {
        return new Fact(id, text, new HashSet<String>(), "atomic");
    }
}
