package io.braineous.dd.llm.query.client;

import ai.braineous.rag.prompt.cgo.api.Fact;
import ai.braineous.rag.prompt.cgo.api.GraphContext;
import ai.braineous.rag.prompt.models.cgo.graph.GraphSnapshot;

import java.util.LinkedHashSet;
import java.util.List;

class GraphContextBuilder {

    public GraphContext buildContext(GraphSnapshot snapshot, Fact anchor, List<String> relatedFacts) {

        GraphContext context = new GraphContext();

        if (snapshot == null) {
            return null;
        }
        if (anchor == null) {
            return null;
        }
        if (anchor.getId() == null || anchor.getId().trim().isEmpty()) {
            return null;
        }
        if (relatedFacts == null) {
            relatedFacts = java.util.Collections.emptyList();
        }

        LinkedHashSet<String> seen = new java.util.LinkedHashSet<String>();

        // 1) anchor
        addFactIfNew(context, seen, anchor);

        // 2) neighbors (stable order as provided by snapshot)
        List<Fact> neighbors = snapshot.findNeighbors(anchor.getId());
        if (neighbors != null) {
            for (int i = 0; i < neighbors.size(); i++) {
                Fact f = neighbors.get(i);
                addFactIfNew(context, seen, f);
            }
        }

        // 3) related facts in the caller-provided order
        for (int i = 0; i < relatedFacts.size(); i++) {
            String factId = relatedFacts.get(i);
            if (factId == null || factId.trim().isEmpty()) {
                continue;
            }
            Fact f = snapshot.findFact(factId);
            addFactIfNew(context, seen, f);
        }

        return context;
    }

    private void addFactIfNew(GraphContext context, java.util.Set<String> seen, Fact fact) {
        if (fact == null) {
            return;
        }
        if (fact.getId() == null || fact.getId().trim().isEmpty()) {
            return;
        }
        if (seen.contains(fact.getId())) {
            return;
        }
        seen.add(fact.getId());
        context.addFact(fact);
    }
}

