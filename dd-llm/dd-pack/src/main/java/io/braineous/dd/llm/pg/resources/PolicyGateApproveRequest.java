package io.braineous.dd.llm.pg.resources;

public class PolicyGateApproveRequest {

    private String queryKind;
    private String commitId;

    public PolicyGateApproveRequest() {
    }

    public String getQueryKind() {
        return queryKind;
    }

    public void setQueryKind(String queryKind) {
        this.queryKind = queryKind;
    }

    public String getCommitId() {
        return commitId;
    }

    public void setCommitId(String commitId) {
        this.commitId = commitId;
    }
}
