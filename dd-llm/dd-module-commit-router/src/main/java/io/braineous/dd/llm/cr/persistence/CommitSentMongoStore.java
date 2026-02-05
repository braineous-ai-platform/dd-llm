package io.braineous.dd.llm.cr.persistence;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;

import java.time.Instant;

@ApplicationScoped
public class CommitSentMongoStore {

    public static final String DEFAULT_DB_NAME = "cgo";
    public static final String DEFAULT_COLLECTION_NAME = "cr_commit_sent";

    private MongoClient mongoClient;
    private String dbName;
    private String collectionName;

    @Inject
    public CommitSentMongoStore(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
        this.dbName = DEFAULT_DB_NAME;
        this.collectionName = DEFAULT_COLLECTION_NAME;
    }

    // -------------------------
    // best-effort breadcrumb ops
    // -------------------------

    public void append(String commitId, String payload) {
        String id = safe(commitId);
        String p = safe(payload);
        if (id == null || p == null) {
            return;
        }

        Document doc = new Document();
        doc.put("createdAt", Instant.now().toString());
        doc.put("commitId", id);
        doc.put("payload", p);

        getCollection().insertOne(doc);
    }

    public long count() {
        return getCollection().countDocuments();
    }

    public void clear() {
        getCollection().deleteMany(new Document());
    }

    // -------------------------
    // helpers
    // -------------------------

    private MongoCollection<Document> getCollection() {
        MongoDatabase db = mongoClient.getDatabase(dbName);
        return db.getCollection(collectionName);
    }

    private String safe(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return null;
        }
        return t;
    }
}

