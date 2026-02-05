package io.braineous.dd.llm.cr.resources;

import ai.braineous.rag.prompt.observe.Console;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.braineous.dd.llm.cr.persistence.CommitSentMongoStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;


@ApplicationScoped
public class CommitConsumer {

    @Inject
    private CommitSentMongoStore store;


    @Incoming("commit_in")
    public void processCommit(String payload){
        Console.log("____aync_commit_success_______", payload);

        JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
        String commitId = json.get("commitId").getAsString();

        try {
            store.append(commitId, payload);
        }catch(Exception e){
            //best-effort for outgoing gated llm packet
        }
    }
}
