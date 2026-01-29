package io.braineous.dd.llm.cr.persistence;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import io.braineous.dd.llm.cr.model.CommitAuditView;
import io.braineous.dd.llm.cr.model.CommitEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;

@ApplicationScoped
public class CommitAuditViewMongoStore {

    public static final String DEFAULT_DB_NAME = "cgo";
    public static final String DEFAULT_COLLECTION_NAME = "cr_commit_audit_views";

    private MongoClient mongoClient;
    private String dbName;
    private String collectionName;

    @Inject
    public CommitAuditViewMongoStore(MongoClient mongoClient) {
        this(mongoClient, DEFAULT_DB_NAME, DEFAULT_COLLECTION_NAME);
    }

    public CommitAuditViewMongoStore(MongoClient mongoClient, String dbName, String collectionName) {
        if (mongoClient == null) {
            throw new IllegalArgumentException("mongoClient cannot be null");
        }
        if (dbName == null || dbName.trim().isEmpty()) {
            throw new IllegalArgumentException("dbName cannot be null/empty");
        }
        if (collectionName == null || collectionName.trim().isEmpty()) {
            throw new IllegalArgumentException("collectionName cannot be null/empty");
        }
        this.mongoClient = mongoClient;
        this.dbName = dbName;
        this.collectionName = collectionName;
    }

    @jakarta.annotation.PostConstruct
    void init() {
        ensureIndexes();
    }

    // -------------------------
    // Orchestrator-facing ops
    // -------------------------

    public CommitAuditView getView(String commitId) {
        String id = safe(commitId);
        if (id == null) {
            return null;
        }

        Document doc = getCollection().find(Filters.eq("commitId", id)).first();
        if (doc == null) {
            return null;
        }

        try {
            JsonElement el = JsonParser.parseString(doc.toJson());
            if (el == null || !el.isJsonObject()) {
                return null;
            }
            return CommitAuditView.fromJson(el.getAsJsonObject());
        } catch (RuntimeException re) {
            return null;
        }
    }

    public void upsertView(CommitAuditView view) {
        if (view == null) {
            return;
        }

        String id = view.safeCommitId();
        if (id == null) {
            return;
        }

        Document doc = Document.parse(view.toJsonString());
        doc.put("commitId", id); // enforce axis

        getCollection().replaceOne(
                Filters.eq("commitId", id),
                doc,
                new ReplaceOptions().upsert(true)
        );
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

    private void ensureIndexes() {
        try {
            Document keys = new Document("commitId", 1);
            com.mongodb.client.model.IndexOptions opts =
                    new com.mongodb.client.model.IndexOptions()
                            .unique(true)
                            .name("ux_commitId");
            getCollection().createIndex(keys, opts);
        } catch (RuntimeException re) {
            // tolerate; IT will catch if truly broken
        }
    }

    void ensureIndexesNow() {
        ensureIndexes();
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


