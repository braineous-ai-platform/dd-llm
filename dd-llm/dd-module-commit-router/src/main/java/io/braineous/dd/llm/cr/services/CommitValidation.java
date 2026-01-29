package io.braineous.dd.llm.cr.services;

import io.braineous.dd.llm.core.model.Why;

public class CommitValidation {

    private boolean ok = false; //not_ok by default (security)
    private Why why;

    public CommitValidation(){

    }

    boolean isOk() {
        return ok;
    }


    Why getWhy() {
        return why;
    }

    public static CommitValidation isFail(Why why){
        CommitValidation v = new CommitValidation();
        v.ok = false;
        v.why = why;
        return v;
    }

    public static CommitValidation isOK(){
        CommitValidation v = new CommitValidation();
        v.ok = true;
        return v;
    }
}
