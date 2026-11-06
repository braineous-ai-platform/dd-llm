package io.braineous.dd.llm.query.client;

import ai.braineous.rag.prompt.cgo.api.QueryExecution;
import ai.braineous.rag.prompt.cgo.prompt.PromptBuilder;
import ai.braineous.rag.prompt.cgo.query.CgoQueryPipeline;
import ai.braineous.rag.prompt.cgo.query.QueryRequest;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.braineous.dd.llm.core.model.Why;
import jakarta.enterprise.context.ApplicationScoped;

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
    public QueryResult execute(QueryRequest request) {

        if (request == null) {
            return QueryResult.fail(new Why(WHY_CODE_NULL_REQUEST, WHY_MSG_NULL_REQUEST));
        }

        QueryExecution queryExecution = this.pipeline.execute(request);
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

        JsonElement parsed;
        try {
            parsed = JsonParser.parseString(queryExecution.getRawResponse());
        } catch (JsonSyntaxException e) {
            return QueryResult.fail(
                    request.toJson(),
                    queryExecution.toJson(),
                    new Why(WHY_CODE_RESULT_NOT_SUCCESS, WHY_MSG_RESULT_NOT_SUCCESS)
            );
        }

        if (parsed == null
                || !parsed.isJsonObject()
                || !parsed.getAsJsonObject().has("result")
                || parsed.getAsJsonObject().get("result").isJsonNull()
                || !parsed.getAsJsonObject().get("result").isJsonPrimitive()
                || !parsed.getAsJsonObject().get("result").getAsJsonPrimitive().isString()
                || !"success".equals(parsed.getAsJsonObject().get("result").getAsString())) {
            return QueryResult.fail(
                    request.toJson(),
                    queryExecution.toJson(),
                    new Why(WHY_CODE_RESULT_NOT_SUCCESS, WHY_MSG_RESULT_NOT_SUCCESS)
            );
        }

        return QueryResult.ok(request.toJson(), queryExecution.toJson());
    }
}