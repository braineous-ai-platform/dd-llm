package io.braineous.dd.llm.query.services;


import ai.braineous.cgo.history.*;
import ai.braineous.rag.prompt.cgo.api.*;
import ai.braineous.rag.prompt.cgo.query.QueryRequest;

import com.google.gson.JsonObject;
import io.braineous.dd.llm.query.client.QueryOrchestrator;
import io.braineous.dd.llm.query.client.QueryResult;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

@QuarkusTest
public class QueryOrchestratorIT {

    @Test
    void query_orchestrator_to_mongo_history_end_to_end() {
        ai.braineous.rag.prompt.observe.Console.log("IT", "query_orchestrator_to_mongo_history_end_to_end");

        MongoHistoryStore store = new MongoHistoryStore();
        store.clear();
        ai.braineous.rag.prompt.observe.Console.log("IT", "mongo cleared");

        QueryOrchestrator orch = new QueryOrchestrator();

        Meta meta = new Meta("v1", "it_query_kind", "description");
        GraphContext ctx = new GraphContext(java.util.Map.of());

        String factId = "Flight:F100";
        ValidateTask task = new ValidateTask("validate flight airports", factId);
        QueryRequest req = new QueryRequest(meta, ctx, task);
        req.setAdapter(new FakeLlmAdapter());

        QueryResult result = orch.execute(req);
        ai.braineous.rag.prompt.observe.Console.log("IT", result.toJson());

        org.junit.jupiter.api.Assertions.assertTrue(result.isOk());
        org.junit.jupiter.api.Assertions.assertNotNull(result.getQueryExecutionJson());

        // Rehydrate QueryExecution from the returned JSON
        QueryExecution<?> exec = QueryExecution.fromJson(result.getQueryExecutionJson());
        org.junit.jupiter.api.Assertions.assertNotNull(exec);
        org.junit.jupiter.api.Assertions.assertNotNull(exec.getRequest());
        org.junit.jupiter.api.Assertions.assertNotNull(exec.getRequest().getMeta());
        org.junit.jupiter.api.Assertions.assertEquals("it_query_kind", exec.getRequest().getMeta().getQueryKind());

        // Create HistoryRecord and persist (THIS WAS MISSING)
        ScorerResult score = ScorerResult.ok("it smoke");
        HistoryRecord rec = new HistoryRecord(exec, score);
        rec.markPending(Instant.now());
        store.upsertPending(rec);

        // Verify via MongoHistoryStore query API
        HistoryView view = store.findHistory("it_query_kind");
        ai.braineous.rag.prompt.observe.Console.log("IT", view);

        List<HistoryRecord> all = store.getAll();
        ai.braineous.rag.prompt.observe.Console.log("IT", "all.size=" + all.size());

        org.junit.jupiter.api.Assertions.assertEquals(1, all.size());

        HistoryRecord r = all.get(0);

        // ---- convergence invariants ----
        org.junit.jupiter.api.Assertions.assertEquals("it_query_kind", r.getQueryKind());
        org.junit.jupiter.api.Assertions.assertNotNull(r.getQueryExecution());
        org.junit.jupiter.api.Assertions.assertNotNull(r.getQueryExecution().getRequest());
        org.junit.jupiter.api.Assertions.assertNotNull(r.getQueryExecution().getRequest().getMeta());
        org.junit.jupiter.api.Assertions.assertEquals(
                r.getQueryKind(),
                r.getQueryExecution().getRequest().getMeta().getQueryKind()
        );

        org.junit.jupiter.api.Assertions.assertEquals(HistoryStatus.PENDING, r.getStatus());
        org.junit.jupiter.api.Assertions.assertNotNull(r.getCreatedAt());
        org.junit.jupiter.api.Assertions.assertNotNull(r.getUpdatedAt());

        QueryExecution<?> ex = r.getQueryExecution();
        org.junit.jupiter.api.Assertions.assertNotNull(ex);
        org.junit.jupiter.api.Assertions.assertTrue(ex.isOk());
        org.junit.jupiter.api.Assertions.assertEquals("OK", ex.getStatus());

        ValidationResult vr = ex.getLlmResponseValidation();
        org.junit.jupiter.api.Assertions.assertNotNull(vr);
        org.junit.jupiter.api.Assertions.assertTrue(vr.isOk());
        org.junit.jupiter.api.Assertions.assertEquals("Flight:F100", vr.getAnchorId());
    }

    @Test
    void query_error_does_not_persist_history_record() {
        ai.braineous.rag.prompt.observe.Console.log("IT", "query_error_does_not_persist_history_record");

        MongoHistoryStore store = new MongoHistoryStore();
        store.clear();
        ai.braineous.rag.prompt.observe.Console.log("IT", "mongo cleared");

        QueryOrchestrator orch = new QueryOrchestrator();

        Meta meta = new Meta("v1", "it_query_error", "error path");
        GraphContext ctx = new GraphContext(java.util.Map.of());

        ValidateTask task = new ValidateTask("validate bad flight", "Flight:F404");
        QueryRequest req = new QueryRequest(meta, ctx, task);
        req.setAdapter(new ErrorLlmAdapter());

        QueryResult result = orch.execute(req);
        ai.braineous.rag.prompt.observe.Console.log("IT", result.toJson());

        QueryExecution<?> exec = QueryExecution.fromJson(result.getQueryExecutionJson());
        ai.braineous.rag.prompt.observe.Console.log("IT", exec.toJson());

        org.junit.jupiter.api.Assertions.assertNotNull(exec);
        org.junit.jupiter.api.Assertions.assertFalse(exec.isOk());
        org.junit.jupiter.api.Assertions.assertEquals("ERROR", exec.getStatus());

        HistoryView view = store.findHistory("it_query_error");
        ai.braineous.rag.prompt.observe.Console.log("IT", view);

        List<HistoryRecord> all = store.getAll();
        ai.braineous.rag.prompt.observe.Console.log("IT", "all.size=" + all.size());

        org.junit.jupiter.api.Assertions.assertEquals(0, all.size());
    }

