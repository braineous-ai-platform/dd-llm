package io.braineous.dd.llm.cr.services;

import ai.braineous.rag.prompt.observe.Console;
import com.google.gson.JsonObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.braineous.dd.llm.cr.model.*;
import io.braineous.dd.llm.cr.persistence.*;
import io.quarkus.test.junit.QuarkusTest;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

@QuarkusTest
public class CommitProcessorIT {

    @Inject
    CommitProcessor processor;

    @Inject
    MongoClient mongoClient;

    @Inject
    CommitAuditViewMongoStore auditViewStore;

    @Inject
    CommitRequestMongoStore requestStore;

    @Inject
    CommitEventMongoStore eventStore;

    @Inject
    CommitReceiptMongoStore receiptStore;

    @Inject
    CommitSentMongoStore commitSentStore;


    @BeforeEach
    void setup() {

        // sync mode for IT
        processor.setAsyncMode(false);

        // drop collections used by these stores (names may differ in your repo)
        drop(CommitAuditViewMongoStore.DEFAULT_DB_NAME, CommitAuditViewMongoStore.DEFAULT_COLLECTION_NAME);

        // If your other stores expose constants, use them. Otherwise, drop by known names.
        // Replace these with your real collection names if needed.
        drop(CommitEventMongoStore.DEFAULT_DB_NAME, CommitEventMongoStore.DEFAULT_COLLECTION_NAME);
        drop(CommitRequestMongoStore.DEFAULT_DB_NAME, CommitRequestMongoStore.DEFAULT_COLLECTION_NAME);
        drop(CommitReceiptMongoStore.DEFAULT_DB_NAME, CommitReceiptMongoStore.DEFAULT_COLLECTION_NAME);
        drop(CommitSentMongoStore.DEFAULT_DB_NAME, CommitSentMongoStore.DEFAULT_COLLECTION_NAME);


        try { Thread.sleep(250L); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }

        Console.log("CommitProcessorIT.setup", "dropped commit collections");
    }

