package io.braineous.dd.llm.audit.client;

import ai.braineous.rag.prompt.observe.Console;
import com.google.gson.JsonObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import io.braineous.dd.llm.core.model.Why;
import io.braineous.dd.llm.cr.model.CommitAuditStatus;
import io.braineous.dd.llm.cr.model.CommitAuditView;
import io.braineous.dd.llm.cr.model.CommitEvent;
import io.braineous.dd.llm.cr.model.CommitRequest;
import io.braineous.dd.llm.cr.model.CommitReceipt;
import io.braineous.dd.llm.cr.persistence.CommitEventMongoStore;
import io.braineous.dd.llm.cr.persistence.CommitRequestMongoStore;
import io.braineous.dd.llm.cr.persistence.CommitReceiptMongoStore;
import io.braineous.dd.llm.cr.services.CommitAuditService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class CommitAuditClientIT {

    @Inject
    CommitAuditService svc;

    @Inject
    MongoClient mongoClient;

    @Inject
    CommitEventMongoStore eventStore;

    @Inject
    CommitRequestMongoStore requestStore;

    @Inject
    CommitReceiptMongoStore receiptStore;

    private CommitAuditClient client;

    @BeforeEach
    void setup() {
        MongoDatabase db = mongoClient.getDatabase("cgo");

        db.getCollection("cr_commit_events").drop();
        db.getCollection("cr_commit_requests").drop();
        db.getCollection("cr_commit_receipts").drop();

        try { Thread.sleep(250L); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }

        this.client = new CommitAuditClientImpl(svc);

        Console.log("CommitAuditClientIT.setup", "dropped cr_commit_* collections");
    }

    @Test
    void getAudit_null_or_blank_returns_null() {
        assertNull(client.getAudit(null));
        assertNull(client.getAudit(""));
        assertNull(client.getAudit("   "));
    }

    @Test
    void getAudit_missing_all_returns_null() {
        CommitAuditView v = client.getAudit("cr_missing");
        assertNull(v);
    }

    @Test
    void getAudit_pending_when_event_present_but_receipt_missing() {
        String commitId = "cr_pending_1";

        CommitEvent ev = new CommitEvent();
        ev.setCommitId(commitId);
        ev.setCreatedAt("2026-01-28T10:00:00Z");
        eventStore.upsertEvent(ev);

        CommitAuditView v = client.getAudit(commitId);

        assertNotNull(v);
        assertEquals(CommitAuditStatus.PENDING, v.getStatus());
        assertNotNull(v.getEvent());
        assertEquals("2026-01-28T10:00:00Z", v.getCreatedAt());
        assertNull(v.getReceipt());
    }

    @Test
    void getAudit_completed_accepted_lifts_truth_surface() {
        String commitId = "cr_done_ok";

        CommitRequest req = new CommitRequest();
        req.setQueryKind("policygate.v1");
        req.setCatalogVersion("v1");
        req.setActor("manual");

        List<String> notes = new ArrayList<String>();
        notes.add("first");
        req.setNotes(notes);

        JsonObject payload = new JsonObject();
        payload.addProperty("k", "v");
        req.setPayload(payload);

        requestStore.upsertRequest(commitId, req);

        CommitReceipt rcpt = new CommitReceipt();
        rcpt.setCommitId(commitId);
        rcpt.setAccepted(true);
        rcpt.setWhyCode(new Why("CR_OK", "accepted"));
        rcpt.setMessage("ok");
        receiptStore.upsertReceipt(rcpt);

        CommitAuditView v = client.getAudit(commitId);

        assertNotNull(v);
        assertEquals(CommitAuditStatus.COMPLETED_ACCEPTED, v.getStatus());
        assertNotNull(v.getRequest());
        assertNotNull(v.getReceipt());

        assertNotNull(v.getWhyCode());
        assertEquals("CR_OK", v.getWhyCode().getReason());
        assertEquals("accepted", v.getWhyCode().getDetails());
        assertEquals("ok", v.getMessage());
    }

    @Test
    void getAudit_completed_rejected_when_receipt_accepted_false() {
        String commitId = "cr_done_no";

        CommitReceipt rcpt = new CommitReceipt();
        rcpt.setCommitId(commitId);
        rcpt.setAccepted(false);
        rcpt.setWhyCode(new Why("CR_DENY", "policy"));
        rcpt.setMessage("denied");
        receiptStore.upsertReceipt(rcpt);

        CommitAuditView v = client.getAudit(commitId);

        assertNotNull(v);
        assertEquals(CommitAuditStatus.COMPLETED_REJECTED, v.getStatus());
        assertNotNull(v.getReceipt());

        assertNotNull(v.getWhyCode());
        assertEquals("CR_DENY", v.getWhyCode().getReason());
        assertEquals("policy", v.getWhyCode().getDetails());
        assertEquals("denied", v.getMessage());
    }
}