    @Test
    void query_error_does_not_pollute_history_and_ok_still_persists() {
        ai.braineous.rag.prompt.observe.Console.log("IT", "query_error_does_not_pollute_history_and_ok_still_persists");

        MongoHistoryStore store = new MongoHistoryStore();
        store.clear();
        ai.braineous.rag.prompt.observe.Console.log("IT", "mongo cleared");

        QueryOrchestrator orch = new QueryOrchestrator();

        // 1) ERROR run
        Meta metaErr = new Meta("v1", "it_query_error", "error path");
        GraphContext ctx = new GraphContext(java.util.Map.of());
        ValidateTask taskErr = new ValidateTask("validate bad flight", "Flight:F404");
        QueryRequest reqErr = new QueryRequest(metaErr, ctx, taskErr);
        reqErr.setAdapter(new ErrorLlmAdapter());

        QueryResult resErr = orch.execute(reqErr);
        ai.braineous.rag.prompt.observe.Console.log("IT", resErr.toJson());

        QueryExecution<?> execErr = QueryExecution.fromJson(resErr.getQueryExecutionJson());
        org.junit.jupiter.api.Assertions.assertNotNull(execErr);
        org.junit.jupiter.api.Assertions.assertFalse(execErr.isOk());
        org.junit.jupiter.api.Assertions.assertEquals("ERROR", execErr.getStatus());

        // Assert history still empty
        org.junit.jupiter.api.Assertions.assertEquals(0, store.getAll().size());

        // 2) OK run (same test proves store still works after error)
        Meta metaOk = new Meta("v1", "it_query_kind", "ok path");
        GraphContext ctxOk = new GraphContext(java.util.Map.of());

        ValidateTask taskOk = new ValidateTask("validate flight airports", "Flight:F100");

        QueryRequest reqOk = new QueryRequest(metaOk, ctxOk, taskOk);
        reqOk.setAdapter(new OkLlmAdapter());

        QueryResult resOk = orch.execute(reqOk);
        ai.braineous.rag.prompt.observe.Console.log("IT", resOk.toJson());

        QueryExecution<?> execOk = QueryExecution.fromJson(resOk.getQueryExecutionJson());
        org.junit.jupiter.api.Assertions.assertNotNull(execOk);
        org.junit.jupiter.api.Assertions.assertTrue(execOk.isOk());
        org.junit.jupiter.api.Assertions.assertEquals("OK", execOk.getStatus());

        List<HistoryRecord> all = store.getAll();
        ai.braineous.rag.prompt.observe.Console.log("IT", "all.size=" + all.size());

        org.junit.jupiter.api.Assertions.assertEquals(1, all.size());
        org.junit.jupiter.api.Assertions.assertEquals("it_query_kind", all.get(0).getQueryKind());
    }

    @Test
    void query_ids_are_present_and_unique_per_execution() {
        ai.braineous.rag.prompt.observe.Console.log("IT", "query_ids_are_present_and_unique_per_execution");

        MongoHistoryStore store = new MongoHistoryStore();
        store.clear();
        ai.braineous.rag.prompt.observe.Console.log("IT", "mongo cleared");

        QueryOrchestrator orch = new QueryOrchestrator();

        GraphContext ctx = new GraphContext(java.util.Map.of());

        // ---- run 1 (OK)
        Meta meta1 = new Meta("v1", "it_query_id_1", "id run 1");
        ValidateTask task1 = new ValidateTask("validate flight airports", "Flight:F101");
        QueryRequest req1 = new QueryRequest(meta1, ctx, task1);
        req1.setAdapter(new OkLlmAdapter("Flight:F101"));

        QueryResult r1 = orch.execute(req1);
        ai.braineous.rag.prompt.observe.Console.log("IT", r1.toJson());

        org.junit.jupiter.api.Assertions.assertNotNull(r1.getId());
        org.junit.jupiter.api.Assertions.assertTrue(r1.getId().startsWith("DD-LLM-QUERY"));
        org.junit.jupiter.api.Assertions.assertNotNull(r1.getQueryExecutionJson());

        // ---- run 2 (OK)
        Meta meta2 = new Meta("v1", "it_query_id_2", "id run 2");
        ValidateTask task2 = new ValidateTask("validate flight airports", "Flight:F102");
        QueryRequest req2 = new QueryRequest(meta2, ctx, task2);
        req2.setAdapter(new OkLlmAdapter("Flight:F102"));

        QueryResult r2 = orch.execute(req2);
        ai.braineous.rag.prompt.observe.Console.log("IT", r2.toJson());

        org.junit.jupiter.api.Assertions.assertNotNull(r2.getId());
        org.junit.jupiter.api.Assertions.assertTrue(r2.getId().startsWith("DD-LLM-QUERY"));
        org.junit.jupiter.api.Assertions.assertNotNull(r2.getQueryExecutionJson());

        // ---- uniqueness
        org.junit.jupiter.api.Assertions.assertNotEquals(r1.getId(), r2.getId());

        // ---- persistence count (OK-only)
        List<HistoryRecord> all = store.getAll();
        ai.braineous.rag.prompt.observe.Console.log("IT", "all.size=" + all.size());
        org.junit.jupiter.api.Assertions.assertEquals(2, all.size());
    }

