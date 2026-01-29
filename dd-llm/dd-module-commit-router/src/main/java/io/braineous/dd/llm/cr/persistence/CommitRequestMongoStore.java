package io.braineous.dd.llm.cr.persistence;

import com.mongodb.client.model.Filters;
import io.braineous.dd.llm.cr.model.CommitRequest;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;

import static com.mongodb.client.model.Filters.eq;

@ApplicationScoped
public class CommitRequestMongoStore {

    public static final String DEFAULT_DB_NAME = "cgo";
    public static final String DEFAULT_COLLECTION_NAME = "cr_commit_requests";

    private MongoClient mongoClient;
    private String dbName;
    private String collectionName;

    @Inject
    public CommitRequestMongoStore(MongoClient mongoClient) {
        this(mongoClient, DEFAULT_DB_NAME, DEFAULT_COLLECTION_NAME);
    }

    public CommitRequestMongoStore(MongoClient mongoClient, String dbName, String collectionName) {
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

    // -------------------------
    // Orchestrator-facing ops
    // -------------------------

    public CommitRequest getRequest(String commitId) {
        String id = safe(commitId);
        if (id == null) {
            return null;
        }

        Document doc = getCollection().find(Filters.eq("commitId", id)).first();
        if (doc == null) {
            return null;
        }

        Object raw = doc.get("request");
        if (!(raw instanceof Document)) {
            return null;
        }

        Document requestDoc = (Document) raw;

        try {
            JsonElement el = JsonParser.parseString(requestDoc.toJson());
            if (el == null || !el.isJsonObject()) {
                return null;
            }
            return CommitRequest.fromJson(el.getAsJsonObject());
        } catch (RuntimeException re) {
            return null;
        }
    }

    public void upsertRequest(String commitId, CommitRequest request) {
        String id = safe(commitId);
        if (id == null) {
            return;
        }
        if (request == null) {
            return;
        }

        JsonObject rj = request.toJson();

        Document doc = new Document();
        doc.put("commitId", id);
        doc.put("request", Document.parse(rj.toString()));

        getCollection().replaceOne(Filters.eq("commitId", id), doc, new ReplaceOptions().upsert(true));
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

