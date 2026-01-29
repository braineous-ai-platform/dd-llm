package io.braineous.dd.llm.cr.services;

import io.braineous.dd.llm.cr.model.CommitAuditView;
import io.braineous.dd.llm.cr.model.CommitEvent;
import io.braineous.dd.llm.cr.model.CommitRequest;
import io.braineous.dd.llm.cr.model.CommitReceipt;
import io.braineous.dd.llm.cr.persistence.CommitEventMongoStore;
import io.braineous.dd.llm.cr.persistence.CommitRequestMongoStore;
import io.braineous.dd.llm.cr.persistence.CommitReceiptMongoStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CommitAuditService {

    @Inject
    private CommitEventMongoStore eventStore;

    @Inject
    private CommitRequestMongoStore requestStore;

    @Inject
    private CommitReceiptMongoStore receiptStore;

    public CommitAuditService() {
    }

    public CommitAuditView getAudit(String commitId) {
        String id = safe(commitId);
        if (id == null) {
            return null;
        }

        CommitEvent event = eventStore.getEvent(id);
        CommitRequest request = requestStore.getRequest(id);
        CommitReceipt receipt = receiptStore.getReceipt(id);

        return CommitAuditView.from(id, event, request, receipt);
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
}
