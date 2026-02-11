package ai.braineous.agentic.fno.reasoning.query;

import ai.braineous.agentic.fno.support.TestGraphSupport;
import ai.braineous.rag.prompt.observe.Console;
import ai.braineous.rag.prompt.utils.Resources;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.braineous.dd.llm.query.client.QueryResult;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
public class FNOQueryExecutionTests {

    @Inject
    private TestGraphSupport support;

    @BeforeEach
    public void setup(){
        support.setupGraphSnapshot();
    }

    @Test
    public void testOrchestrate() throws Exception{
        FNOQueryExecution orchestrator = new FNOQueryExecution();

        String promptStr = Resources.getResource("prompt.json");
        JsonObject promptJson = JsonParser.parseString(promptStr).getAsJsonObject();

        QueryResult result = orchestrator.executeQuery(promptJson);
        JsonObject execution = result.getQueryExecutionJson();

        Console.log("execution", execution);
    }

    @Test
    void promptPipeline_executes_and_returnsExecution() throws Exception{
        String json = Resources.getResource("prompt.json");

        JsonObject input = JsonParser.parseString(json).getAsJsonObject();
        Console.log("test.prompt.input", input);

        FNOQueryExecution orchestrator = new FNOQueryExecution();

        // act
        JsonObject queryJson = JsonParser.parseString(json).getAsJsonObject();
        QueryResult result = orchestrator.executeQuery(queryJson);
        assertNotNull(result);

        JsonObject execution = result.getQueryExecutionJson();

        // assert (spine test: just prove execution exists)
        assertNotNull(execution);

        // debug: blackbox commentary
        Console.log("test.execution.raw", execution.toString());
    }
}
