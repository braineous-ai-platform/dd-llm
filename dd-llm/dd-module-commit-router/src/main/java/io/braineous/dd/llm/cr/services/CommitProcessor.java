package io.braineous.dd.llm.cr.services;

import com.google.gson.JsonObject;
import io.braineous.dd.llm.core.model.Why;
import io.braineous.dd.llm.core.processor.GsonJsonSerializer;
import io.braineous.dd.llm.core.processor.HttpPoster;
import io.braineous.dd.llm.core.processor.JsonSerializer;
import io.braineous.dd.llm.cr.client.CommitClient;
import io.braineous.dd.llm.cr.model.*;
import io.braineous.dd.llm.cr.persistence.CommitAuditViewMongoStore;
import io.braineous.dd.llm.cr.persistence.CommitEventMongoStore;
import io.braineous.dd.llm.cr.persistence.CommitReceiptMongoStore;
import io.braineous.dd.llm.cr.persistence.CommitRequestMongoStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CommitProcessor {

    boolean asyncMode = true; //default. IT-support for sync-mode tests

    @Inject
    private CommitRequestMongoStore requestStore;

    @Inject
    private CommitEventMongoStore eventStore;

    @Inject
    private CommitReceiptMongoStore receiptStore;

    @Inject
    private CommitAuditViewMongoStore auditViewStore;

    @Inject
    private HttpPoster httpPoster;

    public CommitProcessor() {
    }

    void setAsyncMode(boolean asyncMode) {
        this.asyncMode = asyncMode;
    }

    void setHttpPoster(HttpPoster httpPoster) {
        this.httpPoster = httpPoster;
    }

    public void orchestrate(CommitRequest request){
        try {

            //validate request
            CommitValidation validation = this.validate(request);
            if(!validation.isOk()){
                //TODO: //if fail DLQ-D

                return;
            }

            String commitId = request.safeCommitId();
            if (commitId == null) {
                // TODO DLQ-D

                return;
            }

            CommitReceipt receipt = createReceiptEmitAttempted(commitId);
            if(receipt == null){
                // TODO DLQ-D

                return;
            }

            CommitEvent event = createEvent(request, commitId);
            if(event  == null){
                // TODO DLQ-D

                return;
            }

            CommitAuditView view = CommitAuditView.from(commitId, event, request, receipt);
            if (view == null) {
                //TODO: //if fail DLQ-D

                return;
            }


            String kafkaEvent = view.toJsonString();
            if(kafkaEvent == null || kafkaEvent.trim().isBlank()){
                //TODO: //if fail DLQ-D

                return;
            }

            try {
                if (asyncMode) {
                    //emit to the downstream 'commit' topic
                    JsonSerializer ser = new GsonJsonSerializer();
                    String endpoint = "llm/response";
                    JsonObject commitJson = view.toJson();

                    CommitResult cr = CommitClient.getInstance().invoke(
                            this.httpPoster,
                            ser,
                            endpoint,
                            commitJson,
                            commitJson
                    );


                    if (cr == null || !cr.isOk()) {
                        //if fail, DLQ-S
                        //no return. still record the view.
                        //view should not drift due to any error
                        //in emission
                    }
                }
            }catch(Exception e){
                //kafka_failure, hence place in DLQ-S for retry
                //but view should be recorded
            }

            //update db and corresponding collections
            this.commit(request, receipt, event);
        }catch (Exception e){
            //DLQ-S
        }

    }

    private CommitValidation validate(CommitRequest request) {

        if (request == null) {
            return CommitValidation.isFail(new Why("VALIDATION_NULL_REQUEST", null));
        }

        String qk = request.safeQueryKind();
        if (qk == null) {
            return CommitValidation.isFail(new Why("VALIDATION_QUERY_KIND_REQUIRED", null));
        }

        String cv = request.safeCatalogVersion();
        if (cv == null) {
            return CommitValidation.isFail(new Why("VALIDATION_CATALOG_VERSION_REQUIRED", null));
        }

        String a = request.safeActor();
        if (a == null) {
            return CommitValidation.isFail(new Why("VALIDATION_ACTOR_REQUIRED", null));
        }

        if (request.getPayload() == null) {
            return CommitValidation.isFail(new Why("VALIDATION_PAYLOAD_REQUIRED", null));
        }

        return CommitValidation.isOK();
    }


    private void commit(
            CommitRequest request,
            CommitReceipt receipt,
            CommitEvent event
    ) {
        // -------------------------
        // Storage-level validation
        // -------------------------

        if (request == null) {
            throw new IllegalArgumentException("request cannot be null");
        }
        if (event == null) {
            throw new IllegalArgumentException("event cannot be null");
        }
        if (receipt == null) {
            throw new IllegalArgumentException("receipt cannot be null");
        }

        String commitId = safe(event.safeCommitId());
        if (commitId == null) {
            throw new IllegalArgumentException("event.commitId required");
        }

        String rId = safe(receipt.safeCommitId());
        if (rId == null) {
            throw new IllegalArgumentException("receipt.commitId required");
        }

        if (!commitId.equals(rId)) {
            throw new IllegalArgumentException("commitId mismatch: event vs receipt");
        }

        // -------------------------
        // Persist primitives first
        // -------------------------

        // enforce axis on event/receipt if needed
        event.setCommitId(commitId);
        receipt.setCommitId(commitId);

        // 1) request
        // if your request store needs id axis, do it here
        // requestStore.upsert(commitId, request);
        requestStore.upsertRequest(commitId, request);

        // 2) event
        eventStore.upsertEvent(event);

        // 3) receipt
        receiptStore.upsertReceipt(receipt);

        // -------------------------
        // Projection last (AuditView)
        // -------------------------

        CommitAuditView view = CommitAuditView.from(commitId, event, request, receipt);
        if (view == null) {
            throw new IllegalStateException("CommitAuditView.from returned null");
        }
        auditViewStore.upsertView(view);
    }


    private String safe(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return null;
        }
        return t;
    }

    private CommitReceipt createReceiptEmitAttempted(String commitId) {
        String id = safe(commitId);
        if (id == null) {
            return null;
        }

        CommitReceipt r = new CommitReceipt();
        r.setCommitId(id);
        r.setAccepted(true);
        r.setWhyCode(new Why("EMIT_ATTEMPTED", null));
        r.setMessage("emit attempted");
        return r;
    }

    private CommitReceipt createReceiptSystemFail(String commitId, String details) {
        String id = safe(commitId);
        if (id == null) {
            return null;
        }

        CommitReceipt r = new CommitReceipt();
        r.setCommitId(id);
        r.setAccepted(false);
        r.setWhyCode(new Why("SYSTEM_ERROR", safe(details)));
        r.setMessage("system error");
        return r;
    }

    private CommitEvent createEvent(CommitRequest request, String commitId) {
        if (request == null) {
            return null;
        }
        String id = safe(commitId);
        if (id == null) {
            return null;
        }

        CommitEvent e = new CommitEvent();
        e.setCommitId(id);
        e.setAttempt(1);
        e.setCreatedAt(nowIsoUtc());
        e.setRequest(request);
        return e;
    }

    private String nowIsoUtc() {
        java.time.Instant now = java.time.Instant.now();
        return java.time.format.DateTimeFormatter.ISO_INSTANT.format(now);
    }


}
