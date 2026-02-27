package io.braineous.dd.llm.transaction.model;

import ai.braineous.rag.prompt.observe.Console;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.braineous.dd.llm.core.model.Why;
import io.braineous.dd.llm.query.client.QueryResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TxGateRequestTest {

    @Test
    public void txGateRequest_shouldInitializeWithEmptyLists() {

        TxGateRequest r = new TxGateRequest();

        Console.log("ut.txgate.init.json", r.toJson());

        assertNull(r.getId());
        assertNull(r.getDescription());
        assertNull(r.getPolicyRef());

        assertNotNull(r.getStepResults());
        assertTrue(r.getStepResults().isEmpty());

        assertNotNull(r.getCommitOrder());
        assertTrue(r.getCommitOrder().isEmpty());
    }

    @Test
    public void txGateRequest_shouldSetAndGetFieldsCorrectly() {

        TxGateRequest r = new TxGateRequest();

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

        Console.log("ut.txgate.setters.json", r.toJson());

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
    }

    @Test
    public void txGateRequest_shouldSerializeToJson_withListsPresent() {

        TxGateRequest r = new TxGateRequest();
        r.setId("tx-2");
        r.setDescription("Tx gate test");
        r.setPolicyRef("policy:test");

        TxStepResult s1 = new TxStepResult();
        s1.setId("s1");
        s1.setDescription("Step 1");

        QueryResult qr = new QueryResult();
        qr.setOk(true);
        s1.setQueryResult(qr);

        r.getStepResults().add(s1);
        r.getCommitOrder().add("s1");

        String json = r.toJson();
        Console.log("ut.txgate.json.raw", json);

        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

        assertEquals("tx-2", obj.get("id").getAsString());
        assertEquals("Tx gate test", obj.get("description").getAsString());
        assertEquals("policy:test", obj.get("policyRef").getAsString());

        assertTrue(obj.has("stepResults"));
        assertFalse(obj.get("stepResults").isJsonNull());
        assertTrue(obj.get("stepResults").isJsonArray());

        JsonArray stepResults = obj.getAsJsonArray("stepResults");
        assertEquals(1, stepResults.size());
        assertTrue(stepResults.get(0).isJsonObject());

        assertTrue(obj.has("commitOrder"));
        assertFalse(obj.get("commitOrder").isJsonNull());
        assertTrue(obj.get("commitOrder").isJsonArray());

        JsonArray commitOrder = obj.getAsJsonArray("commitOrder");
        assertEquals(1, commitOrder.size());
        assertEquals("s1", commitOrder.get(0).getAsString());
    }
}