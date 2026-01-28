package io.braineous.dd.llm.persistence;

import ai.braineous.rag.prompt.observe.Console;
import io.braineous.dd.llm.core.model.Why;
import io.braineous.dd.llm.cr.model.CommitReceipt;
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
public class CommitReceiptMongoStoreIT {

    @Inject
    MongoClient mongoClient;

    private CommitReceiptMongoStore store;

    @BeforeEach
    void setup() {
        this.store = new CommitReceiptMongoStore(mongoClient);

        MongoDatabase db = mongoClient.getDatabase(CommitReceiptMongoStore.DEFAULT_DB_NAME);
        MongoCollection<Document> col = db.getCollection(CommitReceiptMongoStore.DEFAULT_COLLECTION_NAME);

        col.drop();
        try { Thread.sleep(250L); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }

        Console.log("CommitReceiptMongoStoreIT.setup", "dropped collection cr_commit_receipts");
    }

    @Test
    void upsert_then_get_roundtrip() {
        CommitReceipt r = new CommitReceipt();
        r.setCommitId("cr_rcpt_123");
        r.setAccepted(true);
        r.setMessage("ok");

        Why wc = new Why("CR_OK", "accepted");
        r.setWhyCode(wc);

        store.upsertReceipt(r);

        CommitReceipt got = store.getReceipt("cr_rcpt_123");

        assertNotNull(got);
        assertEquals("cr_rcpt_123", got.getCommitId());
        assertTrue(got.isAccepted());
        assertEquals("ok", got.getMessage());

        assertNotNull(got.getWhyCode());
        assertEquals("CR_OK", got.getWhyCode().getReason());
        assertEquals("accepted", got.getWhyCode().getDetails());

        // raw doc exists
        MongoDatabase db = mongoClient.getDatabase(CommitReceiptMongoStore.DEFAULT_DB_NAME);
        MongoCollection<Document> col = db.getCollection(CommitReceiptMongoStore.DEFAULT_COLLECTION_NAME);

        Document doc = col.find(new Document("commitId", "cr_rcpt_123")).first();
        assertNotNull(doc);

        Object raw = doc.get("receipt");
        assertNotNull(raw);

        Console.log("CommitReceiptMongoStoreIT.upsert_then_get_roundtrip", doc.toJson());
    }

    @Test
    void getReceipt_null_or_blank_returns_null() {
        assertNull(store.getReceipt(null));
        assertNull(store.getReceipt(""));
        assertNull(store.getReceipt("   "));
    }

    @Test
    void clear_deletes_all() {
        CommitReceipt r1 = new CommitReceipt();
        r1.setCommitId("cr_a");
        r1.setAccepted(false);
        r1.setWhyCode(new Why("CR_FAIL", "bad"));
        r1.setMessage("nope");

        CommitReceipt r2 = new CommitReceipt();
        r2.setCommitId("cr_b");
        r2.setAccepted(true);
        r2.setWhyCode(new Why("CR_OK", "good"));
        r2.setMessage("yep");

        store.upsertReceipt(r1);
        store.upsertReceipt(r2);

        assertNotNull(store.getReceipt("cr_a"));
        assertNotNull(store.getReceipt("cr_b"));

        store.clear();

        assertNull(store.getReceipt("cr_a"));
        assertNull(store.getReceipt("cr_b"));

        MongoDatabase db = mongoClient.getDatabase(CommitReceiptMongoStore.DEFAULT_DB_NAME);
        MongoCollection<Document> col = db.getCollection(CommitReceiptMongoStore.DEFAULT_COLLECTION_NAME);

        long count = col.countDocuments();
        assertEquals(0L, count);

        Console.log("CommitReceiptMongoStoreIT.clear_deletes_all", "count=" + count);
    }
}
