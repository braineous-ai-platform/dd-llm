package io.braineous.dd.llm.pg.services;

import ai.braineous.rag.prompt.cgo.prompt.CatalogEntry;
import ai.braineous.rag.prompt.cgo.prompt.CatalogMongoStore;
import ai.braineous.rag.prompt.cgo.prompt.CatalogOrchestrator;
import ai.braineous.rag.prompt.cgo.prompt.CatalogSnapshot;
import com.mongodb.client.MongoClient;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class PolicyGateOrchestrator {
    @Inject
    MongoClient mongoClient;

    private CatalogOrchestrator orch;

    public PolicyGateOrchestrator() {
    }

    @PostConstruct
    public void start(){
        CatalogMongoStore store = new CatalogMongoStore(this.mongoClient);
        this.orch = new CatalogOrchestrator(store);
    }

    //-----------------READ_THE_CATALOG---------------
    public CatalogSnapshot findCatalogSnapshot(String queryKind){
        CatalogSnapshot snapshot = orch.resolveSnapshot(queryKind);
        return snapshot;
    }

    public List<CatalogEntry> findAllEntries(){
        List<CatalogEntry> all = orch.listEntries();
        return all;
    }

    //------EXECUTE-----------------------------------
}
