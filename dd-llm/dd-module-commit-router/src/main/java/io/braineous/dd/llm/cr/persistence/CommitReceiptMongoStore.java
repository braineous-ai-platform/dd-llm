package io.braineous.dd.llm.cr.persistence;

import com.mongodb.client.model.Filters;
import io.braineous.dd.llm.cr.model.CommitReceipt;
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
public class CommitReceiptMongoStore {

    public static final String DEFAULT_DB_NAME = "cgo";
    public static final String DEFAULT_COLLECTION_NAME = "cr_commit_receipts";

    private MongoClient mongoClient;
    private String dbName;
    private String collectionName;

    @Inject
    public CommitReceiptMongoStore(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
        this.dbName = DEFAULT_DB_NAME;
        this.collectionName = DEFAULT_COLLECTION_NAME;
    }

    // -------------------------
    // Orchestrator-facing ops
    // -------------------------

    public CommitReceipt getReceipt(String commitId) {
        String id = safe(commitId);
        if (id == null) {
            return null;
        }

        Document doc = getCollection().find(Filters.eq("commitId", id)).first();
        if (doc == null) {
            return null;
        }

        Object raw = doc.get("receipt");
        if (!(raw instanceof Document)) {
            return null;
        }

        Document receiptDoc = (Document) raw;

        try {
            JsonElement el = JsonParser.parseString(receiptDoc.toJson());
            if (el == null || !el.isJsonObject()) {
                return null;
            }
            return CommitReceipt.fromJson(el.getAsJsonObject());
        } catch (RuntimeException re) {
            return null;
        }
    }

    public void upsertReceipt(CommitReceipt receipt) {
        if (receipt == null) {
            return;
        }

        String id = safeCommitId(receipt);
        if (id == null) {
            return;
        }

        JsonObject rj = receipt.toJson();

        Document doc = new Document();
        doc.put("commitId", id);
        doc.put("receipt", Document.parse(rj.toString()));

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

    private String safeCommitId(CommitReceipt r) {
        if (r == null) {
            return null;
        }
        String id = r.getCommitId();
        return safe(id);
    }
}
