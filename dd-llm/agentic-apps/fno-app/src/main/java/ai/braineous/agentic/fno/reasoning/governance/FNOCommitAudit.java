package ai.braineous.agentic.fno.reasoning.governance;

import io.braineous.dd.llm.audit.client.CommitAuditClient;
import io.braineous.dd.llm.cr.model.CommitAuditView;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class FNOCommitAudit {

    @Inject
    CommitAuditClient client;

    public CommitAuditView getAudit(String commitId) {

        if (commitId == null || commitId.trim().isEmpty()) {
            return null;
        }

        if (this.client == null) {
            return null;
        }

        return this.client.getAudit(commitId);
    }
}



