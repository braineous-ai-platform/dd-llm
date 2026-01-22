package io.braineous.dd.llm.query.services;

import ai.braineous.rag.prompt.cgo.api.QueryExecution;
import ai.braineous.rag.prompt.cgo.prompt.PromptBuilder;
import ai.braineous.rag.prompt.cgo.query.CgoQueryPipeline;

import ai.braineous.rag.prompt.cgo.query.QueryRequest;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class QueryOrchestrator {

    private CgoQueryPipeline pipeline;

    public QueryOrchestrator() {
        this.pipeline = new CgoQueryPipeline(new PromptBuilder());
    }

    public QueryExecution execute(QueryRequest request){

        //execute the query
        QueryExecution queryExecution = this.pipeline.execute(request);

        return queryExecution;
    }
}
