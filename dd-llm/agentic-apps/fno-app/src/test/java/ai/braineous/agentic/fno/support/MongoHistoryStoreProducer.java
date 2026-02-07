package ai.braineous.agentic.fno.support;

import ai.braineous.cgo.history.MongoHistoryStore;
import com.mongodb.client.MongoClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Produces;

@ApplicationScoped
public class MongoHistoryStoreProducer {

    @Inject
    MongoClient mongoClient;

    @Produces
    @ApplicationScoped
    public MongoHistoryStore mongoHistoryStore() {
        return new MongoHistoryStore(mongoClient);
    }
}
