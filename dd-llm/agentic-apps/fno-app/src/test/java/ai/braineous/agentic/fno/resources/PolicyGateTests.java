package ai.braineous.agentic.fno.resources;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class PolicyGateTests {

    @Test
    void observe_missingQueryKind_returns400() {

        String body =
                given()
                        .when()
                        .get("/fno/observe")
                        .then()
                        .statusCode(400)
                        .extract()
                        .asString();

        assertTrue(body.contains("INVALID_REQUEST"));
        assertTrue(body.contains("queryKind must be non-empty"));
    }

    @Test
    void observe_validQueryKind_returns200_andWhySnapshotPresent() {

        String queryKind = "ingest";

        String body =
                given()
                        .queryParam("queryKind", queryKind)
                        .when()
                        .get("/fno/observe")
                        .then()
                        .statusCode(200)
                        .extract()
                        .asString();

        assertTrue(body.contains("\"queryKind\":\"" + queryKind + "\""));
        assertTrue(body.contains("\"why_snapshot\""));
    }

    @Test
    void observe_afterIngest_hasNonEmptyWhySnapshot() {

        String flightsJson =
                "["
                        + "  {"
                        + "    \"flight_id\":\"F1\","
                        + "    \"origin\":\"AUS\","
                        + "    \"destination\":\"DFW\","
                        + "    \"departure_utc\":\"2025-12-17T12:00:00Z\","
                        + "    \"arrival_utc\":\"2025-12-17T13:05:00Z\""
                        + "  }"
                        + "]";


        // seed
        given()
                .contentType("application/json")
                .body(flightsJson)
                .when()
                .post("/fno/ingest")
                .then()
                .statusCode(200);

        // observe
        String body =
                given()
                        .queryParam("queryKind", "ingest")
                        .when()
                        .get("/fno/observe")
                        .then()
                        .statusCode(200)
                        .extract()
                        .asString();

        assertTrue(body.contains("\"why_snapshot\""));
        assertFalse(body.contains("\"why_snapshot\":{}"));
    }

}
