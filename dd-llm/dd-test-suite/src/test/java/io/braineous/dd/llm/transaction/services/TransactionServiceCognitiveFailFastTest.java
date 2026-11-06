package io.braineous.dd.llm.transaction.services;

import ai.braineous.rag.prompt.cgo.api.QueryExecution;
import ai.braineous.rag.prompt.cgo.query.CgoQueryPipeline;
import ai.braineous.rag.prompt.cgo.query.QueryRequest;
import ai.braineous.rag.prompt.observe.Console;
import com.google.gson.JsonObject;
import io.braineous.dd.llm.pg.model.TxStepResult;
import io.braineous.dd.llm.pg.services.PolicyGateOrchestrator;
import io.braineous.dd.llm.query.client.QueryClient;
import io.braineous.dd.llm.query.client.QueryOrchestrator;
import io.braineous.dd.llm.query.client.QueryResult;
import io.braineous.dd.llm.transaction.model.TxExecutionRequest;
import io.braineous.dd.llm.transaction.model.TxExecutionResult;
import io.braineous.dd.llm.transaction.model.TxStepRequest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TransactionServiceCognitiveFailFastTest {

    @Test
    public void execute_shouldFailFast_whenMiddleStepCognitiveResultIsFail() {

        Console.log("UT", "execute_shouldFailFast_whenMiddleStepCognitiveResultIsFail");

        CgoQueryPipeline pipeline = mock(CgoQueryPipeline.class);
        QueryRequest request = mock(QueryRequest.class);
        QueryExecution execSuccess = mock(QueryExecution.class);
        QueryExecution execFail = mock(QueryExecution.class);

        JsonObject reqJson = new JsonObject();
        reqJson.addProperty("q", "hello");

        JsonObject successExecJson = new JsonObject();
        successExecJson.addProperty("ok", true);

        JsonObject failExecJson = new JsonObject();
        failExecJson.addProperty("ok", true);

        when(request.toJson()).thenReturn(reqJson);

        when(execSuccess.toJson()).thenReturn(successExecJson);
        when(execSuccess.isOk()).thenReturn(true);
        when(execSuccess.getRawResponse()).thenReturn("{\"result\":\"success\"}");

        when(execFail.toJson()).thenReturn(failExecJson);
        when(execFail.isOk()).thenReturn(true);
        when(execFail.getRawResponse()).thenReturn("{\"result\":\"fail\"}");

        when(pipeline.execute(request)).thenReturn(execSuccess, execFail);

        QueryOrchestrator orch = new QueryOrchestrator(pipeline);

        final List<String> queried = new ArrayList<String>();

        QueryClient queryClient = new QueryClient() {
            @Override
            public QueryResult query(String queryKind, String query, String fact, List<String> relatedFacts) {
                return null;
            }

            @Override
            public QueryResult query(String sql) {
                queried.add(sql);
                return orch.execute(request);
            }
        };

        TransactionService svc = new TransactionService(queryClient, new PolicyGateOrchestrator());

        TxExecutionRequest req = new TxExecutionRequest();

        TxStepRequest s1 = new TxStepRequest();
        s1.setId("s1");
        s1.setDescription("d1");
        s1.setSql("sql1");

        TxStepRequest s2 = new TxStepRequest();
        s2.setId("s2");
        s2.setDescription("d2");
        s2.setSql("sql2");

        TxStepRequest s3 = new TxStepRequest();
        s3.setId("s3");
        s3.setDescription("d3");
        s3.setSql("sql3");

        req.getSteps().add(s1);
        req.getSteps().add(s2);
        req.getSteps().add(s3);

        TxExecutionResult out = svc.execute(req);

        Console.log("UT", out.toJson());

        assertNotNull(out);

        assertEquals(2, queried.size());
        assertEquals("sql1", queried.get(0));
        assertEquals("sql2", queried.get(1));

        assertEquals(2, out.getStepResults().size());
        assertEquals("s1", out.getStepResults().get(0).getId());
        assertEquals("s2", out.getStepResults().get(1).getId());

        TxStepResult step1 = out.getStepResults().get(0);
        assertTrue(step1.isOk());
        assertNotNull(step1.getQueryResult());
        assertTrue(step1.getQueryResult().isOk());
        assertNull(step1.getQueryResult().getWhy());

        TxStepResult step2 = out.getStepResults().get(1);
        assertFalse(step2.isOk());
        assertNotNull(step2.getQueryResult());
        assertFalse(step2.getQueryResult().isOk());
        assertNotNull(step2.getQueryResult().getWhy());
        assertEquals(
                "DD-LLM-QUERYORCH-FAIL-result_not_success",
                step2.getQueryResult().getWhy().reason());

        verify(pipeline, times(2)).execute(request);
    }
}
