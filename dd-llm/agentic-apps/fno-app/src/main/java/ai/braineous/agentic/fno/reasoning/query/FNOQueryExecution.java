package ai.braineous.agentic.fno.reasoning.query;

import ai.braineous.cgo.llm.OpenAILlmAdapter;
import ai.braineous.rag.prompt.cgo.api.*;

import ai.braineous.rag.prompt.cgo.prompt.PromptBuilder;

import ai.braineous.rag.prompt.cgo.query.CgoQueryPipeline;
import ai.braineous.rag.prompt.cgo.query.QueryRequest;

import ai.braineous.rag.prompt.observe.Console;

import com.google.gson.JsonObject;

public class FNOQueryExecution {

    public QueryExecution<ValidateTask> executeQuery(JsonObject json) {
        if (json == null) {
            throw new IllegalArgumentException("json must not be null");
        }

        // Parse input into task + context
        PromptInput promptInput = new PromptInput().generate(json);
        Meta meta = promptInput.getMeta();
        ValidateTask task = promptInput.getTask();
        GraphContext context = promptInput.getGraphContext();

        if (meta == null || task == null || context == null) {
            throw new IllegalStateException("PromptInput.generate produced null meta/task/context");
        }

        String factId = task.getFactId();
        if (factId == null || factId.isBlank()) {
            throw new IllegalArgumentException("task.factId must be non-empty");
        }

        // v1: adapter config placeholder (keep stable shape, no assumptions)
        JsonObject config = new JsonObject();
        LlmAdapter adapter = new OpenAILlmAdapter(config);

        QueryRequest<ValidateTask> request =
                QueryRequests.validateTask(meta, task, context, factId);
        request.setAdapter(adapter);

        // PromptBuilder (no prompt validator for v1)
        PromptBuilder promptBuilder = new PromptBuilder();

        CgoQueryPipeline pipeline = new CgoQueryPipeline(
                promptBuilder
        );

        QueryExecution<ValidateTask> execution = pipeline.execute(request);

        // inspect
        Console.log("rawResponse", execution.getRawResponse());
        Console.log("promptValidation", execution.getPromptValidation());
        Console.log("llmResponseValidation", execution.getLlmResponseValidation());
        Console.log("domainValidation", execution.getDomainValidation());

        return execution;
    }

}
