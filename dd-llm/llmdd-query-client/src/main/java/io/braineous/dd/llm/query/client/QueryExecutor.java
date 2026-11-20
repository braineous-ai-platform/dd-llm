package io.braineous.dd.llm.query.client;

import ai.braineous.rag.prompt.cgo.api.Fact;
import ai.braineous.rag.prompt.cgo.query.QueryRequest;

import java.util.List;

public interface QueryExecutor {

    QueryResult execute(
            QueryRequest request,
            Fact axis,
            List<String> relatedFacts,
            String expression,
            String definition);
}
