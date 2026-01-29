package io.braineous.dd.llm.cr.model;

import io.braineous.dd.llm.core.model.Why;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CommitAuditViewTest {

    @Test
    void from_null_or_blank_commitId_returns_null() {
        assertNull(CommitAuditView.from(null, null, null, null));
        assertNull(CommitAuditView.from("", null, null, null));
        assertNull(CommitAuditView.from("   ", null, null, null));
    }

    @Test
    void status_missing_all_when_all_null() {
        CommitAuditView v = CommitAuditView.from("cr_1", null, null, null);
        assertNotNull(v);
        assertEquals(CommitAuditStatus.MISSING_ALL, v.getStatus());
    }

    @Test
    void status_pending_when_receipt_missing_but_some_data_present() {
        CommitRequest r = new CommitRequest();
        r.setQueryKind("qk");
        r.setDecision("ALLOW");

        CommitAuditView v = CommitAuditView.from("cr_2", null, r, null);

        assertNotNull(v);
        assertEquals(CommitAuditStatus.PENDING, v.getStatus());
    }

    @Test
    void completed_accepted_lifts_why_message() {
        CommitReceipt rcpt = new CommitReceipt();
        rcpt.setCommitId("cr_3");
        rcpt.setAccepted(true);
        rcpt.setWhyCode(new Why("CR_OK", "accepted"));
        rcpt.setMessage(" ok ");

        CommitAuditView v = CommitAuditView.from("cr_3", null, null, rcpt);

        assertNotNull(v);
        assertEquals(CommitAuditStatus.COMPLETED_ACCEPTED, v.getStatus());
        assertNotNull(v.getWhyCode());
        assertEquals("CR_OK", v.getWhyCode().getReason());
        assertEquals("accepted", v.getWhyCode().getDetails());
        assertEquals("ok", v.getMessage());
    }

    @Test
    void completed_rejected_when_receipt_present_and_accepted_false() {
        CommitReceipt rcpt = new CommitReceipt();
        rcpt.setCommitId("cr_4");
        rcpt.setAccepted(false);
        rcpt.setWhyCode(new Why("CR_FAIL", "denied"));
        rcpt.setMessage("nope");

        CommitAuditView v = CommitAuditView.from("cr_4", null, null, rcpt);

        assertNotNull(v);
        assertEquals(CommitAuditStatus.COMPLETED_REJECTED, v.getStatus());
        assertEquals("CR_FAIL", v.getWhyCode().getReason());
        assertEquals("denied", v.getWhyCode().getDetails());
        assertEquals("nope", v.getMessage());
    }

    @Test
    void lifts_createdAt_from_event_when_present() {
        CommitEvent ev = new CommitEvent();
        ev.setCommitId("cr_5");
        ev.setCreatedAt("2026-01-28T10:00:00Z");

        CommitAuditView v = CommitAuditView.from("cr_5", ev, null, null);

        assertNotNull(v);
        assertEquals("2026-01-28T10:00:00Z", v.getCreatedAt());
        assertEquals(CommitAuditStatus.PENDING, v.getStatus());
    }
}

