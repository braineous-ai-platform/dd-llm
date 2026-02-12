package io.braineous.dd.llm.policygate.client;

import ai.braineous.cgo.history.HistoryRecord;
import ai.braineous.cgo.history.MongoHistoryStore;
import ai.braineous.cgo.history.ScorerResult;
import ai.braineous.rag.prompt.cgo.api.GraphContext;
import ai.braineous.rag.prompt.cgo.api.Meta;
import ai.braineous.rag.prompt.cgo.api.QueryExecution;
import ai.braineous.rag.prompt.cgo.api.ValidateTask;
import ai.braineous.rag.prompt.cgo.query.QueryRequest;
import ai.braineous.rag.prompt.observe.Console;
import com.mongodb.client.MongoClient;
import io.braineous.dd.llm.pg.model.ExecutionView;
import io.braineous.dd.llm.pg.model.PolicyGateResult;
import io.braineous.dd.llm.pg.services.PolicyGateOrchestrator;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class PolicyGateClientImplIT {

    @Inject
    MongoClient mongoClient;

    @Inject
    MongoHistoryStore historyStore;

    @Inject
    PolicyGateOrchestrator orch;

    private PolicyGateClient client;

    @BeforeEach
    void setup() {

        awaitMongoReady(mongoClient);

        if (this.historyStore != null) {
            this.historyStore.clear();
        }

        this.client = new PolicyGateClientImpl(this.orch);

        Console.log("PolicyGateClientImplIT.setup", "cleared history_store");
    }

    @Test
    void getExecutions_happyPath_returns_view_and_contains_seeded_factId() {

        seedHistoryRecord("validate_flight_airports", "Flight:F100");

        ExecutionView v = this.client.getExecutions("validate_flight_airports");
        assertNotNull(v);
        assertEquals("validate_flight_airports", v.safeQueryKind());

        List<QueryExecution> list = v.safeExecutions();
        assertNotNull(list);
        assertTrue(list.size() >= 1);

        boolean found = false;
        for (int i = 0; i < list.size(); i++) {
            QueryExecution ex = list.get(i);
            if (ex == null) {
                continue;
            }

            QueryRequest rq = null;
            try {
                rq = ex.getRequest();
            } catch (RuntimeException re) {
                rq = null;
            }

            if (rq == null) {
                continue;
            }

            String fid = null;
            try {
                fid = rq.safeFactId();
            } catch (RuntimeException re) {
                fid = null;
            }

            if ("Flight:F100".equals(fid)) {
                found = true;
                break;
            }
        }

        assertTrue(found);
    }

    @Test
    void approve_happyPath_returns_ok_true_and_commitId_echo() {

        seedHistoryRecord("validate_flight_airports", "Flight:F100");

        PolicyGateResult r = this.client.approve("validate_flight_airports", "Flight:F100");
        assertNotNull(r);

        assertTrue(r.isOk());
        assertEquals("Flight:F100", r.safeCommitId());
    }

    @Test
    void approve_unknownCommitId_returns_ok_false_and_why_present() {

        seedHistoryRecord("validate_flight_airports", "Flight:F100");

        PolicyGateResult r = this.client.approve("validate_flight_airports", "Flight:DOES_NOT_EXIST");
        assertNotNull(r);

        assertTrue(!r.isOk());
        assertEquals("Flight:DOES_NOT_EXIST", r.safeCommitId());
        assertNotNull(r.safeWhy());
    }

    // ---------------------------------------------------------
    // Seed (history evidence = source of truth)
    // ---------------------------------------------------------

    private void seedHistoryRecord(String queryKind, String factId) {

        Meta meta = new Meta(
                "v1",
                queryKind,
                "seed"
        );

        GraphContext ctx = new GraphContext();

        ValidateTask task = new ValidateTask(queryKind, factId);

        QueryRequest<?> req =
                new QueryRequest<>(meta, ctx, task, factId);

        QueryExecution<?> exec =
                new QueryExecution<>(req);

        ScorerResult scorer = ScorerResult.ok("ok");

        HistoryRecord record =
                new HistoryRecord(exec, scorer);

        this.historyStore.addRecord(record);

        Console.log("PolicyGateClientImplIT.seedHistoryRecord",
                "seeded queryKind=" + queryKind + " factId=" + factId);
    }

    // ---------------------------------------------------------
    // mongo readiness (same style as PolicyGateResourceIT)
    // ---------------------------------------------------------

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
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ie);
                }
            }
        }
    }
}
