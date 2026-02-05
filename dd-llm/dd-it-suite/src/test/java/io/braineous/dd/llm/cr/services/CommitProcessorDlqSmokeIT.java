package io.braineous.dd.llm.cr.services;

import ai.braineous.rag.prompt.observe.Console;
import io.braineous.dd.core.model.CaptureStore;
import io.braineous.dd.llm.cr.model.CommitRequest;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@Tag("smoke")
public class CommitProcessorDlqSmokeIT {

    @Inject
    CommitProcessor processor;

    @BeforeEach
    void setup() {
        processor.setAsyncMode(false); // deterministic path
        CaptureStore.getInstance().clear();
        Console.log("CommitProcessorDlqSmokeIT.setup", "cleared CaptureStore");
    }

    @Test
    void validationFail_routes_to_DLQ_D_and_records_capture() {
        Console.log("TEST", "validationFail_routes_to_DLQ_D_and_records_capture");

        CommitRequest req = buildBadRequest("c-bad"); // payload null -> validation fail

        processor.orchestrate(req);

        int n = CaptureStore.getInstance().sizeDomainFailure();
        String first = CaptureStore.getInstance().firstDomainFailure();

        Console.log("dlq_domain.count", n);
        Console.log("dlq_domain.first", first);

        assertEquals(1, n);
        assertNotNull(first);
        assertTrue(first.contains("DD-DLQ-DOMAIN-EXCEPTION"));
    }

    // -------------------------
    // helpers
    // -------------------------

    private CommitRequest buildBadRequest(String commitId) {
        CommitRequest r = new CommitRequest();
        r.setCommitId(commitId);
        r.setQueryKind("audit.commit");
        r.setCatalogVersion("v1");
        r.setActor("system");
        r.setPayload(null);
        return r;
    }
}



