package io.braineous.dd.llm.cr.testsupport;

import io.braineous.dd.llm.cr.model.CommitAuditView;
import io.braineous.dd.llm.cr.services.CommitAuditService;

public class CommitAuditServiceFailing extends CommitAuditService {

    public CommitAuditServiceFailing() {
        super();
    }

    @Override
    public CommitAuditView getAudit(String commitId) {
        throw new RuntimeException("boom");
    }
}

