package io.braineous.dd.llm.transaction.model;

import ai.braineous.rag.prompt.observe.Console;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.braineous.dd.llm.core.model.Why;
import io.braineous.dd.llm.pg.model.PolicyGateResult;
import io.braineous.dd.llm.query.client.QueryResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TxExecutionResultTest {

    @Test
    public void txExecutionResult_shouldInitializeWithEmptyLists_andNullGateResult() {

        TxExecutionResult r = new TxExecutionResult();

        Console.log("ut.txexecres.init.json", r.toJson());

        assertNull(r.getId());
        assertNull(r.getDescription());
        assertNull(r.getPolicyRef());
        assertNull(r.getGateResult());

        assertNotNull(r.getStepResults());
        assertTrue(r.getStepResults().isEmpty());

        assertNotNull(r.getCommitOrder());
        assertTrue(r.getCommitOrder().isEmpty());

        assertFalse(r.isApproved());
    }

    @Test
    public void txExecutionResult_shouldSetAndGetFieldsCorrectly() {

        TxExecutionResult r = new TxExecutionResult();

        r.setId("tx-1");
        r.setDescription("Rebook disrupted passengers");
        r.setPolicyRef("policy:pnr_rebook_v1");

        List<String> order = new ArrayList<String>();
        order.add("s1");
        order.add("s2");
        r.setCommitOrder(order);

        List<TxStepResult> results = new ArrayList<TxStepResult>();

        TxStepResult s1 = new TxStepResult();
        s1.setId("s1");
        s1.setDescription("Fetch options");
        s1.setQueryResult(QueryResult.fail(new Why("Q_FAIL", "fetch failed")));

        results.add(s1);

        r.setStepResults(results);

        PolicyGateResult gate = PolicyGateResult.fail("commit-1", "DENIED");
        r.setGateResult(gate);

        Console.log("ut.txexecres.setters.json", r.toJson());

        assertEquals("tx-1", r.getId());
        assertEquals("Rebook disrupted passengers", r.getDescription());
        assertEquals("policy:pnr_rebook_v1", r.getPolicyRef());

        assertNotNull(r.getCommitOrder());
        assertEquals(2, r.getCommitOrder().size());
        assertEquals("s1", r.getCommitOrder().get(0));
        assertEquals("s2", r.getCommitOrder().get(1));

        assertNotNull(r.getStepResults());
        assertEquals(1, r.getStepResults().size());
        assertEquals("s1", r.getStepResults().get(0).getId());
        assertFalse(r.getStepResults().get(0).isOk());

        assertNotNull(r.getGateResult());
        assertFalse(r.isApproved());
    }

    @Test
    public void txExecutionResult_isApprovedShouldReturnFalse_whenGateResultNull() {

        TxExecutionResult r = new TxExecutionResult();
        r.setId("tx-null-gate");

        Console.log("ut.txexecres.isapproved.null_gate.json", r.toJson());

        assertNull(r.getGateResult());
        assertFalse(r.isApproved());
    }

    @Test
    public void txExecutionResult_isApprovedShouldReturnTrue_whenGateOkTrue() {

        TxExecutionResult r = new TxExecutionResult();
        r.setId("tx-approved");

        PolicyGateResult gate = new PolicyGateResult();
        gate.setOk(true);

        r.setGateResult(gate);

        Console.log("ut.txexecres.isapproved.true.json", r.toJson());

        assertTrue(r.isApproved());
    }

    @Test
    public void txExecutionResult_isApprovedShouldReturnFalse_whenGateOkFalse() {

        TxExecutionResult r = new TxExecutionResult();
        r.setId("tx-denied");

        PolicyGateResult gate = new PolicyGateResult();
        gate.setOk(false);

        r.setGateResult(gate);

        Console.log("ut.txexecres.isapproved.false.json", r.toJson());

        assertFalse(r.isApproved());
    }

    @Test
    public void txExecutionResult_shouldSerializeToJson_withGateAndListsPresent() {

        TxExecutionResult r = new TxExecutionResult();
        r.setId("tx-2");
        r.setDescription("Tx exec result test");
        r.setPolicyRef("policy:test");

        TxStepResult s1 = new TxStepResult();
        s1.setId("s1");
        s1.setDescription("Step 1");

        QueryResult qr = new QueryResult();
        qr.setOk(true);
        s1.setQueryResult(qr);

        r.getStepResults().add(s1);
        r.getCommitOrder().add("s1");

        PolicyGateResult gate = PolicyGateResult.ok("commit-2", "APPROVED");
        r.setGateResult(gate);

        String json = r.toJson();
        Console.log("ut.txexecres.json.raw", json);

        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

        assertEquals("tx-2", obj.get("id").getAsString());
        assertEquals("Tx exec result test", obj.get("description").getAsString());
        assertEquals("policy:test", obj.get("policyRef").getAsString());

        assertTrue(obj.has("stepResults"));
        assertFalse(obj.get("stepResults").isJsonNull());
        assertTrue(obj.get("stepResults").isJsonArray());

        JsonArray stepResults = obj.getAsJsonArray("stepResults");
        assertEquals(1, stepResults.size());

        assertTrue(obj.has("commitOrder"));
        assertFalse(obj.get("commitOrder").isJsonNull());
        assertTrue(obj.get("commitOrder").isJsonArray());

        JsonArray commitOrder = obj.getAsJsonArray("commitOrder");
        assertEquals(1, commitOrder.size());
        assertEquals("s1", commitOrder.get(0).getAsString());

        assertTrue(obj.has("gateResult"));
        assertFalse(obj.get("gateResult").isJsonNull());
        assertTrue(obj.get("gateResult").isJsonObject());

        JsonObject gateJson = obj.getAsJsonObject("gateResult");
        assertTrue(gateJson.has("ok"));
    }
}
