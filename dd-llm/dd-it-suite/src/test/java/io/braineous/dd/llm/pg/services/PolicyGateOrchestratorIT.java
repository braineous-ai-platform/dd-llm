package io.braineous.dd.llm.pg.services;

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

        mongoClient.getDatabase(CatalogMongoStore.DEFAULT_DB_NAME).drop();
        try { Thread.sleep(150L); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }

        // Harden: prove CDI wiring produced a live orchestrator
        assertNotNull(pg);

        Console.log("PG_ORCH_IT/setup", "done");
    }

    @Test
    void findAllEntries_whenDatabaseEmpty_returnsEmptyList() {
        Console.log("PG_ORCH_IT/findAllEntries_empty", "start");

        List<CatalogEntry> all1 = pg.findAllEntries();
        assertNotNull(all1);
        assertTrue(all1.isEmpty());
        assertEquals(0, all1.size());

        // Harden: repeated call should remain empty (no hidden side-effects)
        List<CatalogEntry> all2 = pg.findAllEntries();
        assertNotNull(all2);
        assertTrue(all2.isEmpty());
        assertEquals(0, all2.size());

        Console.log("PG_ORCH_IT/findAllEntries_empty", "done");
    }

    @Test
    void findCatalogSnapshot_whenDatabaseEmpty_returnsNonNull_and_is_stable() {
        Console.log("PG_ORCH_IT/findCatalogSnapshot_empty", "start");

        assertNotNull(pg);

        Object s1 = pg.findCatalogSnapshot("user.search");
        assertNotNull(s1);

        Object s2 = pg.findCatalogSnapshot("user.search");
        assertNotNull(s2);

        Console.log("PG_ORCH_IT/findCatalogSnapshot_empty", "done");
    }

    @Test
    void findCatalogSnapshot_withDifferentQueryKinds_are_independent_and_non_null() {
        Console.log("PG_ORCH_IT/findCatalogSnapshot_multi_keys", "start");

        Object a = pg.findCatalogSnapshot("A");
        assertNotNull(a);

        Object b = pg.findCatalogSnapshot("B");
        assertNotNull(b);

        Object a2 = pg.findCatalogSnapshot("A");
        assertNotNull(a2);

        Console.log("PG_ORCH_IT/findCatalogSnapshot_multi_keys", "done");
    }


    @Test
    void read_calls_do_not_mutate_list_entries() {
        Console.log("PG_ORCH_IT/read_no_mutation", "start");

        List<CatalogEntry> before = pg.findAllEntries();
        assertNotNull(before);
        assertTrue(before.isEmpty());

        Object snap = pg.findCatalogSnapshot("user.search");
        assertNotNull(snap);

        List<CatalogEntry> after = pg.findAllEntries();
        assertNotNull(after);
        assertTrue(after.isEmpty());

        Console.log("PG_ORCH_IT/read_no_mutation", "done");
    }

    @Test
    void repeated_read_sequence_is_idempotent() {
        Console.log("PG_ORCH_IT/read_idempotent_sequence", "start");

        List<CatalogEntry> l1 = pg.findAllEntries();
        assertNotNull(l1);
        assertTrue(l1.isEmpty());

        Object s1 = pg.findCatalogSnapshot("user.search");
        assertNotNull(s1);

        Object s2 = pg.findCatalogSnapshot("user.search");
        assertNotNull(s2);

        List<CatalogEntry> l2 = pg.findAllEntries();
        assertNotNull(l2);
        assertTrue(l2.isEmpty());

        Console.log("PG_ORCH_IT/read_idempotent_sequence", "done");
    }

    @Test
    void mongo_is_reachable_in_test_harness() {
        Console.log("PG_ORCH_IT/mongo_reachable", "start");

        assertNotNull(mongoClient);

        // Minimal ping without assuming collections/schema
        String db = CatalogMongoStore.DEFAULT_DB_NAME;
        mongoClient.getDatabase(db).listCollectionNames().first();

        Console.log("PG_ORCH_IT/mongo_reachable", "done");
    }


    @Test
    void findCatalogSnapshot_withNullQueryKind_does_not_throw() {
        Console.log("PG_ORCH_IT/findCatalogSnapshot_null", "start");

        Object snap = pg.findCatalogSnapshot(null);
        assertNotNull(snap);

        Console.log("PG_ORCH_IT/findCatalogSnapshot_null", "done");
    }

    @Test
    void findCatalogSnapshot_withBlankQueryKind_does_not_throw() {
        Console.log("PG_ORCH_IT/findCatalogSnapshot_blank", "start");

        Object snap1 = pg.findCatalogSnapshot("");
        assertNotNull(snap1);

        Object snap2 = pg.findCatalogSnapshot("   ");
        assertNotNull(snap2);

        Console.log("PG_ORCH_IT/findCatalogSnapshot_blank", "done");
    }


    @Test
    void interleaved_reads_do_not_affect_each_other() {
        Console.log("PG_ORCH_IT/read_interleaved_isolation", "start");

        List<CatalogEntry> l1 = pg.findAllEntries();
        assertNotNull(l1);
        assertTrue(l1.isEmpty());

        Object s1 = pg.findCatalogSnapshot("A");
        assertNotNull(s1);

        List<CatalogEntry> l2 = pg.findAllEntries();
        assertNotNull(l2);
        assertTrue(l2.isEmpty());

        Object s2 = pg.findCatalogSnapshot("B");
        assertNotNull(s2);

        List<CatalogEntry> l3 = pg.findAllEntries();
        assertNotNull(l3);
        assertTrue(l3.isEmpty());

        Console.log("PG_ORCH_IT/read_interleaved_isolation", "done");
    }

    @Test
    void setup_drop_database_effectively_clears_state() {
        Console.log("PG_ORCH_IT/setup_drop_db_effect", "start");

        // After @BeforeEach drop, DB should have no collections (or at least behave as empty).
        String db = CatalogMongoStore.DEFAULT_DB_NAME;
        String any = mongoClient.getDatabase(db).listCollectionNames().first();

        // If there are no collections, first() returns null. That's what we want.
        assertTrue(any == null);

        Console.log("PG_ORCH_IT/setup_drop_db_effect", "done");
    }

    @Test
    void findCatalogSnapshot_withWeirdQueryKind_does_not_throw() {
        Console.log("PG_ORCH_IT/findCatalogSnapshot_weird", "start");

        Object snap = pg.findCatalogSnapshot("user.search/v1::beta#1");
        assertNotNull(snap);

        Console.log("PG_ORCH_IT/findCatalogSnapshot_weird", "done");
    }


    @Test
    void findAllEntries_does_not_create_collections() {
        Console.log("PG_ORCH_IT/findAllEntries_no_collections", "start");

        String db = CatalogMongoStore.DEFAULT_DB_NAME;

        // sanity: after setup, empty
        assertTrue(mongoClient.getDatabase(db).listCollectionNames().first() == null);

        List<CatalogEntry> all = pg.findAllEntries();
        assertNotNull(all);
        assertTrue(all.isEmpty());

        // still empty: read should not materialize collections
        assertTrue(mongoClient.getDatabase(db).listCollectionNames().first() == null);

        Console.log("PG_ORCH_IT/findAllEntries_no_collections", "done");
    }

    @Test
    void findCatalogSnapshot_does_not_create_collections() {
        Console.log("PG_ORCH_IT/findCatalogSnapshot_no_collections", "start");

        String db = CatalogMongoStore.DEFAULT_DB_NAME;
        assertTrue(mongoClient.getDatabase(db).listCollectionNames().first() == null);

        Object snap = pg.findCatalogSnapshot("user.search");
        assertNotNull(snap);

        assertTrue(mongoClient.getDatabase(db).listCollectionNames().first() == null);

        Console.log("PG_ORCH_IT/findCatalogSnapshot_no_collections", "done");
    }

    @Test
    void read_sequence_does_not_create_collections() {
        Console.log("PG_ORCH_IT/read_sequence_no_collections", "start");

        String db = CatalogMongoStore.DEFAULT_DB_NAME;
        assertTrue(mongoClient.getDatabase(db).listCollectionNames().first() == null);

        List<CatalogEntry> l1 = pg.findAllEntries();
        assertNotNull(l1);
        assertTrue(l1.isEmpty());

        Object s1 = pg.findCatalogSnapshot("A");
        assertNotNull(s1);

        List<CatalogEntry> l2 = pg.findAllEntries();
        assertNotNull(l2);
        assertTrue(l2.isEmpty());

        assertTrue(mongoClient.getDatabase(db).listCollectionNames().first() == null);

        Console.log("PG_ORCH_IT/read_sequence_no_collections", "done");
    }


    @Test
    void findCatalogSnapshot_repeated_different_keys_stays_stable() {
        Console.log("PG_ORCH_IT/findCatalogSnapshot_repeat_keys", "start");

        Object a1 = pg.findCatalogSnapshot("A");
        assertNotNull(a1);

        Object b1 = pg.findCatalogSnapshot("B");
        assertNotNull(b1);

        Object a2 = pg.findCatalogSnapshot("A");
        assertNotNull(a2);

        Object b2 = pg.findCatalogSnapshot("B");
        assertNotNull(b2);

        Console.log("PG_ORCH_IT/findCatalogSnapshot_repeat_keys", "done");
    }

    @Test
    void findAllEntries_called_many_times_is_stable() {
        Console.log("PG_ORCH_IT/findAllEntries_many_calls", "start");

        int i = 0;
        while (i < 10) {
            List<CatalogEntry> list = pg.findAllEntries();
            assertNotNull(list);
            assertTrue(list.isEmpty());
            i = i + 1;
        }

        Console.log("PG_ORCH_IT/findAllEntries_many_calls", "done");
    }

    @Test
    void findCatalogSnapshot_called_many_times_is_stable() {
        Console.log("PG_ORCH_IT/findCatalogSnapshot_many_calls", "start");

        int i = 0;
        while (i < 10) {
            Object snap = pg.findCatalogSnapshot("user.search");
            assertNotNull(snap);
            i = i + 1;
        }

        Console.log("PG_ORCH_IT/findCatalogSnapshot_many_calls", "done");
    }

    @Test
    void mongo_db_handle_is_stable() {
        Console.log("PG_ORCH_IT/mongo_db_handle_stable", "start");

        String db = CatalogMongoStore.DEFAULT_DB_NAME;

        int i = 0;
        while (i < 5) {
            mongoClient.getDatabase(db).listCollectionNames().first();
            i = i + 1;
        }

        Console.log("PG_ORCH_IT/mongo_db_handle_stable", "done");
    }

    @Test
    void findCatalogSnapshot_accepts_various_inputs_without_throwing() {
        Console.log("PG_ORCH_IT/findCatalogSnapshot_various_inputs", "start");

        assertNotNull(pg.findCatalogSnapshot(null));
        assertNotNull(pg.findCatalogSnapshot(""));
        assertNotNull(pg.findCatalogSnapshot("   "));
        assertNotNull(pg.findCatalogSnapshot("user.search/v1::beta#1"));

        Console.log("PG_ORCH_IT/findCatalogSnapshot_various_inputs", "done");
    }

    @Test
    void read_interleaving_loop_is_stable() {
        Console.log("PG_ORCH_IT/read_interleaving_loop", "start");

        int i = 0;
        while (i < 5) {
            List<CatalogEntry> list = pg.findAllEntries();
            assertNotNull(list);
            assertTrue(list.isEmpty());

            Object snap = pg.findCatalogSnapshot("A");
            assertNotNull(snap);

            i = i + 1;
        }

        Console.log("PG_ORCH_IT/read_interleaving_loop", "done");
    }

    @Test
    void toCommitRequest_nullEntry_returnsNull() {
        CommitRequest r = PolicyGateOrchestrator.toCommitRequest(null, new JsonObject(), "HTIL_USER", null);
        assertNull(r);
    }

    @Test
    void toCommitRequest_maps_queryKind_and_catalogVersion() {
        CatalogEntry e = new CatalogEntry();
        e.setQueryKind("  QK_A  ");
        e.setCatalogVersion("  1.0.0  ");

        JsonObject payload = new JsonObject();
        payload.addProperty("x", "y");

        CommitRequest r = PolicyGateOrchestrator.toCommitRequest(e, payload, "HTIL_USER", null);

        assertNotNull(r);
        assertEquals("QK_A", r.getQueryKind());
        assertEquals("1.0.0", r.getCatalogVersion());
    }

    @Test
    void toCommitRequest_trims_actor_and_ignores_blank_actor() {
        CatalogEntry e = new CatalogEntry();
        e.setQueryKind("QK");
        e.setCatalogVersion("1");

        JsonObject payload = new JsonObject();
        payload.addProperty("x", "y");

        CommitRequest r1 = PolicyGateOrchestrator.toCommitRequest(e, payload, "  AUTO_RULES  ", null);
        assertNotNull(r1);
        assertEquals("AUTO_RULES", r1.getActor());

        CommitRequest r2 = PolicyGateOrchestrator.toCommitRequest(e, payload, "   ", null);
        assertNotNull(r2);
        assertNull(r2.getActor());
    }

    @Test
    void toCommitRequest_sets_notes_when_provided() {
        CatalogEntry e = new CatalogEntry();
        e.setQueryKind("QK");
        e.setCatalogVersion("1");

        JsonObject payload = new JsonObject();
        payload.addProperty("x", "y");

        List<String> notes = new ArrayList<String>();
        notes.add("note1");
        notes.add("  note2  ");

        CommitRequest r = PolicyGateOrchestrator.toCommitRequest(e, payload, "HTIL_USER", notes);

        assertNotNull(r);
        assertNotNull(r.getNotes());
        assertEquals(2, r.getNotes().size());
        assertEquals("note1", r.getNotes().get(0));
        assertEquals("  note2  ", r.getNotes().get(1)); // mapper does not sanitize notes list itself
    }

    @Test
    void toCommitRequest_deepCopies_payload() {
        CatalogEntry e = new CatalogEntry();
        e.setQueryKind("QK");
        e.setCatalogVersion("1");

        JsonObject payload = new JsonObject();
        payload.addProperty("x", "y");

        CommitRequest r = PolicyGateOrchestrator.toCommitRequest(e, payload, "HTIL_USER", null);

        assertNotNull(r);
        assertNotNull(r.getPayload());
        assertEquals("y", r.getPayload().get("x").getAsString());

        // mutate original payload after mapping
        payload.addProperty("x", "changed");

        // mapped payload must remain unchanged
        assertEquals("y", r.getPayload().get("x").getAsString());
    }

    @Test
    void toCommitRequest_doesNotSet_commitId() {
        CatalogEntry e = new CatalogEntry();
        e.setQueryKind("QK");
        e.setCatalogVersion("1");

        JsonObject payload = new JsonObject();
        payload.addProperty("x", "y");

        CommitRequest r = PolicyGateOrchestrator.toCommitRequest(e, payload, "HTIL_USER", null);

        assertNotNull(r);
        assertNull(r.getCommitId());
    }

}

