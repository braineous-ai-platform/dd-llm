package io.braineous.dd.llm.query.client;

import ai.braineous.rag.prompt.cgo.api.QueryExecution;
import ai.braineous.rag.prompt.cgo.prompt.LlmClient;
import ai.braineous.rag.prompt.cgo.prompt.PromptBuilder;
import ai.braineous.rag.prompt.cgo.query.CgoQueryPipeline;
import ai.braineous.rag.prompt.cgo.query.QueryRequest;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

public interface QueryClient {

    public QueryResult query(String queryKind, String query, String fact, List<String> relatedFacts);
}
