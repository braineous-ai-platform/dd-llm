package io.braineous.dd.llm.audit.client;

import io.braineous.dd.llm.cr.model.CommitAuditView;

public interface CommitAuditClient {

    CommitAuditView getAudit(String commitId);
}
