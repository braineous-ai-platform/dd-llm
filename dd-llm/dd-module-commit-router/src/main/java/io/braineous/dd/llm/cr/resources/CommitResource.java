package io.braineous.dd.llm.cr.resources;

import ai.braineous.rag.prompt.observe.Console;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;


@Path("/commit")
public class CommitResource {

 @Inject
 @Channel("commit_out")
 Emitter<String> emitter;

 @POST
 @Path("/llm/response")
 public void commitLlmResponse(String payload){
    if(payload == null || payload.trim().length() == 0){
     return;
    }

    Console.log("____commit_llm_response_to_external_system____", payload);

    emitter.send(payload);
 }
}

