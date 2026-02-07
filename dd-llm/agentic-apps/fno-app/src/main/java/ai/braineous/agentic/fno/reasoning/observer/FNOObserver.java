package ai.braineous.agentic.fno.reasoning.observer;

import ai.braineous.cgo.history.HistoryStore;
import ai.braineous.cgo.history.HistoryView;
import ai.braineous.cgo.observer.Observer;
import ai.braineous.cgo.observer.WhySnapshot;

public class FNOObserver {

    public WhySnapshot getHistory(String queryKind) {
        if (queryKind == null || queryKind.isBlank()) {
            throw new IllegalArgumentException("queryKind must be non-empty");
        }
        Observer observer = new Observer();
        return observer.snapshotForQueryKind(queryKind);
    }
}
