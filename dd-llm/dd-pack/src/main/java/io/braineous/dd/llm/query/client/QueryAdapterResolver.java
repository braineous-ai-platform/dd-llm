package io.braineous.dd.llm.query.client;

import ai.braineous.cgo.llm.OpenAILlmAdapter;
import ai.braineous.rag.prompt.cgo.api.LlmAdapter;
import com.google.gson.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class QueryAdapterResolver {

    public QueryAdapterResolver() {
    }

    public LlmAdapter resolve(String adapterKey) {

        String key = safe(adapterKey);
        if (key == null) {
            return null;
        }

        if ("openai".equalsIgnoreCase(key)) {
            // stub adapter today; later you can wire config from env/system config
            JsonObject cfg = new JsonObject();
            return new OpenAILlmAdapter(cfg);
        }

        return null;
    }

    private String safe(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return null;
        }
        return t;
    }
}

