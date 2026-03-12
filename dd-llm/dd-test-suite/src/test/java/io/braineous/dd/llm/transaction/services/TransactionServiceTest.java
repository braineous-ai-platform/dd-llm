package io.braineous.dd.llm.transaction.services;

import ai.braineous.rag.prompt.observe.Console;
import io.braineous.dd.llm.pg.model.PolicyGateResult;
import io.braineous.dd.llm.transaction.model.TxExecutionResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TransactionServiceTest {

    @Test
    public void execute_shouldReturnNonNullResult_whenRequestIsNull() {

        TransactionService svc = new TransactionService();

        TxExecutionResult r = svc.execute(null);

        Console.log("ut.tx.service.step1.null_request.result", r.toJson());

        assertNotNull(r);
    }

    @Test
    public void execute_shouldReturnGateFailure_whenRequestIsNull() {

        TransactionService svc = new TransactionService();

        TxExecutionResult r = svc.execute(null);

        Console.log("ut.tx.service.step1.null_request.gate", r.getGateResult() != null ? r.getGateResult().toJsonString() : null);

        assertNotNull(r.getGateResult());

        PolicyGateResult gate = r.getGateResult();
        assertFalse(gate.isOk());
        assertEquals("INVALID_REQUEST", gate.getWhy());
        assertNull(gate.getCommitId());
    }

    @Test
    public void execute_shouldReturnNotApproved_whenRequestIsNull() {

        TransactionService svc = new TransactionService();

        TxExecutionResult r = svc.execute(null);

        Console.log("ut.tx.service.step1.null_request.approved", r.isApproved());

        assertFalse(r.isApproved());
    }

    @Test
    public void execute_shouldReturnEmptyLists_whenRequestIsNull() {

        TransactionService svc = new TransactionService();

        TxExecutionResult r = svc.execute(null);

        Console.log("ut.tx.service.step1.null_request.lists", r.toJson());

        assertNotNull(r.getStepResults());
        assertTrue(r.getStepResults().isEmpty());

        assertNotNull(r.getCommitOrder());
        assertTrue(r.getCommitOrder().isEmpty());
    }
}