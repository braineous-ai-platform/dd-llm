package io.braineous.dd.llm.agent.client;

import io.braineous.dd.llm.transaction.model.TxExecutionRequest;
import io.braineous.dd.llm.transaction.model.TxExecutionResult;
import io.braineous.dd.llm.transaction.services.TransactionService;

public class AgentClient {
    private final TransactionService svc;

    public AgentClient() {
        this.svc = new TransactionService();
    }

    AgentClient(TransactionService svc) {
        this.svc = svc;
    }

    public TxExecutionResult execute(TxExecutionRequest request){

        // ---------------------------------------------------------
        // STEP 1 — Execute the transaction (v0)
        // ---------------------------------------------------------
        TxExecutionResult result = this.svc.execute(request);

        return result;
    }
}
