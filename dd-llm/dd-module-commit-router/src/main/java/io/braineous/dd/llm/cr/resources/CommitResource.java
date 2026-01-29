package io.braineous.dd.llm.cr.resources;

import ai.braineous.rag.prompt.observe.Console;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;


@Path("/commit")
public class CommitResource {

 @POST
 @Path("/llm/response")
 public void commitLlmResponse(String payload){
  if(payload == null || payload.trim().length() == 0){
   return;
  }

  Console.log("____commit_llm_response____", payload);

   //this.processor.handleSystemFailure(payload);
 }
}

