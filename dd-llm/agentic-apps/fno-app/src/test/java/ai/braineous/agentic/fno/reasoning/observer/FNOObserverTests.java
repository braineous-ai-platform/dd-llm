package ai.braineous.agentic.fno.reasoning.observer;

import ai.braineous.cgo.history.HistoryView;
import ai.braineous.cgo.observer.WhySnapshot;
import ai.braineous.rag.prompt.observe.Console;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class FNOObserverTests {

    @Test
    void observer_returnsHistoryView_forQueryKind() {
        // arrange
        String queryKind = "FNO_VALIDATE";
        Console.log("test.observer.queryKind", queryKind);

        FNOObserver observer = new FNOObserver();

        // act
        WhySnapshot snapshot = observer.getHistory(queryKind);

        // assert (spine test)
        assertNotNull(snapshot);

        // debug
        Console.log("why_snapshot", snapshot.toJson());
    }

}