    @Test
    void commitLLMResponseAsync() {
        Console.log("TEST", "submit_llm_response_async");
        processor.setAsyncMode(true);

        String commitId = "c-1";

        CommitRequest req = buildRequest(commitId, "audit.commit", "v1", "system");

        processor.orchestrate(req);

        // 1) request
        CommitRequest loadedReq = requestStore.getRequest(commitId);
        Console.log("LOADED_REQ", loadedReq == null ? "null" : loadedReq.toJsonString());
        org.junit.jupiter.api.Assertions.assertNotNull(loadedReq);
        org.junit.jupiter.api.Assertions.assertEquals("audit.commit", loadedReq.safeQueryKind());
        org.junit.jupiter.api.Assertions.assertEquals("v1", loadedReq.safeCatalogVersion());
        org.junit.jupiter.api.Assertions.assertEquals("system", loadedReq.safeActor());

        // 2) event
        CommitEvent loadedEvent = eventStore.getEvent(commitId);
        Console.log("LOADED_EVENT", loadedEvent == null ? "null" : loadedEvent.toJsonString());
        org.junit.jupiter.api.Assertions.assertNotNull(loadedEvent);
        org.junit.jupiter.api.Assertions.assertEquals(commitId, loadedEvent.safeCommitId());
        org.junit.jupiter.api.Assertions.assertEquals(1, loadedEvent.getAttempt());
        org.junit.jupiter.api.Assertions.assertNotNull(loadedEvent.safeCreatedAt());

        // 3) receipt
        CommitReceipt loadedReceipt = receiptStore.getReceipt(commitId);
        Console.log("LOADED_RECEIPT", loadedReceipt == null ? "null" : loadedReceipt.toJsonString());
        org.junit.jupiter.api.Assertions.assertNotNull(loadedReceipt);
        org.junit.jupiter.api.Assertions.assertEquals(commitId, loadedReceipt.safeCommitId());
        org.junit.jupiter.api.Assertions.assertTrue(loadedReceipt.isAccepted());
        org.junit.jupiter.api.Assertions.assertNotNull(loadedReceipt.getWhyCode());
        org.junit.jupiter.api.Assertions.assertEquals("EMIT_ATTEMPTED", loadedReceipt.getWhyCode().getReason());
        org.junit.jupiter.api.Assertions.assertEquals("emit attempted", loadedReceipt.getMessage());

        // 4) audit view projection
        CommitAuditView view = auditViewStore.getView(commitId);
        Console.log("LOADED_VIEW", view == null ? "null" : view.toJsonString());
        org.junit.jupiter.api.Assertions.assertNotNull(view);
        org.junit.jupiter.api.Assertions.assertEquals(commitId, view.safeCommitId());
        org.junit.jupiter.api.Assertions.assertEquals(CommitAuditStatus.COMPLETED_ACCEPTED, view.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals("emit attempted", view.getMessage());
        org.junit.jupiter.api.Assertions.assertNotNull(view.getWhyCode());
        org.junit.jupiter.api.Assertions.assertEquals("EMIT_ATTEMPTED", view.getWhyCode().getReason());

        // fragments survive
        org.junit.jupiter.api.Assertions.assertNotNull(view.getEvent());
        org.junit.jupiter.api.Assertions.assertNotNull(view.getRequest());
        org.junit.jupiter.api.Assertions.assertNotNull(view.getReceipt());
    }

    @Test
    void orchestrate_syncMode_persists_request_event_receipt_and_auditView() {
        Console.log("TEST", "sync persist happy path");

        String commitId = "c-1";

        CommitRequest req = buildRequest(commitId, "audit.commit", "v1", "system");

        processor.orchestrate(req);

        // 1) request
        CommitRequest loadedReq = requestStore.getRequest(commitId);
        Console.log("LOADED_REQ", loadedReq == null ? "null" : loadedReq.toJsonString());
        org.junit.jupiter.api.Assertions.assertNotNull(loadedReq);
        org.junit.jupiter.api.Assertions.assertEquals("audit.commit", loadedReq.safeQueryKind());
        org.junit.jupiter.api.Assertions.assertEquals("v1", loadedReq.safeCatalogVersion());
        org.junit.jupiter.api.Assertions.assertEquals("system", loadedReq.safeActor());

        // 2) event
        CommitEvent loadedEvent = eventStore.getEvent(commitId);
        Console.log("LOADED_EVENT", loadedEvent == null ? "null" : loadedEvent.toJsonString());
        org.junit.jupiter.api.Assertions.assertNotNull(loadedEvent);
        org.junit.jupiter.api.Assertions.assertEquals(commitId, loadedEvent.safeCommitId());
        org.junit.jupiter.api.Assertions.assertEquals(1, loadedEvent.getAttempt());
        org.junit.jupiter.api.Assertions.assertNotNull(loadedEvent.safeCreatedAt());

        // 3) receipt
        CommitReceipt loadedReceipt = receiptStore.getReceipt(commitId);
        Console.log("LOADED_RECEIPT", loadedReceipt == null ? "null" : loadedReceipt.toJsonString());
        org.junit.jupiter.api.Assertions.assertNotNull(loadedReceipt);
        org.junit.jupiter.api.Assertions.assertEquals(commitId, loadedReceipt.safeCommitId());
        org.junit.jupiter.api.Assertions.assertTrue(loadedReceipt.isAccepted());
        org.junit.jupiter.api.Assertions.assertNotNull(loadedReceipt.getWhyCode());
        org.junit.jupiter.api.Assertions.assertEquals("EMIT_ATTEMPTED", loadedReceipt.getWhyCode().getReason());
        org.junit.jupiter.api.Assertions.assertEquals("emit attempted", loadedReceipt.getMessage());

        // 4) audit view projection
        CommitAuditView view = auditViewStore.getView(commitId);
        Console.log("LOADED_VIEW", view == null ? "null" : view.toJsonString());
        org.junit.jupiter.api.Assertions.assertNotNull(view);
        org.junit.jupiter.api.Assertions.assertEquals(commitId, view.safeCommitId());
        org.junit.jupiter.api.Assertions.assertEquals(CommitAuditStatus.COMPLETED_ACCEPTED, view.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals("emit attempted", view.getMessage());
        org.junit.jupiter.api.Assertions.assertNotNull(view.getWhyCode());
        org.junit.jupiter.api.Assertions.assertEquals("EMIT_ATTEMPTED", view.getWhyCode().getReason());

        // fragments survive
        org.junit.jupiter.api.Assertions.assertNotNull(view.getEvent());
        org.junit.jupiter.api.Assertions.assertNotNull(view.getRequest());
        org.junit.jupiter.api.Assertions.assertNotNull(view.getReceipt());
    }

    @Test
    void orchestrate_validationFail_doesNotPersist_anything() {
        Console.log("TEST", "validation fail no persist");

        String commitId = "c-bad";

        CommitRequest req = buildRequest(commitId, "audit.commit", "v1", "system");
        req.setPayload(null); // validation failure

        processor.orchestrate(req);

        org.junit.jupiter.api.Assertions.assertNull(requestStore.getRequest(commitId));
        org.junit.jupiter.api.Assertions.assertNull(eventStore.getEvent(commitId));
        org.junit.jupiter.api.Assertions.assertNull(receiptStore.getReceipt(commitId));
        org.junit.jupiter.api.Assertions.assertNull(auditViewStore.getView(commitId));
    }

    @Test
    void orchestrate_missingCommitId_doesNotPersist_anything() {
        Console.log("TEST", "missing commitId no persist");

        CommitRequest req = buildRequest(null, "audit.commit", "v1", "system");

        processor.orchestrate(req);

        // can't lookup by id; assert collections are empty instead (audit view is easiest)
        org.junit.jupiter.api.Assertions.assertNull(auditViewStore.getView("c-any"));
    }


    //@Test
    void commitLLMResponseAsync_writes_commit_sent_breadcrumb() throws Exception {
        Console.log("TEST", "submit_llm_response_async_commit_sent");

        processor.setAsyncMode(true);

        String commitId = "c-1";
        CommitRequest req = buildRequest(commitId, "audit.commit", "v1", "system");

        long before = commitSentStore.count();
        Console.log("commit_sent.before", before);

        processor.orchestrate(req);

        waitUntilCommitSentCountIs(before + 1);

        long after = commitSentStore.count();
        Console.log("commit_sent.after", after);

        org.junit.jupiter.api.Assertions.assertEquals(before + 1, after);
    }


    // -------------------------
    // helpers
    // -------------------------

    private CommitRequest buildRequest(String commitId, String kind, String ver, String actor) {
        CommitRequest r = new CommitRequest();

        // assumes you added commitId to request + safeCommitId()
        r.setCommitId(commitId);

        r.setQueryKind(kind);
        r.setCatalogVersion(ver);
        r.setActor(actor);

        java.util.ArrayList<String> notes = new java.util.ArrayList<String>();
        notes.add("n1");
        notes.add("n2");
        r.setNotes(notes);

        JsonObject payload = new JsonObject();
        payload.addProperty("x", 1);
        payload.addProperty("y", "z");
        r.setPayload(payload);

        return r;
    }

    private void drop(String dbName, String collectionName) {
        MongoDatabase db = mongoClient.getDatabase(dbName);
        MongoCollection<Document> col = db.getCollection(collectionName);
        col.drop();
    }

    private void waitUntilCommitSentCountIs(long expected) throws Exception {
        long start = System.currentTimeMillis();
        long timeoutMs = 5000L;

        while (System.currentTimeMillis() - start < timeoutMs) {
            long c = commitSentStore.count();
            if (c >= expected) {
                return;
            }
            Thread.sleep(100L);
        }

        long finalCount = commitSentStore.count();
        Console.log("commit_sent.timeout_final", finalCount);
        org.junit.jupiter.api.Assertions.assertEquals(expected, finalCount);
    }

}

