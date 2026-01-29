package io.braineous.dd.llm.core.processor;

public interface HttpPoster {
    int post(String endpoint, String jsonBody) throws Exception;
}
