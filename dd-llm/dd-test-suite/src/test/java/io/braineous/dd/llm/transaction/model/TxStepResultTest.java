package io.braineous.dd.llm.transaction.model;

import ai.braineous.rag.prompt.observe.Console;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.braineous.dd.llm.core.model.Why;
import io.braineous.dd.llm.query.client.QueryResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TxStepResultTest {

    @Test
    public void txStepResult_shouldInitializeWithNullFields() {

        TxStepResult r = new TxStepResult();

        Console.log("ut.txstepresult.init.json", r.toJson());

        assertNull(r.getId());
        assertNull(r.getDescription());
        assertNull(r.getQueryResult());
        assertFalse(r.isOk());

        assertNull(r.getVersion());
        assertNull(r.getCreatedAt());
        assertNull(r.getUpdatedAt());
        assertNull(r.getStatus());
        assertNull(r.getCorrelationId());
        assertNull(r.getSnapshotHash());
    }

    @Test
    public void txStepResult_shouldSetAndGetFieldsCorrectly() {

        TxStepResult r = new TxStepResult();

        r.setId("step-1");
        r.setDescription("Rank options");

        QueryResult qr = QueryResult.fail(new Why("Q_FAIL", "query failed"));
        r.setQueryResult(qr);

        r.setVersion("v0");
        r.setStatus("STAGED");
        r.setCorrelationId("corr-456");
        r.setSnapshotHash("hash-xyz");

        Console.log("ut.txstepresult.setters.json", r.toJson());

        assertEquals("step-1", r.getId());
        assertEquals("Rank options", r.getDescription());
        assertNotNull(r.getQueryResult());
        assertFalse(r.isOk());

        assertEquals("v0", r.getVersion());
        assertEquals("STAGED", r.getStatus());
        assertEquals("corr-456", r.getCorrelationId());
        assertEquals("hash-xyz", r.getSnapshotHash());
    }

    @Test
    public void txStepResult_isOkShouldReturnFalse_whenQueryResultNull() {

        TxStepResult r = new TxStepResult();
        r.setId("step-x");

        Console.log("ut.txstepresult.isok.null_qr.json", r.toJson());

        assertNull(r.getQueryResult());
        assertFalse(r.isOk());
    }

    @Test
    public void txStepResult_isOkShouldReturnTrue_whenQueryResultOkTrue() {

        TxStepResult r = new TxStepResult();
        r.setId("step-ok");

        QueryResult qr = new QueryResult();
        qr.setOk(true);

        r.setQueryResult(qr);

        Console.log("ut.txstepresult.isok.true.json", r.toJson());

        assertTrue(r.isOk());
    }

    @Test
    public void txStepResult_isOkShouldReturnFalse_whenQueryResultOkFalse() {

        TxStepResult r = new TxStepResult();
        r.setId("step-fail");

        QueryResult qr = new QueryResult();
        qr.setOk(false);

        r.setQueryResult(qr);

        Console.log("ut.txstepresult.isok.false.json", r.toJson());

        assertFalse(r.isOk());
    }

    @Test
    public void txStepResult_shouldSerializeToJson_withQueryResultPresent() {

        TxStepResult r = new TxStepResult();
        r.setId("step-2");
        r.setDescription("Fetch options");

        QueryResult qr = QueryResult.fail(new Why("Q_FAIL", "bad input"));
        r.setQueryResult(qr);

        String json = r.toJson();
        Console.log("ut.txstepresult.json.raw", json);

        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

        assertEquals("step-2", obj.get("id").getAsString());
        assertEquals("Fetch options", obj.get("description").getAsString());
        assertTrue(obj.has("queryResult"));
        assertFalse(obj.get("queryResult").isJsonNull());
        assertTrue(obj.get("queryResult").isJsonObject());

        JsonObject qj = obj.getAsJsonObject("queryResult");
        assertTrue(qj.has("ok"));
    }
}