    @Test
    void history_findHistory_filters_by_queryKind_only() {
        ai.braineous.rag.prompt.observe.Console.log("IT", "history_findHistory_filters_by_queryKind_only");

        MongoHistoryStore store = new MongoHistoryStore();
        store.clear();
        ai.braineous.rag.prompt.observe.Console.log("IT", "mongo cleared");

        QueryOrchestrator orch = new QueryOrchestrator();
        GraphContext ctx = new GraphContext(java.util.Map.of());

        // insert OK record for qk=A
        Meta metaA = new Meta("v1", "it_qk_A", "qk A");
        ValidateTask taskA = new ValidateTask("validate A", "Flight:A1");
        QueryRequest reqA = new QueryRequest(metaA, ctx, taskA);
        reqA.setAdapter(new OkLlmAdapter("Flight:A1"));
        QueryResult rA = orch.execute(reqA);
        ai.braineous.rag.prompt.observe.Console.log("IT", rA.toJson());
        org.junit.jupiter.api.Assertions.assertTrue(rA.isOk());

        // insert OK record for qk=B
        Meta metaB = new Meta("v1", "it_qk_B", "qk B");
        ValidateTask taskB = new ValidateTask("validate B", "Flight:B1");
        QueryRequest reqB = new QueryRequest(metaB, ctx, taskB);
        reqB.setAdapter(new OkLlmAdapter("Flight:B1"));
        QueryResult rB = orch.execute(reqB);
        ai.braineous.rag.prompt.observe.Console.log("IT", rB.toJson());
        org.junit.jupiter.api.Assertions.assertTrue(rB.isOk());

        // Now filter
        HistoryView viewA = store.findHistory("it_qk_A");
        ai.braineous.rag.prompt.observe.Console.log("IT", "viewA=" + viewA);

        HistoryView viewB = store.findHistory("it_qk_B");
        ai.braineous.rag.prompt.observe.Console.log("IT", "viewB=" + viewB);

        List<HistoryRecord> all = store.getAll();
        ai.braineous.rag.prompt.observe.Console.log("IT", "all.size=" + all.size());
        org.junit.jupiter.api.Assertions.assertEquals(2, all.size());

        int countA = 0;
        int countB = 0;

        for (int i = 0; i < all.size(); i++) {
            HistoryRecord r = all.get(i);
            if ("it_qk_A".equals(r.getQueryKind())) {
                countA++;
            }
            if ("it_qk_B".equals(r.getQueryKind())) {
                countB++;
            }
        }

        org.junit.jupiter.api.Assertions.assertEquals(1, countA);
        org.junit.jupiter.api.Assertions.assertEquals(1, countB);
    }

    @Test
    void history_record_roundtrip_invariant_queryKind_matches_execution_request_meta() {
        ai.braineous.rag.prompt.observe.Console.log("IT", "history_record_roundtrip_invariant_queryKind_matches_execution_request_meta");

        MongoHistoryStore store = new MongoHistoryStore();
        store.clear();
        ai.braineous.rag.prompt.observe.Console.log("IT", "mongo cleared");

        QueryOrchestrator orch = new QueryOrchestrator();
        GraphContext ctx = new GraphContext(java.util.Map.of());

        String qk = "it_qk_roundtrip";
        Meta meta = new Meta("v1", qk, "roundtrip");
        ValidateTask task = new ValidateTask("validate roundtrip", "Flight:RT1");

        QueryRequest req = new QueryRequest(meta, ctx, task);
        req.setAdapter(new OkLlmAdapter("Flight:RT1"));

        QueryResult res = orch.execute(req);
        ai.braineous.rag.prompt.observe.Console.log("IT", res.toJson());
        org.junit.jupiter.api.Assertions.assertTrue(res.isOk());

        List<HistoryRecord> all = store.getAll();
        ai.braineous.rag.prompt.observe.Console.log("IT", "all.size=" + all.size());
        org.junit.jupiter.api.Assertions.assertEquals(1, all.size());

        HistoryRecord r = all.get(0);
        ai.braineous.rag.prompt.observe.Console.log("IT", r.toString());

        org.junit.jupiter.api.Assertions.assertEquals(qk, r.getQueryKind());

        QueryExecution<?> ex = r.getQueryExecution();
        org.junit.jupiter.api.Assertions.assertNotNull(ex);

        org.junit.jupiter.api.Assertions.assertNotNull(ex.getRequest());
        org.junit.jupiter.api.Assertions.assertNotNull(ex.getRequest().getMeta());
        org.junit.jupiter.api.Assertions.assertEquals(qk, ex.getRequest().getMeta().getQueryKind());

        org.junit.jupiter.api.Assertions.assertTrue(ex.isOk());
        org.junit.jupiter.api.Assertions.assertEquals("OK", ex.getStatus());

        ValidationResult vr = ex.getLlmResponseValidation();
        org.junit.jupiter.api.Assertions.assertNotNull(vr);
        org.junit.jupiter.api.Assertions.assertTrue(vr.isOk());
        org.junit.jupiter.api.Assertions.assertEquals("Flight:RT1", vr.getAnchorId());
    }

