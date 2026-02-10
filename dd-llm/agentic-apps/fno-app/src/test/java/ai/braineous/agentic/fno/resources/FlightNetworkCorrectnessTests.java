package ai.braineous.agentic.fno.resources;

import ai.braineous.agentic.fno.agents.FNOAgent;
import ai.braineous.rag.prompt.cgo.api.Edge;
import ai.braineous.rag.prompt.models.cgo.graph.GraphSnapshot;
import ai.braineous.rag.prompt.observe.Console;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FlightNetworkCorrectnessTests {

    @Test
    void builds_expected_connections_for_golden_dataset() {
        // Business correctness:
        // Given a hub-and-spoke mini schedule, the graph should contain some OBVIOUS valid connections.
        // We assert a small set of "golden edges" exist. This proves the substrate is building real connections,
        // not just returning "something".

        String body = goldenFlights10_body();
        Console.log("test.fno.business.golden.in", body);

        // --- Act: ingest/build snapshot using your existing path ---
        // Pick ONE:
        // A) call orchestrator directly
        // GraphSnapshot snapshot = new FNOOrchestrator().ingest(body);

        // B) call controller endpoint (still business assertions, not plumbing)
        // String out = given().contentType("application/json").body(body).post("/fno/ingest").then().statusCode(200).extract().asString();
        // GraphSnapshot snapshot = GraphSnapshot.fromJson(out);

        GraphSnapshot snapshot = ingestToSnapshot(body); // <-- replace this one-liner with your real call
        Console.log("test.fno.business.golden.snapshotHash", snapshot.snapshotHash().getValue());
        Console.log("snapshot", snapshot);

        // --- Assert: nodes = flights count ---
        assertEquals(18, snapshot.nodes().size(), "Expected 18 atomic flight/airport nodes in snapshot");

        // --- Assert: golden edges exist ---
        // These are intentionally obvious: arrive DFW -> depart DFW with a safe gap.
        // Adjust IDs ONLY if you rename flights in the asset JSON.
        assertEdgeExists(snapshot, "F101", "F201"); // AUS->DFW connects to DFW->IAH
        assertEdgeExists(snapshot, "F102", "F202"); // IAH->DFW connects to DFW->AUS
        assertEdgeExists(snapshot, "F103", "F203"); // ATL->DFW connects to DFW->ATL (return / onward)

        // Optional sanity: no self-edge should ever exist (domain invariant)
        assertNoSelfEdges(snapshot);
    }

    @Test
    void does_not_create_connections_for_invalid_cases() {
        // Business correctness:
        // The network must NOT contain connections that violate basic airline logic:
        // - too-short layover
        // - backwards time (next dep before previous arr)
        // - wrong airport continuity (arrival airport != next origin airport)

        String body = goldenFlights10_body();
        Console.log("test.fno.business.invalid.in", body);

        GraphSnapshot snapshot = ingestToSnapshot(body); // wire to your ingest/orchestrator
        Console.log("test.fno.business.invalid.snapshotHash", snapshot.snapshotHash().getValue());
        Console.log("snapshot", snapshot);

        // Sanity substrate (precondition): entities exist, otherwise edge-absence is meaningless
        assertEquals(18, snapshot.nodes().size(), "Expected 18 atomic flight/airport nodes in snapshot");

        // --- Invalid #1: too-short layover (DFW 11:10 -> DFW 11:12) ---
        assertEdgeAbsent(snapshot, "F101", "F902", "too_short_layover");

        // --- Invalid #2: backwards time ---
        // F201 departs DFW at 12:00 and arrives IAH 13:10.
        // F102 departs IAH at 10:50 (earlier), so F201->F102 is nonsense.
        assertEdgeAbsent(snapshot, "F201", "F102", "backwards_time");

        // --- Invalid #3: wrong airport continuity ---
        // F101 arrives DFW. F901 departs DAL. Should never connect.
        assertEdgeAbsent(snapshot, "F101", "F901", "wrong_airport");
    }

    @Test
    void does_create_connections_for_valid_cases() {
        String body =
                "[" +
                        "{\"id\":\"F101\",\"origin\":\"AUS\",\"dest\":\"DFW\",\"dep_utc\":\"2025-10-22T10:00:00Z\",\"arr_utc\":\"2025-10-22T11:10:00Z\"}," +
                        "{\"id\":\"F102\",\"origin\":\"IAH\",\"dest\":\"DFW\",\"dep_utc\":\"2025-10-22T10:50:00Z\",\"arr_utc\":\"2025-10-22T12:00:00Z\"}," +
                        "{\"id\":\"F103\",\"origin\":\"ATL\",\"dest\":\"DFW\",\"dep_utc\":\"2025-10-22T09:40:00Z\",\"arr_utc\":\"2025-10-22T10:50:00Z\"}," +
                        "{\"id\":\"F201\",\"origin\":\"DFW\",\"dest\":\"IAH\",\"dep_utc\":\"2025-10-22T12:00:00Z\",\"arr_utc\":\"2025-10-22T13:10:00Z\"}," +
                        "{\"id\":\"F202\",\"origin\":\"DFW\",\"dest\":\"AUS\",\"dep_utc\":\"2025-10-22T13:30:00Z\",\"arr_utc\":\"2025-10-22T14:40:00Z\"}," +
                        "{\"id\":\"F203\",\"origin\":\"DFW\",\"dest\":\"ATL\",\"dep_utc\":\"2025-10-22T11:40:00Z\",\"arr_utc\":\"2025-10-22T13:20:00Z\"}," +
                        "{\"id\":\"F301\",\"origin\":\"IAH\",\"dest\":\"MIA\",\"dep_utc\":\"2025-10-22T14:20:00Z\",\"arr_utc\":\"2025-10-22T16:30:00Z\"}," +
                        "{\"id\":\"F302\",\"origin\":\"IAH\",\"dest\":\"DEN\",\"dep_utc\":\"2025-10-22T15:10:00Z\",\"arr_utc\":\"2025-10-22T17:20:00Z\"}," +
                        "{\"id\":\"F901\",\"origin\":\"DAL\",\"dest\":\"HOU\",\"dep_utc\":\"2025-10-22T11:20:00Z\",\"arr_utc\":\"2025-10-22T12:20:00Z\"}," +
                        "{\"id\":\"F902\",\"origin\":\"DFW\",\"dest\":\"IAH\",\"dep_utc\":\"2025-10-22T11:12:00Z\",\"arr_utc\":\"2025-10-22T12:22:00Z\"}" +
                        "]";

        Console.log("test.fno.business.valid.in", body);

        GraphSnapshot snapshot = ingestToSnapshot(body);

        Console.log("test.fno.business.valid.snapshotHash", snapshot.snapshotHash().getValue());
        Console.log("snapshot", snapshot);

        // MUST exist (valid connections)
        assertEdgePresent(snapshot, "Flight:F101", "Flight:F201", "valid_connection");
        assertEdgePresent(snapshot, "Flight:F102", "Flight:F202", "valid_connection");
        assertEdgePresent(snapshot, "Flight:F103", "Flight:F203", "valid_connection");
        assertEdgePresent(snapshot, "Flight:F201", "Flight:F301", "valid_connection");
    }


    /** Asset: 10 flights with an unambiguous hub (DFW) and clean connection windows. */
    private String goldenFlights10_body() {
        // Times chosen to make "obvious yes" connections:
        // - F101 arrives DFW 11:10, F201 departs DFW 12:00  -> YES
        // - F102 arrives DFW 12:00, F202 departs DFW 13:30  -> YES
        // - F103 arrives DFW 10:50, F203 departs DFW 11:40  -> YES
        // And two traps that should not connect cleanly (used in test #2 later).
        return "[" +
                "{\"id\":\"F101\",\"origin\":\"AUS\",\"dest\":\"DFW\",\"dep_utc\":\"2025-10-22T10:00:00Z\",\"arr_utc\":\"2025-10-22T11:10:00Z\"}," +
                "{\"id\":\"F102\",\"origin\":\"IAH\",\"dest\":\"DFW\",\"dep_utc\":\"2025-10-22T10:50:00Z\",\"arr_utc\":\"2025-10-22T12:00:00Z\"}," +
                "{\"id\":\"F103\",\"origin\":\"ATL\",\"dest\":\"DFW\",\"dep_utc\":\"2025-10-22T09:40:00Z\",\"arr_utc\":\"2025-10-22T10:50:00Z\"}," +

                "{\"id\":\"F201\",\"origin\":\"DFW\",\"dest\":\"IAH\",\"dep_utc\":\"2025-10-22T12:00:00Z\",\"arr_utc\":\"2025-10-22T13:10:00Z\"}," +
                "{\"id\":\"F202\",\"origin\":\"DFW\",\"dest\":\"AUS\",\"dep_utc\":\"2025-10-22T13:30:00Z\",\"arr_utc\":\"2025-10-22T14:40:00Z\"}," +
                "{\"id\":\"F203\",\"origin\":\"DFW\",\"dest\":\"ATL\",\"dep_utc\":\"2025-10-22T11:40:00Z\",\"arr_utc\":\"2025-10-22T13:20:00Z\"}," +

                // Additional branch flights (not asserted here; they just make the network feel real)
                "{\"id\":\"F301\",\"origin\":\"IAH\",\"dest\":\"MIA\",\"dep_utc\":\"2025-10-22T14:20:00Z\",\"arr_utc\":\"2025-10-22T16:30:00Z\"}," +
                "{\"id\":\"F302\",\"origin\":\"IAH\",\"dest\":\"DEN\",\"dep_utc\":\"2025-10-22T15:10:00Z\",\"arr_utc\":\"2025-10-22T17:20:00Z\"}," +

                // Traps (for test #2 later)
                "{\"id\":\"F901\",\"origin\":\"DAL\",\"dest\":\"HOU\",\"dep_utc\":\"2025-10-22T11:20:00Z\",\"arr_utc\":\"2025-10-22T12:20:00Z\"}," + // wrong airport vs DFW
                "{\"id\":\"F902\",\"origin\":\"DFW\",\"dest\":\"IAH\",\"dep_utc\":\"2025-10-22T11:12:00Z\",\"arr_utc\":\"2025-10-22T12:22:00Z\"}" +  // too tight vs F101 (11:10 -> 11:12)
                "]";
    }



    private void assertEdgeExists(GraphSnapshot snapshot, String fromFlightId, String toFlightId) {
        String fromId = normalizeFlightId(fromFlightId);
        String toId = normalizeFlightId(toFlightId);

        boolean found = snapshot.edges().values().stream().anyMatch(e ->
                fromId.equals(e.getFromFactId()) && toId.equals(e.getToFactId())
        );

        Console.log("test.fno.business.edge.exists",
                fromId + "->" + toId + " found=" + found);

        assertTrue(found, "Expected edge " + fromId + " -> " + toId + " to exist");
    }

    private String normalizeFlightId(String id) {
        if (id == null) return null;
        if (id.startsWith("Flight:")) return id;
        if (id.startsWith("F")) return "Flight:" + id;   // "F101" -> "Flight:F101"
        return id;
    }

    private void assertNoSelfEdges(GraphSnapshot snapshot) {
        boolean anySelf = snapshot.edges().values().stream().anyMatch(e -> {
            String fromId = edgeFromId(e);
            String toId = edgeToId(e);
            return fromId != null && fromId.equals(toId);
        });
        Console.log("test.fno.business.edge.self", "anySelf=" + anySelf);
        assertFalse(anySelf, "No self-edges allowed");
    }

    /**
     * IMPORTANT: update these two methods to match your Edge structure.
     * Keep them tiny so edits are surgical.
     */
    private String edgeFromId(Edge e) {
        // example options (pick your reality):
        // return e.getFrom().getId();
        // return e.getFromId();
        // return e.getFrom();
        return e.getFromFactId(); // <-- most likely tweak point
    }

    private String edgeToId(Edge e) {
        // return e.getTo().getId();
        // return e.getToId();
        // return e.getTo();
        return e.getToFactId(); // <-- most likely tweak point
    }

    /**
     * Replace this with your real ingestion->snapshot call.
     * Keep it as a one-liner in the test so plumbing changes don't infect the assertions.
     */
    private GraphSnapshot ingestToSnapshot(String body) {
        JsonArray jsonArray = JsonParser.parseString(body).getAsJsonArray();
        GraphSnapshot snapshot = (GraphSnapshot) new FNOAgent().ingestFlights(jsonArray);
        return snapshot;
    }

    private void assertEdgeAbsent(GraphSnapshot snapshot, String fromFlightId, String toFlightId, String reason) {
        String fromId = normalizeFlightId(fromFlightId);
        String toId = normalizeFlightId(toFlightId);

        boolean found = snapshot.edges().values().stream().anyMatch(e ->
                fromId.equals(e.getFromFactId()) && toId.equals(e.getToFactId())
        );

        Console.log("test.fno.business.edge.absent",
                fromId + "->" + toId + " reason=" + reason + " found=" + found);

        assertFalse(found, "Expected edge to be ABSENT (" + reason + "): " + fromId + " -> " + toId);
    }

    private void assertEdgePresent(
            GraphSnapshot snapshot,
            String from,
            String to,
            String reason
    ) {
        boolean found = snapshot.edges().containsKey(
                "Edge:" + from + "->" + to
        );

        Console.log("test.fno.business.edge.present",
                from + "->" + to + " reason=" + reason + " found=" + found);

        assertTrue(found, "Expected edge to be PRESENT (" + reason + "): "
                + from + " -> " + to);
    }
}
