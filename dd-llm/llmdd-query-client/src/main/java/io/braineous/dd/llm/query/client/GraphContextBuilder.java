package io.braineous.dd.llm.query.client;

import ai.braineous.rag.prompt.cgo.api.Fact;
import ai.braineous.rag.prompt.cgo.api.GraphContext;
import ai.braineous.rag.prompt.models.cgo.graph.GraphSnapshot;

import java.util.List;

class GraphContextBuilder {

    public GraphContext buildContext(GraphSnapshot snapshot, Fact anchor, List<String> relatedFacts){
        GraphContext context = new GraphContext();

        if(snapshot == null || anchor == null || relatedFacts == null){
            return context;
        }

        //add anchor fact
        context.addFact(anchor);

        //add anchor's neighbors
        List<Fact> neighbors = snapshot.findNeighbors(anchor.getId());
        if(neighbors != null) {
            for (Fact fact : neighbors) {
                context.addFact(fact);
            }
        }

        //add related facts
        for(String factId: relatedFacts){
            Fact fact = snapshot.findFact(factId);
            if(fact != null) {
                context.addFact(fact);
            }
        }

        return context;
    }
}
