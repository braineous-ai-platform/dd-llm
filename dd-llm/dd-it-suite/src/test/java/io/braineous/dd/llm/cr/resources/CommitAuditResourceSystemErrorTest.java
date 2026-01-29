package io.braineous.dd.llm.cr.resources;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.braineous.dd.llm.cr.testsupport.CommitAuditServiceFailing;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CommitAuditResourceSystemErrorTest {

    @Test
    void getAudit_when_service_throws_returns_500_system_error_json() {

        CommitAuditResource r = new CommitAuditResource();

        // svc field is package-private in your resource -> no reflection, no core change
        r.svc = new CommitAuditServiceFailing();

        Response resp = r.getAudit("cr_any");
        assertEquals(500, resp.getStatus());

        String body = (String) resp.getEntity();
        JsonObject o = JsonParser.parseString(body).getAsJsonObject();
        assertEquals("system_error", o.get("error").getAsString());
    }
}
