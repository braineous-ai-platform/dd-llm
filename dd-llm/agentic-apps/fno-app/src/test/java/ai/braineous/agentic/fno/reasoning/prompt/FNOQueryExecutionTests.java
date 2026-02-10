package ai.braineous.agentic.fno.reasoning.prompt;

import ai.braineous.agentic.fno.reasoning.query.FNOQueryExecution;
import ai.braineous.rag.prompt.cgo.api.QueryExecution;
import ai.braineous.rag.prompt.cgo.api.ValidateTask;
import ai.braineous.rag.prompt.observe.Console;
import ai.braineous.rag.prompt.utils.Resources;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class FNOQueryExecutionTests {

    @Test
    public void testOrchestrate() throws Exception{
        FNOQueryExecution orchestrator = new FNOQueryExecution();

        String promptStr = Resources.getResource("prompt.json");
        JsonObject promptJson = JsonParser.parseString(promptStr).getAsJsonObject();

        QueryExecution<ValidateTask> execution = orchestrator.executeQuery(promptJson);

        Console.log("execution", execution);
    }

    @Test
    void promptPipeline_executes_and_returnsExecution() {
        // arrange (Java 11 safe string)
        String json =
                "{" +
                        "  \"meta\": {" +
                        "    \"version\": \"v1\"," +
                        "    \"query_kind\": \"FNO_VALIDATE\"," +
                        "    \"description\": \"FNO v1 prompt spine test\"" +
                        "  }," +
                        "  \"context\": {" +
                        "    \"graph\": {}" +
                        "  }," +
                        "  \"task\": {" +
                        "    \"factId\": \"Flight:F102\"" +
                        "  }" +
                        "}";

        JsonObject input = JsonParser.parseString(json).getAsJsonObject();
        Console.log("test.prompt.input", input);

        FNOQueryExecution orchestrator = new FNOQueryExecution();

        // act
        QueryExecution<ValidateTask> execution = orchestrator.executeQuery(input);

        // assert (spine test: just prove execution exists)
        assertNotNull(execution);

        // debug: blackbox commentary
        Console.log("test.execution.raw", execution.getRawResponse());
        Console.log("test.execution.promptValidation", execution.getPromptValidation());
        Console.log("test.execution.llmValidation", execution.getLlmResponseValidation());
        Console.log("test.execution.domainValidation", execution.getDomainValidation());
    }
}
