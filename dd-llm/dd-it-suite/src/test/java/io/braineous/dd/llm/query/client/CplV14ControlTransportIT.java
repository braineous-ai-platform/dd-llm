package io.braineous.dd.llm.query.client;

import ai.braineous.rag.prompt.cgo.api.Fact;
import ai.braineous.rag.prompt.models.cgo.graph.GraphBuilder;
import ai.braineous.rag.prompt.observe.Console;
import com.google.gson.JsonObject;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

@QuarkusTest
public class CplV14ControlTransportIT {

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

        String sql = ""
                + "select result "
                + "from llm "
                + "where factId = '" + PAY_1001 + "' "
                + "and relatedFactIds = '"
                + CUST_2001 + ","
                + PM_3001 + ","
                + RISK_4001 + ","
                + POL_5001 + "' "
                + "control method = 'decide_payment_capture', "
                + "parameters = 'amount, currency, merchant', "
                + "expression = 'amount > 0 AND currency = USD', "
                + "spec = 'if expression is true then allow, else deny', "
                + "result = 'decision'";

        QueryClient client = new RESTClient();
        QueryResult result = client.query(sql);

        Console.log("c1.transport.sql", sql);
        if (result == null) {
            Console.log("c1.transport.queryResult", "null");
        } else if (result.getRequestJson() == null) {
            Console.log("c1.transport.queryRequest", "null");
        } else {
            Console.log("c1.transport.queryRequest", result.getRequestJson().toString());
        }

        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getRequestJson());

        JsonObject task = result.getRequestJson().getAsJsonObject("task");
        Assertions.assertNotNull(task);

        JsonObject controls = task.getAsJsonObject("controls");
        Assertions.assertNotNull(controls);

        Assertions.assertEquals(5, controls.size());
        Assertions.assertEquals("decide_payment_capture", controls.get("method").getAsString());
        Assertions.assertEquals("amount, currency, merchant", controls.get("parameters").getAsString());
        Assertions.assertEquals("amount > 0 AND currency = USD", controls.get("expression").getAsString());
        Assertions.assertEquals("if expression is true then allow, else deny", controls.get("spec").getAsString());
        Assertions.assertEquals("decision", controls.get("result").getAsString());
        Assertions.assertFalse(controls.has("currency"));
        Assertions.assertFalse(controls.has("merchant"));
        Assertions.assertFalse(controls.has("else deny"));
    }

    private Fact payFact(String id, String text) {
        return new Fact(id, text, new HashSet<String>(), "atomic");
    }
}
