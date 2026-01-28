package io.braineous.dd.llm.persistence;

import io.braineous.dd.llm.cr.model.CommitEvent;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;

import static com.mongodb.client.model.Filters.eq;

public class CommitEventMongoStore {

    public static final String DEFAULT_DB_NAME = "cgo";
    public static final String DEFAULT_COLLECTION_NAME = "cr_commit_events";

    private MongoClient mongoClient;
    private String dbName;
    private String collectionName;

    public CommitEventMongoStore(MongoClient mongoClient) {
        this(mongoClient, DEFAULT_DB_NAME, DEFAULT_COLLECTION_NAME);
    }

    public CommitEventMongoStore(MongoClient mongoClient, String dbName, String collectionName) {
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

    public CommitEvent getEvent(String commitId) {
        String id = safe(commitId);
        if (id == null) {
            return null;
        }

        Document doc = getCollection().find(eq("commitId", id)).first();
        if (doc == null) {
            return null;
        }

        Object raw = doc.get("event");
        if (!(raw instanceof Document)) {
            return null;
        }

        Document eventDoc = (Document) raw;

        try {
            JsonElement el = JsonParser.parseString(eventDoc.toJson());
            if (el == null || !el.isJsonObject()) {
                return null;
            }
            return CommitEvent.fromJson(el.getAsJsonObject());
        } catch (RuntimeException re) {
            return null;
        }
    }

    public void upsertEvent(CommitEvent event) {
        if (event == null) {
            return;
        }

        String id = event.safeCommitId();
        if (id == null) {
            return;
        }

        JsonObject ej = event.toJson();

        Document doc = new Document();
        doc.put("commitId", id);
        doc.put("event", Document.parse(ej.toString()));

        getCollection().replaceOne(eq("commitId", id), doc, new ReplaceOptions().upsert(true));
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

