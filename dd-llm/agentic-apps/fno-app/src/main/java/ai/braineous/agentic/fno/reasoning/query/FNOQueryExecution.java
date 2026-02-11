package ai.braineous.agentic.fno.reasoning.query;

import ai.braineous.cgo.llm.OpenAILlmAdapter;
import ai.braineous.rag.prompt.cgo.api.*;

import com.google.gson.JsonObject;
import io.braineous.dd.llm.query.client.QueryClient;
import io.braineous.dd.llm.query.client.QueryResult;
import io.braineous.dd.llm.query.client.RESTClient;

import java.util.List;

public class FNOQueryExecution {

    public QueryResult executeQuery(JsonObject json) {
        /***
         * Use this API to execute the foundational BraineousAI Query lane directly.
         *
         * Mental model:
         * Ingestion → Query
         *
         * - Ingestion builds the deterministic graph substrate.
         * - Query executes against that substrate.
         * - The Query pipeline may internally invoke an LLM,
         *   but determinism is enforced through contracts,
         *   validation stages, and structured execution.
         *
         * This path runs in-process without governance/commit.
         * Suitable for foundational graph + query use cases.
         */

        /*if (json == null) {
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

        return execution;*/


        /***
         * Use this API to execute the BraineousAI LLMDD Query lane.
         *
         * Mental model:
         * Ingestion → Query → Governance
         *
         * - Ingestion builds the deterministic graph substrate.
         * - Query executes against that substrate (may invoke an LLM internally).
         * - The result is contract-validated and audit-ready.
         * - Governance (PolicyGate → Commit) is the only path
         *   that allows approved system mutation.
         *
         * Agents can compute and propose,
         * but they cannot mutate truth directly.
         *
         * This path is governance-ready and enterprise-aligned.
         */

        if (json == null) {
            throw new IllegalArgumentException("json must not be null");
        }

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

        // adapter stays demo-stable for now
        JsonObject config = new JsonObject();
        LlmAdapter adapter = new OpenAILlmAdapter(config);

        QueryClient queryClient = new RESTClient();
        String queryKind = meta.getQueryKind();
        String query = task.getDescription();

        //TODO: integrate concept of related_facts in the query pipeline
        List<String> safeRelatedFacts = java.util.Collections.emptyList();
        if (safeRelatedFacts == null) {
            safeRelatedFacts = java.util.Collections.emptyList();
        }



        // ✅ one line: delegate to client
        io.braineous.dd.llm.query.client.QueryResult result = queryClient.query(
                adapter,
                queryKind,
                query,
                factId,
                safeRelatedFacts
                );

        // keep your inspect/logs against result (or map to QueryExecution later)
        return result;
    }

}
