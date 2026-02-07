package ai.braineous.agentic.fno.reasoning.ingestion;

import ai.braineous.rag.prompt.cgo.api.Edge;
import ai.braineous.rag.prompt.cgo.api.Fact;
import ai.braineous.rag.prompt.cgo.api.Relationship;
import ai.braineous.rag.prompt.cgo.api.RelationshipProvider;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FNORelationshipProvider implements RelationshipProvider {
    private static final long MIN_CONNECT_MINUTES = 30;   // v1 default
    private static final long MAX_CONNECT_MINUTES = 6 * 60; // v1 default

    @Override
    public List<Relationship> provideRelationships(List<Fact> facts) {
        if (facts == null || facts.isEmpty()) return List.of();

        Map<String, List<Fact>> flightsByFromAirport = new HashMap<>();
        List<Fact> flights = new ArrayList<>();

        // collect flight-like facts by JSON shape (not by mode)
        for (Fact f : facts) {
            if (f == null || f.getId() == null || f.getText() == null) continue;

            JsonObject j;
            try {
                j = JsonParser.parseString(f.getText()).getAsJsonObject();
            } catch (Exception e) {
                continue;
            }

            String kind = j.has("kind") ? j.get("kind").getAsString() : "";
            if (!"Flight".equals(kind)) continue;

            // also enforce id prefix as belt + suspenders
            if (!f.getId().startsWith("Flight:")) continue;

            String fromAirport;
            try {
                fromAirport = j.get("from").getAsString();
            } catch (Exception e) {
                continue;
            }

            flights.add(f);
            flightsByFromAirport.computeIfAbsent(fromAirport, k -> new ArrayList<>()).add(f);
        }

        if (flights.size() < 2) return List.of();

        List<Relationship> rels = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();

        for (Fact a : flights) {
            JsonObject aJson;
            try {
                aJson = JsonParser.parseString(a.getText()).getAsJsonObject();
            } catch (Exception e) {
                continue;
            }

            String hub;
            try {
                hub = aJson.get("to").getAsString(); // a arrives here
            } catch (Exception e) {
                continue;
            }

            List<Fact> candidates = flightsByFromAirport.getOrDefault(hub, List.of());

            for (Fact b : candidates) {
                if (b == null || b.getId() == null) continue;

                String fromId = a.getId();
                String toId = b.getId();

                // hard self-edge guard
                if (fromId.equals(toId)) continue;

                // dedupe (stable)
                String key = fromId + "->" + toId;
                if (!seen.add(key)) continue;

                JsonObject bJson;
                try {
                    bJson = JsonParser.parseString(b.getText()).getAsJsonObject();
                } catch (Exception e) {
                    continue;
                }

                java.time.Instant aArr = parseInstant(aJson, "arr_utc");
                java.time.Instant bDep = parseInstant(bJson, "dep_utc");

                // skip if timestamps missing/invalid (noise-safe)
                if (aArr == null || bDep == null) continue;

                long layoverMin = minutesBetween(aArr, bDep);
                if (layoverMin < MIN_CONNECT_MINUTES) continue;
                if (layoverMin > MAX_CONNECT_MINUTES) continue;

                // create edge with stable id
                Edge edge = new Edge();
                edge.setId("Edge:" + key);
                edge.setFromFactId(fromId);
                edge.setToFactId(toId);

                // IMPORTANT: do NOT reuse the same Fact instances as relationship endpoints
                // Create lightweight refs so equals()/identity can't collapse into self-edges downstream.
                Fact fromRef = new Fact(fromId, a.getText());
                Fact toRef   = new Fact(toId, b.getText());

                rels.add(new Relationship(fromRef, toRef, edge));
            }
        }

        return List.copyOf(rels);
    }

    private long minutesBetween(java.time.Instant a, java.time.Instant b) {
        if (a == null || b == null) return Long.MIN_VALUE; // or throw, but skip is better here
        return java.time.Duration.between(a, b).toMinutes();
    }


    private java.time.Instant parseInstant(JsonObject j, String key) {
        if (j == null || key == null || !j.has(key)) return null;
        try {
            return java.time.Instant.parse(j.get(key).getAsString());
        } catch (Exception e) {
            return null;
        }
    }

}

