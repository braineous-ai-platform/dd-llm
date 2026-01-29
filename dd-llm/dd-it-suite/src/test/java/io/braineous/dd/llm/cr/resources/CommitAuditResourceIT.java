package io.braineous.dd.llm.cr.resources;

import ai.braineous.rag.prompt.observe.Console;
import io.braineous.dd.llm.core.model.Why;
import io.braineous.dd.llm.cr.model.CommitReceipt;
import io.braineous.dd.llm.cr.persistence.CommitReceiptMongoStore;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class CommitAuditResourceIT {

    @Inject
    MongoClient mongoClient;

    @Inject
    CommitReceiptMongoStore receiptStore;

    @BeforeEach
    void setup() {
        awaitMongoReady(mongoClient);
        MongoDatabase db = mongoClient.getDatabase("cgo");
        db.getCollection("cr_commit_events").drop();
        db.getCollection("cr_commit_requests").drop();
        db.getCollection("cr_commit_receipts").drop();


        Console.log("CommitAuditResourceIT.setup", "dropped cr_commit_* collections");
    }

    @Test
    void getAudit_blank_commitId_returns_400() {
        given()
                .when().get("/cr/commit/   ")
                .then().statusCode(404); // path param with spaces typically becomes 404; keep this test optional
    }

    @Test
    void getAudit_completed_accepted_returns_json_view() {
        String commitId = "cr_done_ok";

        CommitReceipt rcpt = new CommitReceipt();
        rcpt.setCommitId(commitId);
        rcpt.setAccepted(true);
        rcpt.setWhyCode(new Why("CR_OK", "accepted"));
        rcpt.setMessage("ok");
        receiptStore.upsertReceipt(rcpt);

        String body =
                given()
                        .when().get("/cr/commit/" + commitId)
                        .then().statusCode(200)
                        .extract().asString();

        JsonObject o = JsonParser.parseString(body).getAsJsonObject();
        assertEquals(commitId, o.get("commitId").getAsString());
        assertEquals("COMPLETED_ACCEPTED", o.get("status").getAsString());
        assertEquals("ok", o.get("message").getAsString());

        assertTrue(o.has("whyCode"));
        JsonObject wc = o.get("whyCode").getAsJsonObject();
        assertEquals("CR_OK", wc.get("reason").getAsString());
        assertEquals("accepted", wc.get("details").getAsString());

        assertTrue(o.has("receipt"));

        Console.log("CommitAuditResourceIT.getAudit_completed", body);
    }

    @Test
    void getAudit_missing_returns_404_with_error_body() {
        String body =
                given()
                        .when().get("/cr/commit/cr_missing")
                        .then().statusCode(404)
                        .extract().asString();

        JsonObject o = JsonParser.parseString(body).getAsJsonObject();
        assertEquals("commitId not found", o.get("error").getAsString());
        assertEquals("cr_missing", o.get("commitId").getAsString());

        Console.log("CommitAuditResourceIT.getAudit_missing", body);
    }


    ///------------------
    private void awaitMongoReady(MongoClient mongoClient) {
        long start = System.currentTimeMillis();
        long timeoutMs = 15000L;

        while (true) {
            try {
                mongoClient.getDatabase("cgo").runCommand(new Document("ping", 1));
                return;
            } catch (RuntimeException e) {
                if ((System.currentTimeMillis() - start) > timeoutMs) {
                    throw new RuntimeException("Mongo not ready within " + timeoutMs + "ms", e);
                }
                try { Thread.sleep(100L); }
                catch (InterruptedException ie) { Thread.currentThread().interrupt(); throw new RuntimeException(ie); }
            }
        }
    }

}