    @Test
    void history_getAll_order_is_not_assumed_membership_only() {
        ai.braineous.rag.prompt.observe.Console.log("IT", "history_getAll_order_is_not_assumed_membership_only");

        MongoHistoryStore store = new MongoHistoryStore();
        store.clear();
        ai.braineous.rag.prompt.observe.Console.log("IT", "mongo cleared");

        QueryOrchestrator orch = new QueryOrchestrator();
        GraphContext ctx = new GraphContext(java.util.Map.of());

        runOk(orch, ctx, "it_order_A", "Flight:OA");
        runOk(orch, ctx, "it_order_B", "Flight:OB");
        runOk(orch, ctx, "it_order_C", "Flight:OC");

        List<HistoryRecord> all = store.getAll();
        ai.braineous.rag.prompt.observe.Console.log("IT", "all.size=" + all.size());
        org.junit.jupiter.api.Assertions.assertEquals(3, all.size());

        boolean seenA = false;
        boolean seenB = false;
        boolean seenC = false;

        for (int i = 0; i < all.size(); i++) {
            String qk = all.get(i).getQueryKind();
            if ("it_order_A".equals(qk)) { seenA = true; }
            if ("it_order_B".equals(qk)) { seenB = true; }
            if ("it_order_C".equals(qk)) { seenC = true; }
        }

        org.junit.jupiter.api.Assertions.assertTrue(seenA);
        org.junit.jupiter.api.Assertions.assertTrue(seenB);
        org.junit.jupiter.api.Assertions.assertTrue(seenC);
    }

    @Test
    void null_or_blank_queryKind_is_not_indexed_for_findHistory() {
        ai.braineous.rag.prompt.observe.Console.log("IT", "null_or_blank_queryKind_is_not_indexed_for_findHistory");

        MongoHistoryStore store = new MongoHistoryStore();
        store.clear();
        ai.braineous.rag.prompt.observe.Console.log("IT", "mongo cleared");

        QueryOrchestrator orch = new QueryOrchestrator();
        GraphContext ctx = new GraphContext(java.util.Map.of());

        Meta metaNull = new Meta("v1", null, "null qk");
        ValidateTask taskNull = new ValidateTask("validate null qk", "Flight:NQK");
        QueryRequest reqNull = new QueryRequest(metaNull, ctx, taskNull);
        reqNull.setAdapter(new OkLlmAdapter("Flight:NQK"));

        QueryResult resNull = orch.execute(reqNull);
        ai.braineous.rag.prompt.observe.Console.log("IT", resNull.toJson());
        org.junit.jupiter.api.Assertions.assertTrue(resNull.isOk());

        Meta metaBlank = new Meta("v1", "   ", "blank qk");
        ValidateTask taskBlank = new ValidateTask("validate blank qk", "Flight:BQK");
        QueryRequest reqBlank = new QueryRequest(metaBlank, ctx, taskBlank);
        reqBlank.setAdapter(new OkLlmAdapter("Flight:BQK"));

        QueryResult resBlank = orch.execute(reqBlank);
        ai.braineous.rag.prompt.observe.Console.log("IT", resBlank.toJson());
        org.junit.jupiter.api.Assertions.assertTrue(resBlank.isOk());

        HistoryView view = store.findHistory("some_real_query_kind");
        ai.braineous.rag.prompt.observe.Console.log("IT", view);

        List<HistoryRecord> all = store.getAll();
        ai.braineous.rag.prompt.observe.Console.log("IT", "all.size=" + all.size());

        int hits = 0;
        for (int i = 0; i < all.size(); i++) {
            if ("some_real_query_kind".equals(all.get(i).getQueryKind())) {
                hits++;
            }
        }
        org.junit.jupiter.api.Assertions.assertEquals(0, hits);
    }

    @Test
    void duplicate_queryKind_accumulates_multiple_records() {
        ai.braineous.rag.prompt.observe.Console.log("IT", "duplicate_queryKind_accumulates_multiple_records");

        MongoHistoryStore store = new MongoHistoryStore();
        store.clear();
        ai.braineous.rag.prompt.observe.Console.log("IT", "mongo cleared");

        QueryOrchestrator orch = new QueryOrchestrator();
        GraphContext ctx = new GraphContext(java.util.Map.of());

        String qk = "it_qk_dup";

        Meta meta1 = new Meta("v1", qk, "dup 1");
        ValidateTask task1 = new ValidateTask("validate dup 1", "Flight:D1");
        QueryRequest req1 = new QueryRequest(meta1, ctx, task1);
        req1.setAdapter(new OkLlmAdapter("Flight:D1"));

        QueryResult r1 = orch.execute(req1);
        ai.braineous.rag.prompt.observe.Console.log("IT", r1.toJson());
        org.junit.jupiter.api.Assertions.assertTrue(r1.isOk());

        Meta meta2 = new Meta("v1", qk, "dup 2");
        ValidateTask task2 = new ValidateTask("validate dup 2", "Flight:D2");
        QueryRequest req2 = new QueryRequest(meta2, ctx, task2);
        req2.setAdapter(new OkLlmAdapter("Flight:D2"));

        QueryResult r2 = orch.execute(req2);
        ai.braineous.rag.prompt.observe.Console.log("IT", r2.toJson());
        org.junit.jupiter.api.Assertions.assertTrue(r2.isOk());

        HistoryView view = store.findHistory(qk);
        ai.braineous.rag.prompt.observe.Console.log("IT", view);

        List<HistoryRecord> all = store.getAll();
        ai.braineous.rag.prompt.observe.Console.log("IT", "all.size=" + all.size());
        org.junit.jupiter.api.Assertions.assertEquals(2, all.size());

        int hits = 0;
        for (int i = 0; i < all.size(); i++) {
            if (qk.equals(all.get(i).getQueryKind())) {
                hits++;
            }
        }
        org.junit.jupiter.api.Assertions.assertEquals(2, hits);
    }

