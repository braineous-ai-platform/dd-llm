package io.braineous.dd.llm.transaction.services;

import ai.braineous.rag.prompt.observe.Console;
import io.braineous.dd.llm.query.client.QueryExecutor;
import io.braineous.dd.llm.query.client.QueryOrchestrator;
import io.braineous.dd.llm.transaction.model.TxExecutionResult;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class TransactionServiceCdiIT {

    @Inject
    QueryExecutor queryExecutor;

    @Inject
    TransactionService transactionService;

    @Test
    void cdi_shouldWireQueryOrchestrator_andPreserveInvalidRequestGate() {

        Console.log("IT", "cdi_shouldWireQueryOrchestrator_andPreserveInvalidRequestGate");

        Assertions.assertNotNull(queryExecutor);
        Assertions.assertTrue(queryExecutor instanceof QueryOrchestrator);
        Console.log("IT", "queryExecutor.class=" + queryExecutor.getClass().getName());

        Assertions.assertNotNull(transactionService);
        Console.log("IT", "transactionService.class=" + transactionService.getClass().getName());

        TxExecutionResult result = transactionService.execute(null);
        Console.log("IT", result != null ? result.toJson() : null);

        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getGateResult());
        Assertions.assertFalse(result.getGateResult().isOk());
        Assertions.assertEquals("INVALID_REQUEST", result.getGateResult().getWhy());
    }
}
