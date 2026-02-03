package io.braineous.dd.llm.pg.services;

import static org.junit.jupiter.api.Assertions.*;

import ai.braineous.cgo.history.ScorerResult;
import ai.braineous.rag.prompt.cgo.api.GraphContext;
import ai.braineous.rag.prompt.cgo.api.Meta;
import ai.braineous.rag.prompt.cgo.api.ValidateTask;
import ai.braineous.rag.prompt.cgo.query.QueryTask;
import ai.braineous.rag.prompt.observe.Console;
import com.google.gson.JsonObject;
import com.mongodb.client.MongoClient;
import io.braineous.dd.llm.cr.model.*;
import io.braineous.dd.llm.cr.persistence.CommitAuditViewMongoStore;
import io.braineous.dd.llm.cr.persistence.CommitEventMongoStore;
import io.braineous.dd.llm.cr.persistence.CommitReceiptMongoStore;
import io.braineous.dd.llm.cr.persistence.CommitRequestMongoStore;
import io.braineous.dd.llm.cr.services.CommitAuditService;
import io.braineous.dd.llm.cr.services.CommitProcessor;
import io.braineous.dd.llm.pg.model.PolicyGateResult;
import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;

import io.quarkus.test.junit.QuarkusTest;

import ai.braineous.cgo.history.MongoHistoryStore;
import ai.braineous.cgo.history.HistoryRecord;

import ai.braineous.rag.prompt.cgo.api.QueryExecution;
import ai.braineous.rag.prompt.cgo.query.QueryRequest;


// If these are your concrete mongo stores, import them.
// Otherwise replace with the correct classes in your repo.

// Optional: only if you want to call orchestrator directly in setup sanity checks.
// import io.braineous.dd.llm.pg.services.PolicyGateOrchestrator;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;

import io.quarkus.test.junit.QuarkusTest;

import ai.braineous.cgo.history.MongoHistoryStore;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class PolicyGateIT {

    @Inject
    MongoHistoryStore historyStore;

    @Inject
    CommitEventMongoStore eventStore;

    @Inject
    CommitRequestMongoStore requestStore;

    @Inject
    CommitReceiptMongoStore receiptStore;

    @Inject
    CommitAuditViewMongoStore auditStore;

    @Inject
    CommitAuditService auditService;

    @Inject
    PolicyGateOrchestrator policyGateOrchestrator;

    @Inject
    CommitProcessor commitProcessor;

    @BeforeEach
    public void setup() {
        historyStore.clear();
        eventStore.clear();
        requestStore.clear();
        receiptStore.clear();
        auditStore.clear();

        // force sync for IT
        commitProcessor.setAsyncMode(false);
    }

    @Test
    public void approve_creates_full_commit_audit_truth_surface() {

        // -------------------------
        // Seed history (simulate pipeline output)
        // -------------------------

        Meta meta = new Meta(
                "v1",
                "validate_flight_airports",
                "validate airports"
        );

        GraphContext ctx = new GraphContext();


        // IMPORTANT: factId == executionId == commitId
        String factId = "Flight:F100";

        ValidateTask task = new ValidateTask("validate_flight_airports", factId);

        QueryRequest<?> req =
                new QueryRequest<>(meta, ctx, task, factId);

        QueryExecution<?> exec =
                new QueryExecution<>(req);

        ScorerResult scorer = ScorerResult.ok("ok");

        HistoryRecord record =
                new HistoryRecord(exec, scorer);

        historyStore.addRecord(record);

        // -------------------------
        // Act
        // -------------------------

        PolicyGateResult result =
                policyGateOrchestrator.approve(
                        "validate_flight_airports",
                        factId
                );

        // -------------------------
        // Assert: PolicyGate surface
        // -------------------------
        Console.log("__processor_result____", result.toJsonString());
        assertTrue(result.isOk());

        // -------------------------
        // Assert: Commit truth surface
        // -------------------------

        CommitAuditView view = auditStore.getView(factId);
        assertNotNull(view);
        assertEquals(CommitAuditStatus.COMPLETED_ACCEPTED, view.getStatus());

        CommitAuditView viaService = auditService.getAudit(factId);
        assertNotNull(viaService);
        assertEquals(CommitAuditStatus.COMPLETED_ACCEPTED, viaService.getStatus());
    }

    @Test
    public void approve_when_history_missing_returns_not_ok_and_does_not_create_commit_audit() {

        String queryKind = "validate_flight_airports";
        String factId = "Flight:DOES_NOT_EXIST";

        PolicyGateResult result = policyGateOrchestrator.approve(queryKind, factId);

        assertFalse(result.isOk());

        CommitAuditView view = auditStore.getView(factId);
        assertNull(view);
    }

    @Test
    public void approve_when_query_kind_mismatch_returns_not_ok_and_does_not_create_commit_audit() {

        // seed under a different queryKind
        String seedKind = "validate_flight_airports";
        String approveKind = "validate_invoice_totals";

        String factId = "Flight:F100";

        Meta meta = new Meta("v1", seedKind, "validate airports");
        GraphContext ctx = new GraphContext();

        ValidateTask task = new ValidateTask(seedKind, factId);

        QueryRequest<?> req = new QueryRequest<>(meta, ctx, task, factId);
        QueryExecution<?> exec = new QueryExecution<>(req);

        historyStore.addRecord(new HistoryRecord(exec, ScorerResult.ok("ok")));

        // act: try to approve with wrong kind
        PolicyGateResult result = policyGateOrchestrator.approve(approveKind, factId);

        // assert
        assertFalse(result.isOk());

        CommitAuditView view = auditStore.getView(factId);
        assertNull(view);
    }

    @Test
    public void approve_is_idempotent_second_call_keeps_audit_status_completed_accepted() {

        // seed
        String queryKind = "validate_flight_airports";
        String factId = "Flight:F100";

        Meta meta = new Meta("v1", queryKind, "validate airports");
        GraphContext ctx = new GraphContext();

        ValidateTask task = new ValidateTask(queryKind, factId);

        QueryRequest<?> req = new QueryRequest<>(meta, ctx, task, factId);
        QueryExecution<?> exec = new QueryExecution<>(req);

        historyStore.addRecord(new HistoryRecord(exec, ScorerResult.ok("ok")));

        // act 1
        PolicyGateResult r1 = policyGateOrchestrator.approve(queryKind, factId);
        assertTrue(r1.isOk());

        CommitAuditView v1 = auditStore.getView(factId);
        assertNotNull(v1);
        assertEquals(CommitAuditStatus.COMPLETED_ACCEPTED, v1.getStatus());

        // act 2
        PolicyGateResult r2 = policyGateOrchestrator.approve(queryKind, factId);
        assertTrue(r2.isOk());

        CommitAuditView v2 = auditStore.getView(factId);
        assertNotNull(v2);
        assertEquals(CommitAuditStatus.COMPLETED_ACCEPTED, v2.getStatus());
    }

}


