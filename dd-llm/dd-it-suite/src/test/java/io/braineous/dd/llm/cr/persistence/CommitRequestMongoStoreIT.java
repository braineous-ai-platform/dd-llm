package io.braineous.dd.llm.cr.persistence;

import ai.braineous.rag.prompt.observe.Console;
import io.braineous.dd.llm.cr.model.CommitRequest;
import com.google.gson.JsonObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.braineous.dd.llm.cr.persistence.CommitRequestMongoStore;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import io.quarkus.test.junit.QuarkusTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class CommitRequestMongoStoreIT {

    @Inject
    MongoClient mongoClient;

    private CommitRequestMongoStore store;

    @BeforeEach
    void setup() {
        this.store = new CommitRequestMongoStore(mongoClient);

        MongoDatabase db = mongoClient.getDatabase(CommitRequestMongoStore.DEFAULT_DB_NAME);
        MongoCollection<Document> col = db.getCollection(CommitRequestMongoStore.DEFAULT_COLLECTION_NAME);

        col.drop();
        try { Thread.sleep(250L); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }

        Console.log("CommitRequestMongoStoreIT.setup", "dropped collection cr_commit_requests");
    }

    @Test
    void upsert_then_get_roundtrip() {
        String commitId = "cr_req_123";

        CommitRequest r = new CommitRequest();
        r.setQueryKind("policygate.v1");
        r.setCatalogVersion("v1");
        r.setDecision("ALLOW");
        r.setActor("manual");
        r.setRequestId("req-777");

        List<String> notes = new ArrayList<String>();
        notes.add(" first ");
        notes.add("  ");
        notes.add(null);
        notes.add("second");
        r.setNotes(notes);

        JsonObject payload = new JsonObject();
        payload.addProperty("k", "v");
        payload.addProperty("n", 7);
        r.setPayload(payload);

        store.upsertRequest(commitId, r);

        CommitRequest got = store.getRequest(commitId);

        assertNotNull(got);
        assertEquals("policygate.v1", got.getQueryKind());
        assertEquals("v1", got.getCatalogVersion());
        assertEquals("ALLOW", got.getDecision());
        assertEquals("manual", got.getActor());
        assertEquals("req-777", got.getRequestId());

        assertNotNull(got.getNotes());
        // your toJson() filters blanks/nulls; fromJson reads back only the kept ones
        assertEquals(2, got.getNotes().size());
        assertEquals("first", got.getNotes().get(0));
        assertEquals("second", got.getNotes().get(1));

        assertNotNull(got.getPayload());
        assertTrue(got.getPayload().has("k"));
        assertEquals("v", got.getPayload().get("k").getAsString());
        assertTrue(got.getPayload().has("n"));
        assertEquals(7, got.getPayload().get("n").getAsInt());

        // raw doc exists
        MongoDatabase db = mongoClient.getDatabase(CommitRequestMongoStore.DEFAULT_DB_NAME);
        MongoCollection<Document> col = db.getCollection(CommitRequestMongoStore.DEFAULT_COLLECTION_NAME);

        Document doc = col.find(new Document("commitId", commitId)).first();
        assertNotNull(doc);

        Object raw = doc.get("request");
        assertNotNull(raw);

        Console.log("CommitRequestMongoStoreIT.upsert_then_get_roundtrip", doc.toJson());
    }

    @Test
    void getRequest_null_or_blank_returns_null() {
        assertNull(store.getRequest(null));
        assertNull(store.getRequest(""));
        assertNull(store.getRequest("   "));
    }

    @Test
    void clear_deletes_all() {
        CommitRequest r1 = new CommitRequest();
        r1.setQueryKind("q1");
        r1.setDecision("ALLOW");

        CommitRequest r2 = new CommitRequest();
        r2.setQueryKind("q2");
        r2.setDecision("DENY");

        store.upsertRequest("cr_a", r1);
        store.upsertRequest("cr_b", r2);

        assertNotNull(store.getRequest("cr_a"));
        assertNotNull(store.getRequest("cr_b"));

        store.clear();

        assertNull(store.getRequest("cr_a"));
        assertNull(store.getRequest("cr_b"));

        MongoDatabase db = mongoClient.getDatabase(CommitRequestMongoStore.DEFAULT_DB_NAME);
        MongoCollection<Document> col = db.getCollection(CommitRequestMongoStore.DEFAULT_COLLECTION_NAME);

        long count = col.countDocuments();
        assertEquals(0L, count);

        Console.log("CommitRequestMongoStoreIT.clear_deletes_all", "count=" + count);
    }
}

