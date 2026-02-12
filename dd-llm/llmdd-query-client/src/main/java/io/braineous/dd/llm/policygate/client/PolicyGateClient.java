package io.braineous.dd.llm.policygate.client;

import io.braineous.dd.llm.pg.model.ExecutionView;
import io.braineous.dd.llm.pg.model.PolicyGateResult;

public interface PolicyGateClient {

    ExecutionView getExecutions(String queryKind);

    PolicyGateResult approve(String queryKind, String commitId);
}

