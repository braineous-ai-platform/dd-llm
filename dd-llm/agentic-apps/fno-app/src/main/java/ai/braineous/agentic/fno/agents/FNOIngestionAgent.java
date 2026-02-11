package ai.braineous.agentic.fno.agents;

import ai.braineous.agentic.fno.reasoning.ingestion.FNOFactExtractor;
import ai.braineous.agentic.fno.reasoning.ingestion.FNORelationshipProvider;
import ai.braineous.rag.prompt.cgo.api.*;
import ai.braineous.rag.prompt.services.cgo.causal.CausalLLMBridge;
import com.google.gson.JsonArray;

public class FNOIngestionAgent {
    private LLMBridge llmBridge = new CausalLLMBridge();

    public GraphView ingestFlights(JsonArray flightsJsonArray) {
        try {
            LLMContext context = new LLMContext();

            FactExtractor factExtractor = new FNOFactExtractor();
            RelationshipProvider relationshipProvider = new FNORelationshipProvider();

            context.build("flights",
                    flightsJsonArray.toString(),
                    factExtractor,
            relationshipProvider,
                    null);

            // bridge to CGO
            return this.llmBridge.submit(context);

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
