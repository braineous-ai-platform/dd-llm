package io.braineous.dd.llm.policygate.client;

import io.braineous.dd.llm.pg.model.ExecutionView;
import io.braineous.dd.llm.pg.model.PolicyGateResult;
import io.braineous.dd.llm.pg.services.PolicyGateOrchestrator;

public class PolicyGateClientImpl implements PolicyGateClient {

    private PolicyGateOrchestrator orch;

    public PolicyGateClientImpl() {
        this.orch = new PolicyGateOrchestrator();
    }

    public PolicyGateClientImpl(PolicyGateOrchestrator orch) {
        this.orch = orch;
    }

    @Override
    public ExecutionView getExecutions(String queryKind) {

        String qk = safe(queryKind);
        if (qk == null) {
            return null;
        }

        if (this.orch == null) {
            return null;
        }

        ExecutionView v = this.orch.getExecutions(qk);
        if (v == null) {
            return null;
        }

        return v;
    }

    @Override
    public PolicyGateResult approve(String queryKind, String commitId) {

        String qk = safe(queryKind);
        if (qk == null) {
            return null;
        }

        String cid = safe(commitId);
        if (cid == null) {
            return null;
        }

        if (this.orch == null) {
            return null;
        }

        PolicyGateResult r = this.orch.approve(qk, cid);
        if (r == null) {
            return null;
        }

        return r;
    }

    // -------------------------
    // helpers
    // -------------------------

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

