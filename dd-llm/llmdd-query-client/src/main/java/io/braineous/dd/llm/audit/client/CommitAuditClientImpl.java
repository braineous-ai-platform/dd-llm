package io.braineous.dd.llm.audit.client;

import io.braineous.dd.llm.cr.model.CommitAuditView;
import io.braineous.dd.llm.cr.services.CommitAuditService;

public class CommitAuditClientImpl implements CommitAuditClient {

    private CommitAuditService svc;

    // No default constructor. Force app/runtime to provide a wired service.
    public CommitAuditClientImpl(CommitAuditService svc) {
        this.svc = svc;
    }

    @Override
    public CommitAuditView getAudit(String commitId) {

        String cid = safe(commitId);
        if (cid == null) {
            return null;
        }

        if (this.svc == null) {
            return null;
        }

        CommitAuditView v = null;
        try {
            v = this.svc.getAudit(cid);
        } catch (RuntimeException re) {
            v = null;
        }

        return v;
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



