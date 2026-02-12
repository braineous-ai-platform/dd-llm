package ai.braineous.agentic.fno.reasoning.governance;

import io.braineous.dd.llm.pg.model.ExecutionView;
import io.braineous.dd.llm.pg.model.PolicyGateResult;
import io.braineous.dd.llm.policygate.client.PolicyGateClient;
import io.braineous.dd.llm.policygate.client.PolicyGateClientImpl;

public class FNOPolicyGate {

    private PolicyGateClient client;

    public FNOPolicyGate() {
        this.client = new PolicyGateClientImpl();
    }

    public FNOPolicyGate(PolicyGateClient client) {
        this.client = client;
    }

    public ExecutionView getExecutions(String queryKind) {
        if (queryKind == null || queryKind.trim().isEmpty()) {
            return null;
        }
        if (this.client == null) {
            return null;
        }
        return this.client.getExecutions(queryKind);
    }

    public PolicyGateResult approve(String queryKind, String commitId) {
        if (queryKind == null || queryKind.trim().isEmpty()) {
            return null;
        }
        if (commitId == null || commitId.trim().isEmpty()) {
            return null;
        }
        if (this.client == null) {
            return null;
        }
        return this.client.approve(queryKind, commitId);
    }
}
