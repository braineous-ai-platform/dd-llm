package ai.braineous.agentic.fno.reasoning.governace;

import ai.braineous.agentic.fno.reasoning.governance.FNOPolicyGate;
import ai.braineous.cgo.observer.WhySnapshot;
import ai.braineous.rag.prompt.observe.Console;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class FNOPolicyGateTests {

   /* @Test
    void observer_returnsHistoryView_forQueryKind() {
        // arrange
        String queryKind = "FNO_VALIDATE";
        Console.log("test.observer.queryKind", queryKind);

        FNOPolicyGate observer = new FNOPolicyGate();

        // act
        WhySnapshot snapshot = observer.getHistory(queryKind);

        // assert (spine test)
        assertNotNull(snapshot);

        // debug
        Console.log("why_snapshot", snapshot.toJson());
    }*/

}
