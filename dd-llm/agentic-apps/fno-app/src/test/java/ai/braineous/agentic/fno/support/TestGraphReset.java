package ai.braineous.agentic.fno.support;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;

@ApplicationScoped
public class TestGraphReset {

    @Inject
    MongoClient mongoClient;

    public void clearGraph() {
        MongoDatabase db = mongoClient.getDatabase("cgo");
        db.getCollection("cgo_edges").deleteMany(new Document());
        db.getCollection("cgo_nodes").deleteMany(new Document());
    }
}

