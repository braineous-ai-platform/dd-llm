package ai.braineous.agentic.fno.reasoning.ingestion;

import ai.braineous.rag.prompt.cgo.api.Edge;
import ai.braineous.rag.prompt.cgo.api.Fact;
import ai.braineous.rag.prompt.cgo.api.Relationship;
import ai.braineous.rag.prompt.observe.Console;
import ai.braineous.rag.prompt.utils.Resources;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FNORelationProviderTests {

    @Test
    void relationshipProvider_builds_possible_connections() {
        Console.log("test.start", "FNORelationshipProvider");

        FNORelationshipProvider provider = new FNORelationshipProvider();
        List<Fact> facts = new ArrayList<>();

        // Needs dep_utc/arr_utc now because provider filters by layover window
        Fact f100 = new Fact(
                "Flight:F100",
                "{"
                        + "\"id\":\"Flight:F100\","
                        + "\"kind\":\"Flight\","
                        + "\"from\":\"Airport:AUS\","
                        + "\"to\":\"Airport:DFW\","
                        + "\"dep_utc\":\"2025-10-22T10:00:00Z\","
                        + "\"arr_utc\":\"2025-10-22T11:10:00Z\""
                        + "}"
        );
        f100.setMode("relational");
        facts.add(f100);

        // DFW departure 40 min after F100 arrival (11:10 -> 11:50) => valid (>= 30)
        Fact f200 = new Fact(
                "Flight:F200",
                "{"
                        + "\"id\":\"Flight:F200\","
                        + "\"kind\":\"Flight\","
                        + "\"from\":\"Airport:DFW\","
                        + "\"to\":\"Airport:ORD\","
                        + "\"dep_utc\":\"2025-10-22T11:50:00Z\","
                        + "\"arr_utc\":\"2025-10-22T13:50:00Z\""
                        + "}"
        );
        f200.setMode("relational");
        facts.add(f200);

        // Another DFW departure also valid (11:10 -> 12:20) => 70 min
        Fact f210 = new Fact(
                "Flight:F210",
                "{"
                        + "\"id\":\"Flight:F210\","
                        + "\"kind\":\"Flight\","
                        + "\"from\":\"Airport:DFW\","
                        + "\"to\":\"Airport:JFK\","
                        + "\"dep_utc\":\"2025-10-22T12:20:00Z\","
                        + "\"arr_utc\":\"2025-10-22T15:30:00Z\""
                        + "}"
        );
        f210.setMode("relational");
        facts.add(f210);

        Console.log("test.facts.count", facts.size());

        List<Relationship> rels = provider.provideRelationships(facts);

        Console.log("test.relationships.count", rels.size());
        for (Relationship r : rels) {
            Edge e = (Edge) r.getEdge();
            Console.log("rel", e.getFromFactId() + " -> " + e.getToFactId());
        }

        assertFalse(rels.isEmpty(), "relationships should be generated");
        assertEquals(2, rels.size(), "expected 2 outbound connections from F100 via DFW");

        // verify we got exactly: F100 -> F200 and F100 -> F210
        java.util.Set<String> keys = new java.util.HashSet<>();
        for (Relationship r : rels) {
            Edge e = (Edge) r.getEdge();
            keys.add(e.getFromFactId() + "->" + e.getToFactId());
        }

        assertTrue(keys.contains("Flight:F100->Flight:F200"), "missing expected edge F100->F200");
        assertTrue(keys.contains("Flight:F100->Flight:F210"), "missing expected edge F100->F210");
    }



    @Test
    void relationshipProvider_skips_self_and_ignores_non_flight_noise() {
        Console.log("test.start", "FNORelationshipProvider.noise_and_self");

        FNORelationshipProvider provider = new FNORelationshipProvider();
        List<Fact> facts = new ArrayList<>();

        // Real flights (must include dep_utc/arr_utc now; provider filters by layover window)
        Fact f100 = new Fact(
                "Flight:F100",
                "{"
                        + "\"id\":\"Flight:F100\","
                        + "\"kind\":\"Flight\","
                        + "\"from\":\"Airport:AUS\","
                        + "\"to\":\"Airport:DFW\","
                        + "\"dep_utc\":\"2025-10-22T10:00:00Z\","
                        + "\"arr_utc\":\"2025-10-22T11:10:00Z\""
                        + "}"
        );
        f100.setMode("relational");
        facts.add(f100);

        // Valid connection from DFW after 40 mins (>=30)
        Fact f200 = new Fact(
                "Flight:F200",
                "{"
                        + "\"id\":\"Flight:F200\","
                        + "\"kind\":\"Flight\","
                        + "\"from\":\"Airport:DFW\","
                        + "\"to\":\"Airport:ORD\","
                        + "\"dep_utc\":\"2025-10-22T11:50:00Z\","
                        + "\"arr_utc\":\"2025-10-22T13:50:00Z\""
                        + "}"
        );
        f200.setMode("relational");
        facts.add(f200);

        // Noise fact should be ignored (kind != Flight and id != Flight:*)
        Fact noise = new Fact("Noise:X1", "{ \"kind\":\"SomethingElse\", \"foo\":\"bar\" }");
        noise.setMode("atomic");
        facts.add(noise);

        // Self-loop-ish flight: DFW -> DFW
        // Give it times, but ensure provider does NOT create Flight:FSELF -> Flight:FSELF
        // (it may still legitimately create FSELF -> F200 depending on times; that's fine)
        Fact fself = new Fact(
                "Flight:FSELF",
                "{"
                        + "\"id\":\"Flight:FSELF\","
                        + "\"kind\":\"Flight\","
                        + "\"from\":\"Airport:DFW\","
                        + "\"to\":\"Airport:DFW\","
                        + "\"dep_utc\":\"2025-10-22T09:00:00Z\","
                        + "\"arr_utc\":\"2025-10-22T11:00:00Z\""
                        + "}"
        );
        fself.setMode("relational");
        facts.add(fself);

        Console.log("test.facts.count", facts.size());

        List<Relationship> rels = provider.provideRelationships(facts);

        Console.log("test.relationships.count", rels.size());

        boolean found = false;

        for (Relationship r : rels) {
            Edge edge = (Edge) r.getEdge();

            Console.log("rel", edge.getFromFactId() + " -> " + edge.getToFactId());

            // Provider must never emit literal self-edge (same from/to id)
            assertNotEquals(edge.getFromFactId(), edge.getToFactId(), "no self-loop");

            if ("Flight:F100".equals(edge.getFromFactId())
                    && "Flight:F200".equals(edge.getToFactId())) {
                found = true;
            }
        }

        assertTrue(found, "should contain Flight:F100 -> Flight:F200");
    }


    @Test
    void relationshipProvider_builds_expected_edges_from_real_flights_json() throws Exception{
        Console.log("test.start", "FNORelationshipProvider.real_flights_json");

        // Read the curated payload from the fixture file
        String body = Resources.getResource("models/fno/nano_llm_sample_dataset/flights.json");

        Console.log("test.json.length", body.length());

        // Extract Facts using your real extractor (the one you uploaded)
        FNOFactExtractor extractor = new FNOFactExtractor();
        List<Fact> facts = extractor.extract(body);

        Console.log("test.facts.count", facts.size());

        // Build relationships
        FNORelationshipProvider provider = new FNORelationshipProvider();
        List<Relationship> rels = provider.provideRelationships(facts);

        Console.log("test.relationships.count", rels.size());

        // Print a few edges (first 10) to keep logs readable
        int shown = 0;
        for (Relationship r : rels) {
            Edge edge = (Edge) r.getEdge();
            Console.log("rel", edge.getFromFactId() + " -> " + edge.getToFactId());
            shown++;
            if (shown >= 10) break;
        }

        // Expect: 4 inbound to DFW (F100,F102,F110,F120) x 3 outbound from DFW (F200,F210,F220) = 12
        assertEquals(12, rels.size(), "expected 12 possible-connection relationships via DFW");
    }
}