    @Test
    void findHistory_trims_input_queryKind() {
        ai.braineous.rag.prompt.observe.Console.log("IT", "findHistory_trims_input_queryKind");

        MongoHistoryStore store = new MongoHistoryStore();
        store.clear();
        ai.braineous.rag.prompt.observe.Console.log("IT", "mongo cleared");

        QueryOrchestrator orch = new QueryOrchestrator();
        GraphContext ctx = new GraphContext(java.util.Map.of());

        String qk = "it_qk_trim";

        Meta meta1 = new Meta("v1", qk, "trim 1");
        ValidateTask task1 = new ValidateTask("validate trim 1", "Flight:T1");
        QueryRequest req1 = new QueryRequest(meta1, ctx, task1);
        req1.setAdapter(new OkLlmAdapter("Flight:T1"));
        QueryResult r1 = orch.execute(req1);
        ai.braineous.rag.prompt.observe.Console.log("IT", r1.toJson());
        org.junit.jupiter.api.Assertions.assertTrue(r1.isOk());

        Meta meta2 = new Meta("v1", qk, "trim 2");
        ValidateTask task2 = new ValidateTask("validate trim 2", "Flight:T2");
        QueryRequest req2 = new QueryRequest(meta2, ctx, task2);
        req2.setAdapter(new OkLlmAdapter("Flight:T2"));
        QueryResult r2 = orch.execute(req2);
        ai.braineous.rag.prompt.observe.Console.log("IT", r2.toJson());
        org.junit.jupiter.api.Assertions.assertTrue(r2.isOk());

        HistoryView exact = store.findHistory(qk);
        ai.braineous.rag.prompt.observe.Console.log("IT", "exact=" + exact);

        HistoryView spaced = store.findHistory("   " + qk + "   ");
        ai.braineous.rag.prompt.observe.Console.log("IT", "spaced=" + spaced);

        List<HistoryRecord> all = store.getAll();
        ai.braineous.rag.prompt.observe.Console.log("IT", "all.size=" + all.size());
        org.junit.jupiter.api.Assertions.assertEquals(2, all.size());

        int hits = 0;
        for (int i = 0; i < all.size(); i++) {
            if (qk.equals(all.get(i).getQueryKind())) {
                hits++;
            }
        }
        org.junit.jupiter.api.Assertions.assertEquals(2, hits);
    }

    @Test
    void store_clear_removes_all_records_and_indexes() {
        ai.braineous.rag.prompt.observe.Console.log("IT", "store_clear_removes_all_records_and_indexes");

        MongoHistoryStore store = new MongoHistoryStore();
        store.clear();
        ai.braineous.rag.prompt.observe.Console.log("IT", "mongo cleared (initial)");

        QueryOrchestrator orch = new QueryOrchestrator();
        GraphContext ctx = new GraphContext(java.util.Map.of());

        Meta meta1 = new Meta("v1", "it_clear_qk", "clear 1");
        ValidateTask task1 = new ValidateTask("validate clear 1", "Flight:C1");
        QueryRequest req1 = new QueryRequest(meta1, ctx, task1);
        req1.setAdapter(new OkLlmAdapter("Flight:C1"));
        QueryResult r1 = orch.execute(req1);
        ai.braineous.rag.prompt.observe.Console.log("IT", r1.toJson());
        org.junit.jupiter.api.Assertions.assertTrue(r1.isOk());

        Meta meta2 = new Meta("v1", "it_clear_qk", "clear 2");
        ValidateTask task2 = new ValidateTask("validate clear 2", "Flight:C2");
        QueryRequest req2 = new QueryRequest(meta2, ctx, task2);
        req2.setAdapter(new OkLlmAdapter("Flight:C2"));
        QueryResult r2 = orch.execute(req2);
        ai.braineous.rag.prompt.observe.Console.log("IT", r2.toJson());
        org.junit.jupiter.api.Assertions.assertTrue(r2.isOk());

        List<HistoryRecord> before = store.getAll();
        ai.braineous.rag.prompt.observe.Console.log("IT", "before.size=" + before.size());
        org.junit.jupiter.api.Assertions.assertEquals(2, before.size());

        store.clear();
        ai.braineous.rag.prompt.observe.Console.log("IT", "mongo cleared (after)");

        List<HistoryRecord> after = store.getAll();
        ai.braineous.rag.prompt.observe.Console.log("IT", "after.size=" + after.size());
        org.junit.jupiter.api.Assertions.assertEquals(0, after.size());

        HistoryView view = store.findHistory("it_clear_qk");
        ai.braineous.rag.prompt.observe.Console.log("IT", view);
        org.junit.jupiter.api.Assertions.assertEquals(0, store.getAll().size());
    }

