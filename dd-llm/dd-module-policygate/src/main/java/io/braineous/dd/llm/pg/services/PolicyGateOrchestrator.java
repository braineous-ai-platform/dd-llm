package io.braineous.dd.llm.pg.services;

import ai.braineous.cgo.history.HistoryRecord;
import ai.braineous.cgo.history.HistoryView;
import ai.braineous.cgo.history.MongoHistoryStore;
import ai.braineous.rag.prompt.cgo.api.QueryExecution;
import ai.braineous.rag.prompt.cgo.query.QueryRequest;
import io.braineous.dd.llm.cr.model.CommitRequest;
import io.braineous.dd.llm.cr.services.CommitProcessor;
import io.braineous.dd.llm.pg.model.ExecutionView;
import io.braineous.dd.llm.pg.model.PolicyGateResult;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.mongodb.client.MongoClient;

@ApplicationScoped
public class PolicyGateOrchestrator {

    private static final String DEFAULT_ACTOR = "policygate";

    @Inject
    private MongoHistoryStore historyStore;

    @Inject
    CommitProcessor commitProcessor;

    public PolicyGateOrchestrator() {
    }

    // ----------------------------------------------------------------
    // READ: executions
    // ----------------------------------------------------------------

    public ExecutionView getExecutions(String queryKind) {

        ExecutionView out = new ExecutionView();

        String qk = safe(queryKind);
        if (qk == null) {
            out.setQueryKind(null);
            out.setExecutions(java.util.Collections.emptyList());
            return out;
        }

        out.setQueryKind(qk);

        if (this.historyStore == null) {
            out.setExecutions(java.util.Collections.emptyList());
            return out;
        }

        java.util.List<QueryExecution> executions = new java.util.ArrayList<QueryExecution>();

        try {
            HistoryView hv = this.historyStore.findHistory(qk);
            if (hv != null) {

                // NOTE: change this if your HistoryView getter differs.
                java.util.List<HistoryRecord> records = hv.getRecords();

                if (records != null) {
                    for (int i = 0; i < records.size(); i++) {
                        HistoryRecord r = records.get(i);
                        if (r == null) {
                            continue;
                        }

                        QueryExecution<?> qe = r.getQueryExecution();
                        if (qe == null) {
                            continue;
                        }

                        executions.add((QueryExecution) qe);
                    }
                }
            }
        } catch (RuntimeException re) {
            // best-effort: return empty list
        }

        // deterministic order: factId asc (stable). If missing, keep at end.
        java.util.Collections.sort(executions, new java.util.Comparator<QueryExecution>() {
            @Override
            public int compare(QueryExecution a, QueryExecution b) {

                String ida = safeFactIdStatic(a);
                String idb = safeFactIdStatic(b);

                if (ida == null && idb == null) {
                    return 0;
                }
                if (ida == null) {
                    return 1;
                }
                if (idb == null) {
                    return -1;
                }
                return ida.compareTo(idb);
            }
        });

        out.setExecutions(executions);
        return out;
    }

    // ----------------------------------------------------------------
    // COMMAND: approve / reject
    // ----------------------------------------------------------------

    public PolicyGateResult approve(String queryKind, String executionId) {
        return submitDecision(queryKind, executionId, true);
    }

    public PolicyGateResult reject(String queryKind, String executionId) {
        //return submitDecision(queryKind, executionId, false);
        return null;
    }

