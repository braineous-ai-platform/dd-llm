package io.braineous.dd.llm.persistence;

import ai.braineous.rag.prompt.observe.Console;
import io.braineous.dd.llm.cr.model.CommitEvent;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import io.quarkus.test.junit.QuarkusTest;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class CommitEventMongoStoreIT {

    @Inject
    MongoClient mongoClient;

    private CommitEventMongoStore store;

    @BeforeEach
    void setup() {
        this.store = new CommitEventMongoStore(mongoClient);

        MongoDatabase db = mongoClient.getDatabase(CommitEventMongoStore.DEFAULT_DB_NAME);
        MongoCollection<Document> col = db.getCollection(CommitEventMongoStore.DEFAULT_COLLECTION_NAME);

        col.drop();
        try { Thread.sleep(250L); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }

        Console.log("CommitEventMongoStoreIT.setup", "dropped collection cr_commit_events");
    }

    @Test
    void upsert_then_get_roundtrip() {
        CommitEvent e = new CommitEvent();
        e.setCommitId("cr_123");
        e.setCreatedAt("2026-01-28T10:00:00Z");
        e.setRequest(null); // keep minimal for this IT

        store.upsertEvent(e);

        CommitEvent got = store.getEvent("cr_123");

        assertNotNull(got);
        assertEquals("cr_123", got.getCommitId());
        assertEquals("2026-01-28T10:00:00Z", got.getCreatedAt());
        assertNull(got.getRequest());

        // also verify raw doc exists
        MongoDatabase db = mongoClient.getDatabase(CommitEventMongoStore.DEFAULT_DB_NAME);
        MongoCollection<Document> col = db.getCollection(CommitEventMongoStore.DEFAULT_COLLECTION_NAME);

        Document doc = col.find(new Document("commitId", "cr_123")).first();
        assertNotNull(doc);

        Object raw = doc.get("event");
        assertNotNull(raw);

        Console.log("CommitEventMongoStoreIT.upsert_then_get_roundtrip", doc.toJson());
    }

    @Test
    void getEvent_null_or_blank_returns_null() {
        assertNull(store.getEvent(null));
        assertNull(store.getEvent(""));
        assertNull(store.getEvent("   "));
    }

    @Test
    void clear_deletes_all() {
        CommitEvent e1 = new CommitEvent();
        e1.setCommitId("cr_a");
        e1.setCreatedAt("2026-01-28T10:01:00Z");

        CommitEvent e2 = new CommitEvent();
        e2.setCommitId("cr_b");
        e2.setCreatedAt("2026-01-28T10:02:00Z");

        store.upsertEvent(e1);
        store.upsertEvent(e2);

        assertNotNull(store.getEvent("cr_a"));
        assertNotNull(store.getEvent("cr_b"));

        store.clear();

        assertNull(store.getEvent("cr_a"));
        assertNull(store.getEvent("cr_b"));

        MongoDatabase db = mongoClient.getDatabase(CommitEventMongoStore.DEFAULT_DB_NAME);
        MongoCollection<Document> col = db.getCollection(CommitEventMongoStore.DEFAULT_COLLECTION_NAME);

        long count = col.countDocuments();
        assertEquals(0L, count);

        Console.log("CommitEventMongoStoreIT.clear_deletes_all", "count=" + count);
    }
}
