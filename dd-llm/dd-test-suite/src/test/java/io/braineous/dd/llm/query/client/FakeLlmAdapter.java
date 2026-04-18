package io.braineous.dd.llm.query.client;

import ai.braineous.rag.prompt.cgo.api.LlmAdapter;
import ai.braineous.rag.prompt.cgo.query.QueryRequest;
import com.google.gson.JsonObject;

public class FakeLlmAdapter extends LlmAdapter {

    public FakeLlmAdapter() {
        super(new JsonObject());
    }

    @Override
    public String invokeLlm(QueryRequest queryRequest, JsonObject prompt) {
        return null;
    }
}