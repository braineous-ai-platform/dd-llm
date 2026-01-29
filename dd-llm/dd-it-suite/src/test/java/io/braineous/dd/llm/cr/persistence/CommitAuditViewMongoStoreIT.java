package io.braineous.dd.llm.cr.persistence;

import ai.braineous.rag.prompt.observe.Console;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.braineous.dd.llm.core.model.Why;
import io.braineous.dd.llm.cr.model.*;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class CommitAuditViewMongoStoreIT {

    @Inject
    MongoClient mongoClient;

    private CommitAuditViewMongoStore store;

    @BeforeEach
    void setup() {
        this.store = new CommitAuditViewMongoStore(mongoClient);

        // IMPORTANT: create indexes explicitly because @PostConstruct does NOT run on "new"
        store.ensureIndexesNow();

        // IMPORTANT: don't drop collection (drop nukes indexes); just clear docs
        store.clear();

        ai.braineous.rag.prompt.observe.Console.log(
                "CommitAuditViewMongoStoreIT.setup",
                "ensureIndexesNow + clear cr_commit_audit_views"
        );
    }


    @Test
    void upsertView_then_getView_roundtrip_happyPath() {
        ai.braineous.rag.prompt.observe.Console.log("TEST", "roundtrip happy path");

        String commitId = "c-1";

        CommitRequest req = buildRequest("audit.commit", "v1", "system");
        CommitEvent ev = buildEvent(commitId, 1, "2026-01-29T10:00:00Z", req);

        CommitReceipt receipt = buildReceiptAccepted(commitId, "OK", null, "done");
        CommitAuditView view = CommitAuditView.from(commitId, ev, req, receipt);

        store.upsertView(view);

        CommitAuditView loaded = store.getView(commitId);
        ai.braineous.rag.prompt.observe.Console.log("LOADED", loaded == null ? "null" : loaded.toJsonString());

        org.junit.jupiter.api.Assertions.assertNotNull(loaded);
        org.junit.jupiter.api.Assertions.assertEquals(commitId, loaded.safeCommitId());

        org.junit.jupiter.api.Assertions.assertEquals(CommitAuditStatus.COMPLETED_ACCEPTED, loaded.getStatus());
        org.junit.jupiter.api.Assertions.assertNotNull(loaded.getWhyCode());
        org.junit.jupiter.api.Assertions.assertEquals("done", loaded.getMessage());

        // createdAt is best-effort from event
        org.junit.jupiter.api.Assertions.assertEquals("2026-01-29T10:00:00Z", loaded.getCreatedAt());

        // fragments should survive
        org.junit.jupiter.api.Assertions.assertNotNull(loaded.getEvent());
        org.junit.jupiter.api.Assertions.assertNotNull(loaded.getRequest());
        org.junit.jupiter.api.Assertions.assertNotNull(loaded.getReceipt());
    }

    @Test
    void getView_missing_returnsNull() {
        ai.braineous.rag.prompt.observe.Console.log("TEST", "missing returns null");

        CommitAuditView loaded = store.getView("missing-1");
        org.junit.jupiter.api.Assertions.assertNull(loaded);
    }

    @Test
    void upsertView_nullOrBlankCommitId_isNoop() {
        ai.braineous.rag.prompt.observe.Console.log("TEST", "null/blank axis noop");

        // factory path returns null
        CommitAuditView v1 = CommitAuditView.from(null, null, null, null);
        store.upsertView(v1);

        // setter path
        CommitAuditView v2 = new CommitAuditView();
        v2.setCommitId("   ");
        store.upsertView(v2);

        org.junit.jupiter.api.Assertions.assertNull(store.getView(null));
        org.junit.jupiter.api.Assertions.assertNull(store.getView("   "));
        org.junit.jupiter.api.Assertions.assertNull(store.getView("does-not-exist"));
    }

    @Test
    void upsertView_overwritesExisting_sameCommitId() {
        ai.braineous.rag.prompt.observe.Console.log("TEST", "overwrite same commitId");

        String commitId = "c-2";

        CommitReceipt r1 = buildReceiptRejected(commitId, "NOPE", "first-reject", "first");
        CommitAuditView v1 = CommitAuditView.from(commitId, null, null, r1);
        store.upsertView(v1);

        CommitReceipt r2 = buildReceiptAccepted(commitId, "OK", null, "second");
        CommitAuditView v2 = CommitAuditView.from(commitId, null, null, r2);
        store.upsertView(v2);

        CommitAuditView loaded = store.getView(commitId);
        ai.braineous.rag.prompt.observe.Console.log("LOADED", loaded == null ? "null" : loaded.toJsonString());

        org.junit.jupiter.api.Assertions.assertNotNull(loaded);
        org.junit.jupiter.api.Assertions.assertEquals(commitId, loaded.safeCommitId());
        org.junit.jupiter.api.Assertions.assertEquals(CommitAuditStatus.COMPLETED_ACCEPTED, loaded.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals("second", loaded.getMessage());
        org.junit.jupiter.api.Assertions.assertNotNull(loaded.getWhyCode());
    }

    @Test
    void getView_corruptStoredJson_returnsNull_and_doesNotThrow() {
        ai.braineous.rag.prompt.observe.Console.log("TEST", "corrupt doc returns null");

        com.mongodb.client.MongoCollection<org.bson.Document> col =
                mongoClient.getDatabase(CommitAuditViewMongoStore.DEFAULT_DB_NAME)
                        .getCollection(CommitAuditViewMongoStore.DEFAULT_COLLECTION_NAME);

        org.bson.Document bad = new org.bson.Document();
        bad.put("commitId", "c-bad");
        bad.put("event", "this-should-be-object-not-string");
        bad.put("receipt", "also-wrong");
        col.insertOne(bad);

        CommitAuditView loaded = store.getView("c-bad");
        org.junit.jupiter.api.Assertions.assertNull(loaded);
    }

    @Test
    void ensureIndexes_uniqueCommitId_enforced() {
        ai.braineous.rag.prompt.observe.Console.log("TEST", "unique index enforced");

        com.mongodb.client.MongoCollection<org.bson.Document> col =
                mongoClient.getDatabase(CommitAuditViewMongoStore.DEFAULT_DB_NAME)
                        .getCollection(CommitAuditViewMongoStore.DEFAULT_COLLECTION_NAME);

        org.bson.Document d1 = new org.bson.Document();
        d1.put("commitId", "c-uniq");
        d1.put("status", "PENDING");

        org.bson.Document d2 = new org.bson.Document();
        d2.put("commitId", "c-uniq");
        d2.put("status", "PENDING");

        col.insertOne(d1);

        boolean threw = false;
        try {
            col.insertOne(d2);
        } catch (RuntimeException re) {
            threw = true;
            ai.braineous.rag.prompt.observe.Console.log("EXPECTED_DUP_KEY", re.getMessage());
        }

        org.junit.jupiter.api.Assertions.assertTrue(threw);
    }


    //--------helpers---------------------
    private CommitRequest buildRequest(String kind, String ver, String actor) {
        CommitRequest r = new CommitRequest();
        r.setQueryKind(kind);
        r.setCatalogVersion(ver);
        r.setActor(actor);

        com.google.gson.JsonArray notes = new com.google.gson.JsonArray();
        notes.add("n1");
        notes.add("n2");

        java.util.ArrayList<String> list = new java.util.ArrayList<String>();
        for (int i = 0; i < notes.size(); i++) {
            list.add(notes.get(i).getAsString());
        }
        r.setNotes(list);

        com.google.gson.JsonObject payload = new com.google.gson.JsonObject();
        payload.addProperty("x", 1);
        payload.addProperty("y", "z");
        r.setPayload(payload);

        return r;
    }

    private CommitEvent buildEvent(String commitId, int attempt, String createdAt, CommitRequest req) {
        CommitEvent e = new CommitEvent();
        e.setCommitId(commitId);
        e.setAttempt(attempt);
        e.setCreatedAt(createdAt);
        e.setRequest(req);
        return e;
    }

    private CommitReceipt buildReceiptAccepted(String commitId, String reason, String details, String message) {
        CommitReceipt r = new CommitReceipt();
        r.setCommitId(commitId);
        r.setAccepted(true);
        if (reason != null) {
            r.setWhyCode(new Why(reason, details));
        }
        r.setMessage(message);
        return r;
    }

    private CommitReceipt buildReceiptRejected(String commitId, String reason, String details, String message) {
        CommitReceipt r = new CommitReceipt();
        r.setCommitId(commitId);
        r.setAccepted(false);
        if (reason != null) {
            r.setWhyCode(new Why(reason, details));
        }
        r.setMessage(message);
        return r;
    }

}
