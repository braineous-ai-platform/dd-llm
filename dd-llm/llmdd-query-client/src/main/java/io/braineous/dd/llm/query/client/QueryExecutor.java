package io.braineous.dd.llm.query.client;

import ai.braineous.rag.prompt.cgo.query.QueryRequest;

public interface QueryExecutor {

    QueryResult execute(QueryRequest request);
}
