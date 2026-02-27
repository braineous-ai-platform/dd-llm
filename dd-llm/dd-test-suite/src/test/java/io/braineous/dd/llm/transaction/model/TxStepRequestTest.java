package io.braineous.dd.llm.transaction.model;

import ai.braineous.rag.prompt.observe.Console;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TxStepRequestTest {

    @Test
    public void txStepRequest_shouldInitializeWithNullFields() {

        TxStepRequest step = new TxStepRequest();

        Console.log("ut.txstep.init.json", step.toJson());

        assertNull(step.getId());
        assertNull(step.getDescription());
        assertNull(step.getSql());
        assertNull(step.getVersion());
        assertNull(step.getCreatedAt());
        assertNull(step.getUpdatedAt());
        assertNull(step.getStatus());
        assertNull(step.getCorrelationId());
        assertNull(step.getSnapshotHash());
    }

    @Test
    public void txStepRequest_shouldSetAndGetFieldsCorrectly() {

        TxStepRequest step = new TxStepRequest();

        step.setId("step-1");
        step.setDescription("Rank options");
        step.setSql("select decision from llm where task = 'rank'");
        step.setVersion("v0");
        step.setStatus("STAGED");
        step.setCorrelationId("corr-123");
        step.setSnapshotHash("hash-abc");

        Console.log("ut.txstep.setters.json", step.toJson());

        assertEquals("step-1", step.getId());
        assertEquals("Rank options", step.getDescription());
        assertEquals("select decision from llm where task = 'rank'", step.getSql());
        assertEquals("v0", step.getVersion());
        assertEquals("STAGED", step.getStatus());
        assertEquals("corr-123", step.getCorrelationId());
        assertEquals("hash-abc", step.getSnapshotHash());
    }

    @Test
    public void txStepRequest_shouldSerializeToJsonDeterministically() {

        TxStepRequest step = new TxStepRequest();
        step.setId("step-2");
        step.setDescription("Fetch options");
        step.setSql("select decision from llm where task = 'fetch'");

        String json = step.toJson();

        Console.log("ut.txstep.json.raw", json);

        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

        assertEquals("step-2", obj.get("id").getAsString());
        assertEquals("Fetch options", obj.get("description").getAsString());
        assertEquals("select decision from llm where task = 'fetch'", obj.get("sql").getAsString());
    }
}