package ai.braineous.agentic.fno.support;

import ai.braineous.agentic.fno.agents.FNOIngestionAgent;
import ai.braineous.rag.prompt.models.cgo.graph.GraphBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;

@ApplicationScoped
public class TestGraphSupport {

    @Inject
    MongoClient mongoClient;

    public void clearGraph() {
        MongoDatabase db = mongoClient.getDatabase("cgo");
        db.getCollection("cgo_edges").deleteMany(new Document());
        db.getCollection("cgo_nodes").deleteMany(new Document());
    }

    public void setupGraphSnapshot(){
        String snapshot = "{\n" +
                "  \"flights\": [\n" +
                "    {\n" +
                "      \"id\": \"F100\",\n" +
                "      \"number\": \"F100\",\n" +
                "      \"origin\": \"AUS\",\n" +
                "      \"dest\": \"DFW\",\n" +
                "      \"dep_utc\": \"2025-10-22T10:00:00Z\",\n" +
                "      \"arr_utc\": \"2025-10-22T11:10:00Z\",\n" +
                "      \"capacity\": 150,\n" +
                "      \"equipment\": \"320\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": \"F102\",\n" +
                "      \"number\": \"F102\",\n" +
                "      \"origin\": \"AUS\",\n" +
                "      \"dest\": \"DFW\",\n" +
                "      \"dep_utc\": \"2025-10-22T11:30:00Z\",\n" +
                "      \"arr_utc\": \"2025-10-22T12:40:00Z\",\n" +
                "      \"capacity\": 150,\n" +
                "      \"equipment\": \"320\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": \"F110\",\n" +
                "      \"number\": \"F110\",\n" +
                "      \"origin\": \"SAT\",\n" +
                "      \"dest\": \"DFW\",\n" +
                "      \"dep_utc\": \"2025-10-22T11:10:00Z\",\n" +
                "      \"arr_utc\": \"2025-10-22T12:15:00Z\",\n" +
                "      \"capacity\": 150,\n" +
                "      \"equipment\": \"320\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": \"F120\",\n" +
                "      \"number\": \"F120\",\n" +
                "      \"origin\": \"IAH\",\n" +
                "      \"dest\": \"DFW\",\n" +
                "      \"dep_utc\": \"2025-10-22T11:20:00Z\",\n" +
                "      \"arr_utc\": \"2025-10-22T12:25:00Z\",\n" +
                "      \"capacity\": 150,\n" +
                "      \"equipment\": \"319\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": \"F200\",\n" +
                "      \"number\": \"F200\",\n" +
                "      \"origin\": \"DFW\",\n" +
                "      \"dest\": \"ORD\",\n" +
                "      \"dep_utc\": \"2025-10-22T13:30:00Z\",\n" +
                "      \"arr_utc\": \"2025-10-22T16:50:00Z\",\n" +
                "      \"capacity\": 150,\n" +
                "      \"equipment\": \"738\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": \"F210\",\n" +
                "      \"number\": \"F210\",\n" +
                "      \"origin\": \"DFW\",\n" +
                "      \"dest\": \"JFK\",\n" +
                "      \"dep_utc\": \"2025-10-22T13:20:00Z\",\n" +
                "      \"arr_utc\": \"2025-10-22T17:10:00Z\",\n" +
                "      \"capacity\": 150,\n" +
                "      \"equipment\": \"321\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": \"F220\",\n" +
                "      \"number\": \"F220\",\n" +
                "      \"origin\": \"DFW\",\n" +
                "      \"dest\": \"LAX\",\n" +
                "      \"dep_utc\": \"2025-10-22T13:45:00Z\",\n" +
                "      \"arr_utc\": \"2025-10-22T15:20:00Z\",\n" +
                "      \"capacity\": 150,\n" +
                "      \"equipment\": \"73G\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        GraphBuilder.getInstance().clear();
        this.clearGraph();

        FNOIngestionAgent agent = new FNOIngestionAgent();

        JsonObject flightsJson = JsonParser.parseString(snapshot).getAsJsonObject();
        JsonArray array = flightsJson.get("flights").getAsJsonArray();
        agent.ingestFlights(array);

    }
}

