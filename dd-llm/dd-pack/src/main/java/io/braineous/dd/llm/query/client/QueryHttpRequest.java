package io.braineous.dd.llm.query.client;

import java.util.List;

public class QueryHttpRequest {

    private String adapter;       // e.g. "openai"
    private String queryKind;
    private String query;
    private String fact;
    private List<String> relatedFacts;

    public QueryHttpRequest() {
    }

    public String getAdapter() {
        return adapter;
    }

    public void setAdapter(String adapter) {
        this.adapter = adapter;
    }

    public String getQueryKind() {
        return queryKind;
    }

    public void setQueryKind(String queryKind) {
        this.queryKind = queryKind;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getFact() {
        return fact;
    }

    public void setFact(String fact) {
        this.fact = fact;
    }

    public List<String> getRelatedFacts() {
        return relatedFacts;
    }

    public void setRelatedFacts(List<String> relatedFacts) {
        this.relatedFacts = relatedFacts;
    }
}
