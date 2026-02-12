package ai.braineous.agentic.fno.reasoning.governace;

import ai.braineous.agentic.fno.reasoning.governance.FNOPolicyGate;
import io.braineous.dd.llm.pg.model.ExecutionView;
import io.braineous.dd.llm.pg.model.PolicyGateResult;
import io.braineous.dd.llm.policygate.client.PolicyGateClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;

public class FNOPolicyGateTest {

    // -------------------------------------------------------
    // getExecutions
    // -------------------------------------------------------

    @Test
    void getExecutions_nullQueryKind_returnsNull() {
        FNOPolicyGate pg = new FNOPolicyGate((PolicyGateClient) null);
        Assertions.assertNull(pg.getExecutions(null));
    }

    @Test
    void getExecutions_blankQueryKind_returnsNull() {
        FNOPolicyGate pg = new FNOPolicyGate((PolicyGateClient) null);
        Assertions.assertNull(pg.getExecutions("   "));
    }

    @Test
    void getExecutions_clientNull_returnsNull() {
        FNOPolicyGate pg = new FNOPolicyGate((PolicyGateClient) null);
        Assertions.assertNull(pg.getExecutions("validate_task"));
    }

    @Test
    void getExecutions_happyPath_passthrough() {

        PolicyGateClient fake = new PolicyGateClient() {
            @Override
            public ExecutionView getExecutions(String queryKind) {
                ExecutionView v = new ExecutionView();
                v.setQueryKind(queryKind);
                v.setExecutions(Collections.emptyList());
                return v;
            }

            @Override
            public PolicyGateResult approve(String queryKind, String commitId) {
                return null;
            }
        };

        FNOPolicyGate pg = new FNOPolicyGate(fake);

        ExecutionView v = pg.getExecutions("validate_task");

        Assertions.assertNotNull(v);
        Assertions.assertEquals("validate_task", v.getQueryKind());
        Assertions.assertNotNull(v.getExecutions());
    }

    @Test
    void getExecutions_sameRequestTwice_returnsStableShape() {

        PolicyGateClient fake = new PolicyGateClient() {
            @Override
            public ExecutionView getExecutions(String queryKind) {
                ExecutionView v = new ExecutionView();
                v.setQueryKind(queryKind);
                v.setExecutions(Collections.emptyList());
                return v;
            }

            @Override
            public PolicyGateResult approve(String queryKind, String commitId) {
                return null;
            }
        };

        FNOPolicyGate pg = new FNOPolicyGate(fake);

        ExecutionView v1 = pg.getExecutions("validate_task");
        ExecutionView v2 = pg.getExecutions("validate_task");

        Assertions.assertNotNull(v1);
        Assertions.assertNotNull(v2);

        Assertions.assertEquals(v1.getQueryKind(), v2.getQueryKind());
        Assertions.assertEquals(v1.getExecutions().size(), v2.getExecutions().size());
    }

    // -------------------------------------------------------
    // approve
    // -------------------------------------------------------

    @Test
    void approve_nullQueryKind_returnsNull() {
        FNOPolicyGate pg = new FNOPolicyGate((PolicyGateClient) null);
        Assertions.assertNull(pg.approve(null, "CID"));
    }

    @Test
    void approve_blankQueryKind_returnsNull() {
        FNOPolicyGate pg = new FNOPolicyGate((PolicyGateClient) null);
        Assertions.assertNull(pg.approve("   ", "CID"));
    }

    @Test
    void approve_nullCommitId_returnsNull() {
        FNOPolicyGate pg = new FNOPolicyGate((PolicyGateClient) null);
        Assertions.assertNull(pg.approve("validate_task", null));
    }

    @Test
    void approve_blankCommitId_returnsNull() {
        FNOPolicyGate pg = new FNOPolicyGate((PolicyGateClient) null);
        Assertions.assertNull(pg.approve("validate_task", "   "));
    }

    @Test
    void approve_clientNull_returnsNull() {
        FNOPolicyGate pg = new FNOPolicyGate((PolicyGateClient) null);
        Assertions.assertNull(pg.approve("validate_task", "CID"));
    }

    @Test
    void approve_happyPath_passthrough() {

        PolicyGateClient fake = new PolicyGateClient() {
            @Override
            public ExecutionView getExecutions(String queryKind) {
                return null;
            }

            @Override
            public PolicyGateResult approve(String queryKind, String commitId) {
                PolicyGateResult r = new PolicyGateResult();
                r.setOk(true);
                r.setCommitId(commitId);
                return r;
            }
        };

        FNOPolicyGate pg = new FNOPolicyGate(fake);

        PolicyGateResult r = pg.approve("validate_task", "CID-1");

        Assertions.assertNotNull(r);
        Assertions.assertTrue(r.isOk());
        Assertions.assertEquals("CID-1", r.getCommitId());
    }

    @Test
    void approve_sameRequestTwice_returnsStableResult() {

        PolicyGateClient fake = new PolicyGateClient() {
            @Override
            public ExecutionView getExecutions(String queryKind) {
                return null;
            }

            @Override
            public PolicyGateResult approve(String queryKind, String commitId) {
                PolicyGateResult r = new PolicyGateResult();
                r.setOk(false);
                r.setCommitId(commitId);
                return r;
            }
        };

        FNOPolicyGate pg = new FNOPolicyGate(fake);

        PolicyGateResult r1 = pg.approve("validate_task", "CID-X");
        PolicyGateResult r2 = pg.approve("validate_task", "CID-X");

        Assertions.assertNotNull(r1);
        Assertions.assertNotNull(r2);

        Assertions.assertEquals(r1.isOk(), r2.isOk());
        Assertions.assertEquals(r1.getCommitId(), r2.getCommitId());
    }
}

