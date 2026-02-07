package ai.braineous.agentic.fno.support;

import ai.braineous.cgo.history.MongoHistoryStore;
import ai.braineous.rag.prompt.models.cgo.graph.GraphStoreMongo;
import com.mongodb.client.MongoClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Produces;

@ApplicationScoped
public class MongoGraphStoreProducer {

    @Inject
    MongoClient mongoClient;

    @Produces
    @ApplicationScoped
    public GraphStoreMongo mongoGraphStore() {
        return new GraphStoreMongo(mongoClient);
    }
}