    @Test
    void rehydrated_execution_has_null_adapter_and_still_roundtrips() {
        ai.braineous.rag.prompt.observe.Console.log("IT", "rehydrated_execution_has_null_adapter_and_still_roundtrips");

        MongoHistoryStore store = new MongoHistoryStore();
        store.clear();
        ai.braineous.rag.prompt.observe.Console.log("IT", "mongo cleared");

        QueryOrchestrator orch = new QueryOrchestrator();

        Meta meta = new Meta("v1", "it_adapter_null", "adapter null rehydrate");
        GraphContext ctx = new GraphContext(java.util.Map.of());
        ValidateTask task = new ValidateTask("validate adapter null", "Flight:AN1");

        QueryRequest req = new QueryRequest(meta, ctx, task);
        req.setAdapter(new OkLlmAdapter("Flight:AN1"));

        QueryResult res = orch.execute(req);
        ai.braineous.rag.prompt.observe.Console.log("IT", res.toJson());
        org.junit.jupiter.api.Assertions.assertTrue(res.isOk());
        org.junit.jupiter.api.Assertions.assertNotNull(res.getQueryExecutionJson());

        QueryExecution<?> exec = QueryExecution.fromJson(res.getQueryExecutionJson());
        ai.braineous.rag.prompt.observe.Console.log("IT", exec.toJson());

        org.junit.jupiter.api.Assertions.assertNotNull(exec);
        org.junit.jupiter.api.Assertions.assertNotNull(exec.getRequest());

        org.junit.jupiter.api.Assertions.assertNull(exec.getRequest().getAdapter());

        org.junit.jupiter.api.Assertions.assertTrue(exec.isOk());
        org.junit.jupiter.api.Assertions.assertEquals("OK", exec.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals("it_adapter_null", exec.getRequest().getMeta().getQueryKind());

        List<HistoryRecord> all = store.getAll();
        ai.braineous.rag.prompt.observe.Console.log("IT", "all.size=" + all.size());
        org.junit.jupiter.api.Assertions.assertEquals(1, all.size());

        HistoryRecord r = all.get(0);
        org.junit.jupiter.api.Assertions.assertEquals("it_adapter_null", r.getQueryKind());
        org.junit.jupiter.api.Assertions.assertNotNull(r.getQueryExecution());
        org.junit.jupiter.api.Assertions.assertNull(r.getQueryExecution().getRequest().getAdapter());
    }

    @Test
    void rawResponse_is_persisted_exactly_as_returned_by_adapter() {
        ai.braineous.rag.prompt.observe.Console.log("IT", "rawResponse_is_persisted_exactly_as_returned_by_adapter");

        MongoHistoryStore store = new MongoHistoryStore();
        store.clear();
        ai.braineous.rag.prompt.observe.Console.log("IT", "mongo cleared");

        QueryOrchestrator orch = new QueryOrchestrator();

        String qk = "it_raw_response";
        String anchorId = "Flight:RR1";
        String expectedRaw = "{\"result\":{\"ok\":true,\"code\":\"response.contract.ok\",\"message\":\"ok\",\"stage\":\"llm_response_validation\",\"anchorId\":\""
                + anchorId
                + "\",\"metadata\":{}}}";

        Meta meta = new Meta("v1", qk, "raw response invariant");
        GraphContext ctx = new GraphContext(java.util.Map.of());
        ValidateTask task = new ValidateTask("validate raw response", anchorId);

        QueryRequest req = new QueryRequest(meta, ctx, task);
        req.setAdapter(new FixedRawAdapter(expectedRaw));

        QueryResult res = orch.execute(req);
        ai.braineous.rag.prompt.observe.Console.log("IT", res.toJson());
        org.junit.jupiter.api.Assertions.assertTrue(res.isOk());

        List<HistoryRecord> all = store.getAll();
        ai.braineous.rag.prompt.observe.Console.log("IT", "all.size=" + all.size());
        org.junit.jupiter.api.Assertions.assertEquals(1, all.size());

        HistoryRecord r = all.get(0);
        QueryExecution<?> ex = r.getQueryExecution();
        org.junit.jupiter.api.Assertions.assertNotNull(ex);

        ai.braineous.rag.prompt.observe.Console.log("IT", "stored.rawResponse=" + ex.getRawResponse());
        org.junit.jupiter.api.Assertions.assertEquals(expectedRaw, ex.getRawResponse());
    }

    @Test
    void execution_stage_and_validation_stages_are_present_and_consistent() {
        ai.braineous.rag.prompt.observe.Console.log("IT", "execution_stage_and_validation_stages_are_present_and_consistent");

        MongoHistoryStore store = new MongoHistoryStore();
        store.clear();
        ai.braineous.rag.prompt.observe.Console.log("IT", "mongo cleared");

        QueryOrchestrator orch = new QueryOrchestrator();

        Meta meta = new Meta("v1", "it_stage_invariants", "stage invariants");
        GraphContext ctx = new GraphContext(java.util.Map.of());
        ValidateTask task = new ValidateTask("validate stages", "Flight:S1");

        QueryRequest req = new QueryRequest(meta, ctx, task);
        req.setAdapter(new OkLlmAdapter("Flight:S1"));

        QueryResult res = orch.execute(req);
        ai.braineous.rag.prompt.observe.Console.log("IT", res.toJson());
        org.junit.jupiter.api.Assertions.assertTrue(res.isOk());

        List<HistoryRecord> all = store.getAll();
        ai.braineous.rag.prompt.observe.Console.log("IT", "all.size=" + all.size());
        org.junit.jupiter.api.Assertions.assertEquals(1, all.size());

        QueryExecution<?> ex = all.get(0).getQueryExecution();
        ai.braineous.rag.prompt.observe.Console.log("IT", ex.toJson());

        org.junit.jupiter.api.Assertions.assertNotNull(ex.getStage());
        org.junit.jupiter.api.Assertions.assertFalse(ex.getStage().isBlank());

        ValidationResult pv = ex.getPromptValidation();
        org.junit.jupiter.api.Assertions.assertNotNull(pv);
        org.junit.jupiter.api.Assertions.assertNotNull(pv.getStage());
        org.junit.jupiter.api.Assertions.assertFalse(pv.getStage().isBlank());

        ValidationResult lv = ex.getLlmResponseValidation();
        org.junit.jupiter.api.Assertions.assertNotNull(lv);
        org.junit.jupiter.api.Assertions.assertNotNull(lv.getStage());
        org.junit.jupiter.api.Assertions.assertFalse(lv.getStage().isBlank());

        org.junit.jupiter.api.Assertions.assertTrue(pv.isOk());
        org.junit.jupiter.api.Assertions.assertTrue(lv.isOk());

        String execStage = ex.getStage().toLowerCase();
        org.junit.jupiter.api.Assertions.assertTrue(execStage.contains("ok"));
    }

    @Test
    void loop_smoke_10_executions_all_persisted() {
        ai.braineous.rag.prompt.observe.Console.log("IT", "loop_smoke_10_executions_all_persisted");

        MongoHistoryStore store = new MongoHistoryStore();
        store.clear();
        ai.braineous.rag.prompt.observe.Console.log("IT", "mongo cleared");

        QueryOrchestrator orch = new QueryOrchestrator();
        GraphContext ctx = new GraphContext(java.util.Map.of());

        int n = 10;
        for (int i = 0; i < n; i++) {
            String qk = "it_loop_" + i;
            String anchorId = "Flight:L" + i;

            Meta meta = new Meta("v1", qk, "loop " + i);
            ValidateTask task = new ValidateTask("validate loop " + i, anchorId);

            QueryRequest req = new QueryRequest(meta, ctx, task);
            req.setAdapter(new OkLlmAdapter(anchorId));

            QueryResult res = orch.execute(req);
            ai.braineous.rag.prompt.observe.Console.log("IT", "i=" + i + " id=" + res.getId() + " ok=" + res.isOk());

            org.junit.jupiter.api.Assertions.assertTrue(res.isOk());
            org.junit.jupiter.api.Assertions.assertNotNull(res.getId());
            org.junit.jupiter.api.Assertions.assertTrue(res.getId().startsWith("DD-LLM-QUERY"));
        }

        List<HistoryRecord> all = store.getAll();
        ai.braineous.rag.prompt.observe.Console.log("IT", "all.size=" + all.size());
        org.junit.jupiter.api.Assertions.assertEquals(n, all.size());

        int okCount = 0;
        for (int i = 0; i < all.size(); i++) {
            HistoryRecord r = all.get(i);
            if (r != null && r.getQueryExecution() != null && r.getQueryExecution().isOk()) {
                okCount++;
            }
        }
        org.junit.jupiter.api.Assertions.assertEquals(n, okCount);
    }

    @Test
    void same_queryKind_10_times_accumulates_no_overwrite() {
        ai.braineous.rag.prompt.observe.Console.log("IT", "same_queryKind_10_times_accumulates_no_overwrite");

        MongoHistoryStore store = new MongoHistoryStore();
        store.clear();
        ai.braineous.rag.prompt.observe.Console.log("IT", "mongo cleared");

        QueryOrchestrator orch = new QueryOrchestrator();
        GraphContext ctx = new GraphContext(java.util.Map.of());

        String qk = "it_same_qk_10";
        int n = 10;

        for (int i = 0; i < n; i++) {
            String anchorId = "Flight:SQ" + i;

            Meta meta = new Meta("v1", qk, "same qk " + i);
            ValidateTask task = new ValidateTask("validate same qk " + i, anchorId);

            QueryRequest req = new QueryRequest(meta, ctx, task);
            req.setAdapter(new OkLlmAdapter(anchorId));

            QueryResult res = orch.execute(req);
            ai.braineous.rag.prompt.observe.Console.log("IT", "i=" + i + " id=" + res.getId() + " ok=" + res.isOk());
            org.junit.jupiter.api.Assertions.assertTrue(res.isOk());
        }

        List<HistoryRecord> all = store.getAll();
        ai.braineous.rag.prompt.observe.Console.log("IT", "all.size=" + all.size());
        org.junit.jupiter.api.Assertions.assertEquals(n, all.size());

        int hits = 0;
        for (int i = 0; i < all.size(); i++) {
            if (qk.equals(all.get(i).getQueryKind())) {
                hits++;
            }
        }
        org.junit.jupiter.api.Assertions.assertEquals(n, hits);

        HistoryView view = store.findHistory(qk);
        ai.braineous.rag.prompt.observe.Console.log("IT", "view=" + view);
    }

    @Test
    void repeated_errors_do_not_persist_any_history_records() {
        ai.braineous.rag.prompt.observe.Console.log("IT", "repeated_errors_do_not_persist_any_history_records");

        MongoHistoryStore store = new MongoHistoryStore();
        store.clear();
        ai.braineous.rag.prompt.observe.Console.log("IT", "mongo cleared");

        QueryOrchestrator orch = new QueryOrchestrator();
        GraphContext ctx = new GraphContext(java.util.Map.of());

        int n = 10;
        for (int i = 0; i < n; i++) {
            String qk = "it_err_loop_" + i;

            Meta meta = new Meta("v1", qk, "err loop " + i);
            ValidateTask task = new ValidateTask("validate bad flight " + i, "Flight:E" + i);

            QueryRequest req = new QueryRequest(meta, ctx, task);
            req.setAdapter(new ErrorLlmAdapter());

            QueryResult res = orch.execute(req);
            ai.braineous.rag.prompt.observe.Console.log("IT", "i=" + i + " id=" + res.getId());

            org.junit.jupiter.api.Assertions.assertTrue(res.isOk());

            QueryExecution<?> ex = QueryExecution.fromJson(res.getQueryExecutionJson());
            org.junit.jupiter.api.Assertions.assertNotNull(ex);
            org.junit.jupiter.api.Assertions.assertFalse(ex.isOk());
            org.junit.jupiter.api.Assertions.assertEquals("ERROR", ex.getStatus());
        }

        List<HistoryRecord> all = store.getAll();
        ai.braineous.rag.prompt.observe.Console.log("IT", "all.size=" + all.size());
        org.junit.jupiter.api.Assertions.assertEquals(0, all.size());
    }

    @Test
    void interleaved_error_and_ok_only_ok_persists_count_exact() {
        ai.braineous.rag.prompt.observe.Console.log("IT", "interleaved_error_and_ok_only_ok_persists_count_exact");

        MongoHistoryStore store = new MongoHistoryStore();
        store.clear();
        ai.braineous.rag.prompt.observe.Console.log("IT", "mongo cleared");

        QueryOrchestrator orch = new QueryOrchestrator();
        GraphContext ctx = new GraphContext(java.util.Map.of());

        int okCount = 0;

        for (int i = 0; i < 10; i++) {
            boolean doOk = (i % 2 == 0);

            String qk = doOk ? ("it_mix_ok_" + i) : ("it_mix_err_" + i);
            String anchorId = doOk ? ("Flight:MO" + i) : ("Flight:ME" + i);

            Meta meta = new Meta("v1", qk, "mix " + i);
            ValidateTask task = new ValidateTask("validate mix " + i, anchorId);

            QueryRequest req = new QueryRequest(meta, ctx, task);
            if (doOk) {
                req.setAdapter(new OkLlmAdapter(anchorId));
                okCount++;
            } else {
                req.setAdapter(new ErrorLlmAdapter());
            }

            QueryResult res = orch.execute(req);
            ai.braineous.rag.prompt.observe.Console.log("IT", "i=" + i + " doOk=" + doOk + " id=" + res.getId());

            org.junit.jupiter.api.Assertions.assertTrue(res.isOk());

            QueryExecution<?> ex = QueryExecution.fromJson(res.getQueryExecutionJson());
            org.junit.jupiter.api.Assertions.assertNotNull(ex);

            if (doOk) {
                org.junit.jupiter.api.Assertions.assertTrue(ex.isOk());
                org.junit.jupiter.api.Assertions.assertEquals("OK", ex.getStatus());
            } else {
                org.junit.jupiter.api.Assertions.assertFalse(ex.isOk());
                org.junit.jupiter.api.Assertions.assertEquals("ERROR", ex.getStatus());
            }
        }

        List<HistoryRecord> all = store.getAll();
        ai.braineous.rag.prompt.observe.Console.log("IT", "all.size=" + all.size());
        org.junit.jupiter.api.Assertions.assertEquals(okCount, all.size());

        for (int i = 0; i < all.size(); i++) {
            QueryExecution<?> ex = all.get(i).getQueryExecution();
            org.junit.jupiter.api.Assertions.assertNotNull(ex);
            org.junit.jupiter.api.Assertions.assertTrue(ex.isOk());
            org.junit.jupiter.api.Assertions.assertEquals("OK", ex.getStatus());
        }
    }

    //-------------------------------------------------
    private static class FixedRawAdapter extends LlmAdapter {

        private final String raw;

        private FixedRawAdapter(String raw) {
            this.raw = raw;
        }

        @Override
        public String invokeLlm(JsonObject prompt) {
            return raw;
        }
    }

    private void runOk(QueryOrchestrator orch, GraphContext ctx, String queryKind, String anchorId) {
        ai.braineous.rag.prompt.observe.Console.log("IT", "runOk queryKind=" + queryKind + " anchorId=" + anchorId);

        Meta meta = new Meta("v1", queryKind, "order test");
        ValidateTask task = new ValidateTask("validate " + queryKind, anchorId);

        QueryRequest req = new QueryRequest(meta, ctx, task);
        req.setAdapter(new OkLlmAdapter(anchorId));

        QueryResult res = orch.execute(req);
        ai.braineous.rag.prompt.observe.Console.log("IT", res.toJson());

        org.junit.jupiter.api.Assertions.assertTrue(res.isOk());
    }

    private static class FakeLlmAdapter extends LlmAdapter {

        @Override
        public String invokeLlm(JsonObject prompt) {
            return "{\"result\":{\"ok\":true,\"code\":\"response.contract.ok\",\"message\":\"ok\",\"stage\":\"llm_response_validation\",\"anchorId\":\"Flight:F100\",\"metadata\":{}}}";
        }
    }

    private static class ErrorLlmAdapter extends LlmAdapter {
        @Override
        public String invokeLlm(JsonObject prompt) {
            return "{\"garbage\":true}";
        }
    }

    private static class OkLlmAdapter extends LlmAdapter {

        private final String anchorId;

        private OkLlmAdapter() {
            this.anchorId = "Flight:F100";
        }

        private OkLlmAdapter(String anchorId) {
            this.anchorId = anchorId;
        }

        @Override
        public String invokeLlm(JsonObject prompt) {
            return "{\"result\":{\"ok\":true,\"code\":\"response.contract.ok\",\"message\":\"ok\",\"stage\":\"llm_response_validation\",\"anchorId\":\""
                    + anchorId
                    + "\",\"metadata\":{}}}";
        }
    }
}

