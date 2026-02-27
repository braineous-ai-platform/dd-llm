package io.braineous.dd.llm.transaction.model;

import ai.braineous.rag.prompt.observe.Console;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TxExecutionRequestTest {

    @Test
    public void txExecutionRequest_shouldInitializeWithEmptyLists() {

        TxExecutionRequest r = new TxExecutionRequest();

        Console.log("ut.txexecreq.init.json", r.toJson());

        assertNull(r.getId());
        assertNull(r.getDescription());
        assertNull(r.getPolicyRef());

        assertNotNull(r.getSteps());
        assertTrue(r.getSteps().isEmpty());

        assertNotNull(r.getCommitOrder());
        assertTrue(r.getCommitOrder().isEmpty());
    }

    @Test
    public void txExecutionRequest_shouldSetAndGetFieldsCorrectly() {

        TxExecutionRequest r = new TxExecutionRequest();

        r.setId("tx-1");
        r.setDescription("Rebook disrupted passengers");
        r.setPolicyRef("policy:pnr_rebook_v1");

        List<String> order = new ArrayList<String>();
        order.add("s1");
        order.add("s2");
        r.setCommitOrder(order);

        List<TxStepRequest> steps = new ArrayList<TxStepRequest>();

        TxStepRequest s1 = new TxStepRequest();
        s1.setId("s1");
        s1.setDescription("Fetch options");
        s1.setSql("select decision from llm where task = 'fetch options'");

        steps.add(s1);

        r.setSteps(steps);

        Console.log("ut.txexecreq.setters.json", r.toJson());

        assertEquals("tx-1", r.getId());
        assertEquals("Rebook disrupted passengers", r.getDescription());
        assertEquals("policy:pnr_rebook_v1", r.getPolicyRef());

        assertNotNull(r.getCommitOrder());
        assertEquals(2, r.getCommitOrder().size());
        assertEquals("s1", r.getCommitOrder().get(0));
        assertEquals("s2", r.getCommitOrder().get(1));

        assertNotNull(r.getSteps());
        assertEquals(1, r.getSteps().size());
        assertEquals("s1", r.getSteps().get(0).getId());
        assertEquals("Fetch options", r.getSteps().get(0).getDescription());
        assertEquals("select decision from llm where task = 'fetch options'", r.getSteps().get(0).getSql());
    }

    @Test
    public void txExecutionRequest_shouldSerializeToJson_withListsPresent() {

        TxExecutionRequest r = new TxExecutionRequest();
        r.setId("tx-2");
        r.setDescription("Tx exec request test");
        r.setPolicyRef("policy:test");

        TxStepRequest s1 = new TxStepRequest();
        s1.setId("s1");
        s1.setDescription("Step 1");
        s1.setSql("select decision from llm where task = 'step1'");

        r.getSteps().add(s1);
        r.getCommitOrder().add("s1");

        String json = r.toJson();
        Console.log("ut.txexecreq.json.raw", json);

        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

        assertEquals("tx-2", obj.get("id").getAsString());
        assertEquals("Tx exec request test", obj.get("description").getAsString());
        assertEquals("policy:test", obj.get("policyRef").getAsString());

        assertTrue(obj.has("steps"));
        assertFalse(obj.get("steps").isJsonNull());
        assertTrue(obj.get("steps").isJsonArray());

        JsonArray steps = obj.getAsJsonArray("steps");
        assertEquals(1, steps.size());
        assertTrue(steps.get(0).isJsonObject());

        JsonObject step0 = steps.get(0).getAsJsonObject();
        assertEquals("s1", step0.get("id").getAsString());
        assertEquals("Step 1", step0.get("description").getAsString());
        assertEquals("select decision from llm where task = 'step1'", step0.get("sql").getAsString());

        assertTrue(obj.has("commitOrder"));
        assertFalse(obj.get("commitOrder").isJsonNull());
        assertTrue(obj.get("commitOrder").isJsonArray());

        JsonArray commitOrder = obj.getAsJsonArray("commitOrder");
        assertEquals(1, commitOrder.size());
        assertEquals("s1", commitOrder.get(0).getAsString());
    }
}