    private PolicyGateResult submitDecision(String queryKind, String factId, boolean approve) {

        String qk = safe(queryKind);
        if (qk == null) {
            return PolicyGateResult.fail(null, "queryKind is required");
        }

        String eid = safe(factId);
        if (eid == null) {
            return PolicyGateResult.fail(null, "factId is required");
        }

        if (this.historyStore == null) {
            return PolicyGateResult.fail(eid, "historyStore not initialized");
        }

        // Find execution evidence (source of truth)
        QueryExecution<?> exec = null;

        try {
            HistoryView hv = this.historyStore.findHistory(qk);
            if (hv != null) {

                // NOTE: change this if your HistoryView getter differs.
                java.util.List<HistoryRecord> records = hv.getRecords();

                if (records != null) {
                    for (int i = 0; i < records.size(); i++) {
                        HistoryRecord r = records.get(i);
                        if (r == null) {
                            continue;
                        }

                        QueryExecution<?> qe = r.getQueryExecution();
                        if (qe == null) {
                            continue;
                        }

                        String fid = safeFactId(qe);
                        if (fid == null) {
                            continue;
                        }

                        if (fid.equals(eid)) {
                            exec = qe;
                            break;
                        }
                    }
                }
            }
        } catch (RuntimeException re) {
            re.printStackTrace();
            exec = null;
        }

        if (exec == null) {
            return PolicyGateResult.fail(eid, "unknown factId for queryKind");
        }

        CommitRequest cr = null;
        try {
            cr = toCommitRequest(exec, DEFAULT_ACTOR, approve);
        } catch (RuntimeException re) {
            cr = null;
        }

        if (cr == null) {
            return PolicyGateResult.fail(eid, "failed to build CommitRequest");
        }

        // Ensure commitId axis aligns with factId (best-effort)
        try {
            cr.setCommitId(eid);
        } catch (RuntimeException re) {
            // ignore if CommitRequest doesn't have commitId
        }

        if (this.commitProcessor == null) {
            return PolicyGateResult.fail(eid, "commitProcessor not initialized");
        }

        try {
            this.commitProcessor.orchestrate(cr);
            return PolicyGateResult.ok(eid, "submitted");
        } catch (RuntimeException re) {
            return PolicyGateResult.fail(eid, "commit router submission failed");
        }
    }

    // ----------------------------------------------------------------
    // CommitRequest derivation (from execution evidence)
    // ----------------------------------------------------------------

    public static CommitRequest toCommitRequest(QueryExecution exec, String actor, boolean approve) {

        if (exec == null) {
            return null;
        }

        QueryRequest req = null;
        try {
            req = exec.getRequest();
        } catch (RuntimeException re) {
            req = null;
        }

        if (req == null) {
            return null;
        }

        String qk = null;
        try {
            qk = req.safeQueryKind();
        } catch (RuntimeException re) {
            qk = null;
        }
        qk = safeStatic(qk);

        String factId = null;
        try {
            factId = req.safeFactId();
        } catch (RuntimeException re) {
            factId = null;
        }
        factId = safeStatic(factId);

        if (qk == null) {
            return null;
        }
        if (factId == null) {
            return null;
        }

        CommitRequest cr = new CommitRequest();

        cr.setQueryKind(qk);
        cr.setCatalogVersion("v1");


        try {
            cr.setCommitId(factId);
        } catch (RuntimeException re) {
            // ignore if field doesn't exist
        }

        if (actor != null) {
            String a = actor.trim();
            if (!a.isEmpty()) {
                cr.setActor(a);
            }
        }

        com.google.gson.JsonObject payload = new com.google.gson.JsonObject();

        String decision = "REJECT";
        if (approve) {
            decision = "APPROVE";
        }

        payload.addProperty("decision", decision);
        payload.addProperty("factId", factId);
        payload.addProperty("queryKind", qk);

        try {
            payload.add("execution",
                    com.google.gson.JsonParser.parseString(exec.toJsonString()).getAsJsonObject());
        } catch (RuntimeException re) {
            // if execution json is broken, still submit minimal payload
        }

        cr.setPayload(payload);

        return cr;
    }

    // ----------------------------------------------------------------
    // helpers
    // ----------------------------------------------------------------

    private String safeFactId(QueryExecution<?> qe) {
        if (qe == null) {
            return null;
        }
        try {
            QueryRequest<?> req = qe.getRequest();
            if (req == null) {
                return null;
            }
            return safe(req.safeFactId());
        } catch (RuntimeException re) {
            return null;
        }
    }

    private static String safeFactIdStatic(QueryExecution qe) {
        if (qe == null) {
            return null;
        }
        try {
            QueryRequest req = qe.getRequest();
            if (req == null) {
                return null;
            }
            String fid = req.safeFactId();
            return safeStatic(fid);
        } catch (RuntimeException re) {
            return null;
        }
    }

    private String safe(String s) {
        return safeStatic(s);
    }

    private static String safeStatic(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return null;
        }
        return t;
    }
}
