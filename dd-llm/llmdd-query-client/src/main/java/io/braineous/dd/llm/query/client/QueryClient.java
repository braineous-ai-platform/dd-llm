package io.braineous.dd.llm.query.client;

import java.util.List;

public interface QueryClient {

    public QueryResult query(String queryKind,
                             String query,
                             String fact,
                             List<String> relatedFacts);

    QueryResult query(String sql);
}
