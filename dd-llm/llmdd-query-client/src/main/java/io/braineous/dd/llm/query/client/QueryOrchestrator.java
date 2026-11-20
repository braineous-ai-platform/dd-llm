package io.braineous.dd.llm.query.client;

import ai.braineous.rag.prompt.cgo.api.Fact;
import ai.braineous.rag.prompt.cgo.api.QueryExecution;
import ai.braineous.rag.prompt.cgo.prompt.PromptBuilder;
import ai.braineous.rag.prompt.cgo.query.CgoQueryPipeline;
import ai.braineous.rag.prompt.cgo.query.QueryRequest;
import ai.braineous.rag.prompt.cgo.querygen.services.QueryResultValidator;
import io.braineous.dd.llm.core.model.Why;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class QueryOrchestrator implements QueryExecutor{

    private static final String ID_PREFIX = "DD-LLM-QUERYORCH";
    private static final String WHY_CODE_NULL_REQUEST = ID_PREFIX + "-FAIL-request_null";
    private static final String WHY_MSG_NULL_REQUEST  = "request cannot be null";

    private static final String WHY_CODE_NULL_EXEC = ID_PREFIX + "-FAIL-execution_null";
    private static final String WHY_MSG_NULL_EXEC  = "queryExecution cannot be null";

    private static final String WHY_CODE_NULL_RAW = ID_PREFIX + "-FAIL-rawResponse_null";
    private static final String WHY_MSG_NULL_RAW  = "rawResponse cannot be null";

    private static final String WHY_CODE_RESULT_NOT_SUCCESS = ID_PREFIX + "-FAIL-result_not_success";
    private static final String WHY_MSG_RESULT_NOT_SUCCESS  = "result is not success";

    private final CgoQueryPipeline pipeline;

    public QueryOrchestrator() {
        this(new CgoQueryPipeline(new PromptBuilder()));
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

    @Override
    public QueryResult execute(
            QueryRequest request,
            Fact axis,
            List<String> relatedFacts,
            String expression,
            String definition) {

        if (request == null) {
            return QueryResult.fail(new Why(WHY_CODE_NULL_REQUEST, WHY_MSG_NULL_REQUEST));
        }

        QueryExecution queryExecution =
                this.pipeline.execute(
                        request,
                        axis,
                        relatedFacts,
                        expression,
                        definition,
                        null,
                        null);
        if (queryExecution == null) {
            return QueryResult.fail(new Why(WHY_CODE_NULL_EXEC, WHY_MSG_NULL_EXEC));
        }

        if (queryExecution.getRawResponse() == null) {
            return QueryResult.fail(
                    request.toJson(),
                    queryExecution.toJson(),
                    new Why(WHY_CODE_NULL_RAW, WHY_MSG_NULL_RAW)
            );
        }

        if (!queryExecution.isOk()) {
            return QueryResult.fail(
                    request.toJson(),
                    queryExecution.toJson(),
                    new Why(WHY_CODE_RESULT_NOT_SUCCESS, WHY_MSG_RESULT_NOT_SUCCESS)
            );
        }

        String resultValue = queryExecution.getRawResponse().trim();
        if (!QueryResultValidator.SUCCESS.equals(resultValue)) {
            return QueryResult.fail(
                    request.toJson(),
                    queryExecution.toJson(),
                    new Why(WHY_CODE_RESULT_NOT_SUCCESS, WHY_MSG_RESULT_NOT_SUCCESS)
            );
        }

        return QueryResult.ok(request.toJson(), queryExecution.toHistoryJson());
    }
}