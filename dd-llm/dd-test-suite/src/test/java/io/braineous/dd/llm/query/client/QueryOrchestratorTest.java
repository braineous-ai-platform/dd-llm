package io.braineous.dd.llm.query.client;

import ai.braineous.rag.prompt.cgo.api.QueryExecution;
import ai.braineous.rag.prompt.cgo.query.CgoQueryPipeline;
import ai.braineous.rag.prompt.cgo.query.QueryRequest;
import ai.braineous.rag.prompt.observe.Console;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class QueryOrchestratorTest {

    @Test
    void execute_null_request_returns_fail_with_why() {
        Console.log("UT", "execute_null_request_returns_fail_with_why");

        CgoQueryPipeline pipeline = mock(CgoQueryPipeline.class);
        QueryOrchestrator orch = new QueryOrchestrator(pipeline);

        QueryResult out = orch.execute(null);

        Console.log("UT", out.toJson());

        assertFalse(out.isOk());
        assertNotNull(out.getWhy());
        assertEquals("DD-LLM-QUERYORCH-FAIL-request_null", out.getWhy().reason());

        verifyNoInteractions(pipeline);
    }

    @Test
    void execute_pipeline_returns_null_execution_returns_fail_with_why() {
        Console.log("UT", "execute_pipeline_returns_null_execution_returns_fail_with_why");

        CgoQueryPipeline pipeline = mock(CgoQueryPipeline.class);
        QueryRequest request = mock(QueryRequest.class);

        when(pipeline.execute(request)).thenReturn(null);

        QueryOrchestrator orch = new QueryOrchestrator(pipeline);
        QueryResult out = orch.execute(request);

        Console.log("UT", out.toJson());

        assertFalse(out.isOk());
        assertNotNull(out.getWhy());
        assertEquals("DD-LLM-QUERYORCH-FAIL-execution_null", out.getWhy().reason());

        verify(pipeline, times(1)).execute(request);
        verifyNoMoreInteractions(pipeline);
    }

    @Test
    void execute_success_returns_ok_with_request_and_execution_json() {
        Console.log("UT", "execute_success_returns_ok_with_request_and_execution_json");

        CgoQueryPipeline pipeline = mock(CgoQueryPipeline.class);
        QueryRequest request = mock(QueryRequest.class);
        QueryExecution execution = mock(QueryExecution.class);

        JsonObject reqJson = new JsonObject();
        reqJson.addProperty("q", "hello");

        JsonObject execJson = new JsonObject();
        execJson.addProperty("tookMs", 7);

        when(request.toJson()).thenReturn(reqJson);
        when(execution.toJson()).thenReturn(execJson);
        when(pipeline.execute(request)).thenReturn(execution);

        QueryOrchestrator orch = new QueryOrchestrator(pipeline);
        QueryResult out = orch.execute(request);

        Console.log("UT", out.toJson());

        assertTrue(out.isOk());
        assertNull(out.getWhy());
        assertNotNull(out.getRequestJson());
        assertNotNull(out.getQueryExecutionJson());

        assertEquals("hello", out.getRequestJson().get("q").getAsString());
        assertEquals(7, out.getQueryExecutionJson().get("tookMs").getAsInt());

        verify(pipeline, times(1)).execute(request);
    }
}

