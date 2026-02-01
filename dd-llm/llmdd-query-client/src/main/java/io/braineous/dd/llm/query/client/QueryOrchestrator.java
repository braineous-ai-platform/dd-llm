package io.braineous.dd.llm.query.client;

import ai.braineous.rag.prompt.cgo.api.QueryExecution;
import ai.braineous.rag.prompt.cgo.prompt.LlmClient;
import ai.braineous.rag.prompt.cgo.prompt.PromptBuilder;
import ai.braineous.rag.prompt.cgo.query.CgoQueryPipeline;
import ai.braineous.rag.prompt.cgo.query.QueryRequest;
import io.braineous.dd.llm.core.model.Why;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class QueryOrchestrator {

    private static final String ID_PREFIX = "DD-LLM-QUERYORCH";
    private static final String WHY_CODE_NULL_REQUEST = ID_PREFIX + "-FAIL-request_null";
    private static final String WHY_MSG_NULL_REQUEST  = "request cannot be null";

    private static final String WHY_CODE_NULL_EXEC = ID_PREFIX + "-FAIL-execution_null";
    private static final String WHY_MSG_NULL_EXEC  = "queryExecution cannot be null";

    private final CgoQueryPipeline pipeline;

    public QueryOrchestrator() {
        this(new CgoQueryPipeline(new PromptBuilder()));
    }

    QueryOrchestrator(LlmClient llmClient) {
        this(new CgoQueryPipeline(new PromptBuilder(), llmClient));
    }



    // UT seam (boring + legal)
    public QueryOrchestrator(CgoQueryPipeline pipeline) {
        if (pipeline == null) {
            throw new IllegalArgumentException("pipeline cannot be null");
        }
        this.pipeline = pipeline;
    }

    public CgoQueryPipeline getPipeline() {
        return pipeline;
    }

    public QueryResult execute(QueryRequest request) {

        if (request == null) {
            return QueryResult.fail(new Why(WHY_CODE_NULL_REQUEST, WHY_MSG_NULL_REQUEST));
        }

        QueryExecution queryExecution = this.pipeline.execute(request);
        if (queryExecution == null) {
            return QueryResult.fail(new Why(WHY_CODE_NULL_EXEC, WHY_MSG_NULL_EXEC));
        }

        return QueryResult.ok(request.toJson(), queryExecution.toJson());
    }
}