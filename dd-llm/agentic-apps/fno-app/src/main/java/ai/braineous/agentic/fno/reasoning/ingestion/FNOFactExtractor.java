package ai.braineous.agentic.fno.reasoning.ingestion;

import ai.braineous.rag.prompt.cgo.api.Fact;
import ai.braineous.rag.prompt.cgo.api.FactExtractor;
import ai.braineous.rag.prompt.observe.Console;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

/**
 * FNOFactExtractor
 * Input: JSON with either { "flights": [ ... ] } or a bare JSON array [ ... ].
 * Each flight:
 *   { "id":"F102", "origin":"AUS", "dest":"DFW",
 *     "dep_utc":"2025-10-22T11:30:00Z", "arr_utc":"2025-10-22T12:40:00Z" }
 *
 * Emits:
 *   Airport facts:  id="Airport:AUS", kind="Airport", features:{code:"AUS"}
 *   Flight fact:    id="Flight:F102", kind="Flight",
 *                   features:{from:"Airport:AUS", to:"Airport:DFW",
 *                             depTime:"...", arrTime:"...", carrier?}
 */
public class FNOFactExtractor implements FactExtractor {

    /**
     * Applies this function to the given argument.
     *
     * @param jsonArrayStr the function argument
     * @return the function result
     */
    @Override
    public List<Fact> extract(String jsonArrayStr) {
        List<Fact> facts = new ArrayList<>();

        if (jsonArrayStr == null || jsonArrayStr.isBlank()) {
            return facts;
        }

        JsonArray flightsArray;
        try {
            var root = JsonParser.parseString(jsonArrayStr);

            if (root.isJsonArray()) {
                flightsArray = root.getAsJsonArray();
            } else if (root.isJsonObject()
                    && root.getAsJsonObject().has("flights")
                    && root.getAsJsonObject().get("flights").isJsonArray()) {
                flightsArray = root.getAsJsonObject().getAsJsonArray("flights");
            } else {
                // v1: fail-soft
                return facts;
            }
        } catch (Exception e) {
            // v1: fail-soft
            return facts;
        }

        // Dedup airport facts across all flights
        java.util.Set<String> seenAirports = new java.util.HashSet<>();

        for (int i = 0; i < flightsArray.size(); i++) {
            if (!flightsArray.get(i).isJsonObject()) {
                continue;
            }

            JsonObject o = flightsArray.get(i).getAsJsonObject();

            // Skip malformed flight rows (v1: resilient ingestion)
            if (!o.has("id") || !o.has("origin") || !o.has("dest") || !o.has("dep_utc") || !o.has("arr_utc")) {
                continue;
            }

            String id = o.get("id").getAsString();          // "F102"
            String src = o.get("origin").getAsString();     // "AUS"
            String dst = o.get("dest").getAsString();       // "DFW"
            String depZ = o.get("dep_utc").getAsString();   // "2025-10-22T11:30:00Z"
            String arrZ = o.get("arr_utc").getAsString();   // "2025-10-22T12:40:00Z"

            // Airport facts (one per station)
            String srcAirportId = "Airport:" + src;
            JsonObject srcAirportJson = new JsonObject();
            srcAirportJson.addProperty("id", srcAirportId);
            srcAirportJson.addProperty("kind", "Airport");
            srcAirportJson.addProperty("mode", "atomic");
            srcAirportJson.addProperty("code", src); // simple feature for v1
            Console.log("src_airport", srcAirportJson);

            if (seenAirports.add(srcAirportId)) {
                facts.add(new Fact(srcAirportId, srcAirportJson.toString()));
            }

            String dstAirportId = "Airport:" + dst;
            JsonObject dstAirportJson = new JsonObject();
            dstAirportJson.addProperty("id", dstAirportId);
            dstAirportJson.addProperty("kind", "Airport");
            dstAirportJson.addProperty("mode", "atomic");
            dstAirportJson.addProperty("code", dst); // simple feature for v1
            Console.log("dst_airport", dstAirportJson); // FIX: log correct object

            if (seenAirports.add(dstAirportId)) {
                facts.add(new Fact(dstAirportId, dstAirportJson.toString()));
            }

            // Flight fact (canonical)
            String flightId = "Flight:" + id;
            JsonObject flightJson = new JsonObject();
            flightJson.addProperty("id", flightId);         // align JSON id with Fact id
            flightJson.addProperty("kind", "Flight");
            flightJson.addProperty("mode", "atomic");
            flightJson.addProperty("from", srcAirportId);   // link to Airport IDs
            flightJson.addProperty("to", dstAirportId);
            flightJson.addProperty("dep_utc", depZ);
            flightJson.addProperty("arr_utc", arrZ);

            Console.log("flight", flightJson);
            Fact flightFact = new Fact(flightId, flightJson.toString());
            flightFact.setMode("atomic");
            facts.add(flightFact);
        }

        return facts;
    }
}
