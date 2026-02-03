package io.braineous.dd.llm.pg.services;

import ai.braineous.cgo.history.MongoHistoryStore;
import ai.braineous.rag.prompt.cgo.prompt.CatalogEntry;
import ai.braineous.rag.prompt.cgo.prompt.CatalogMongoStore;
import ai.braineous.rag.prompt.observe.Console;
import com.google.gson.JsonObject;
import com.mongodb.client.MongoClient;
import io.braineous.dd.llm.cr.model.CommitRequest;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class PolicyGateOrchestratorIT {

    @Inject
    PolicyGateOrchestrator pg;

    @Inject
    MongoClient mongoClient;

    @BeforeEach
    void setup() {
        Console.log("PG_ORCH_IT/setup", "start");

        mongoClient.getDatabase(MongoHistoryStore.DEFAULT_DB_NAME).drop();
        try { Thread.sleep(150L); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }

        // Harden: prove CDI wiring produced a live orchestrator
        assertNotNull(pg);

        Console.log("PG_ORCH_IT/setup", "done");
    }
}

