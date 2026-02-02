package io.braineous.dd.llm.pg.services;

import ai.braineous.cgo.history.MongoHistoryStore;
import ai.braineous.rag.prompt.cgo.prompt.CatalogEntry;
import ai.braineous.rag.prompt.cgo.prompt.CatalogMongoStore;
import ai.braineous.rag.prompt.cgo.prompt.CatalogOrchestrator;
import ai.braineous.rag.prompt.cgo.prompt.CatalogSnapshot;
import com.google.gson.JsonObject;
import com.mongodb.client.MongoClient;

import io.braineous.dd.llm.cr.model.CommitRequest;
import io.braineous.dd.llm.pg.model.ExecutionView;
import io.braineous.dd.llm.pg.model.PolicyGateResult;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class PolicyGateOrchestrator {
    @Inject
    MongoClient mongoClient;

    private CatalogOrchestrator orch;

    private MongoHistoryStore historyStore;

    public PolicyGateOrchestrator() {
    }

    @PostConstruct
    public void start(){
        CatalogMongoStore store = new CatalogMongoStore(this.mongoClient);
        this.orch = new CatalogOrchestrator(store);

        historyStore = new MongoHistoryStore(this.mongoClient);
    }

    //---TODO---EXECUTE---------------------------------------------------
    public ExecutionView getExecutions(String queryKind){

        return null;
    }


    public PolicyGateResult approve(String queryKind, String executionId){
        return null;
    }

    public PolicyGateResult reject(String queryKind, String executionId){
        return null;
    }

    //-----------------DEPRECATE_NOT_EXPOSE_READ_THE_CATALOG---------------
    public CatalogSnapshot findCatalogSnapshot(String queryKind){
        CatalogSnapshot snapshot = orch.resolveSnapshot(queryKind);
        return snapshot;
    }

    public List<CatalogEntry> findAllEntries(){
        List<CatalogEntry> all = orch.listEntries();
        return all;
    }

    public CatalogEntry findEntry(String queryKind) {
        if (this.orch == null) {
            return null;
        }
        return this.orch.getEntry(queryKind);
    }


    //-----------helpers-----------------------------------
    public static CommitRequest toCommitRequest(
            CatalogEntry entry,
            JsonObject payload,
            String actor,
            List<String> notes
    ) {
        if (entry == null) {
            return null;
        }

        CommitRequest r = new CommitRequest();

        String qk = entry.safeQueryKind();
        if (qk != null) {
            r.setQueryKind(qk);
        }

        String v = entry.safeCatalogVersion();
        if (v != null) {
            r.setCatalogVersion(v);
        }

        if (actor != null) {
            String t = actor.trim();
            if (!t.isEmpty()) {
                r.setActor(t);
            }
        }

        if (notes != null) {
            r.setNotes(notes);
        }

        if (payload != null) {
            // deep-ish copy to avoid shared mutation
            r.setPayload(
                    com.google.gson.JsonParser
                            .parseString(payload.toString())
                            .getAsJsonObject()
            );
        }

        return r;
    }
}
