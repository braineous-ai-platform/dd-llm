package io.braineous.dd.llm.cr.services;

import io.braineous.dd.llm.cr.model.CommitRequest;
import io.braineous.dd.llm.cr.model.CommitEvent;

public class CommitProcessor {

    public void orchestrate(CommitRequest request){
        try {

            //validate request
            //if fail DLQ-D


            //emit to the downstream topic
            //if fail, DLQ-S

            //update db and corresponding collections
        }catch (Exception e){
            //DLQ-S
        }

    }
}
