# FNO — Flight Network Optimization (Reference Application)

**Location:** `agentic-apps/fno`

---

## Purpose

FNO (Flight Network Optimization) is a reference application used to demonstrate how CGO and LLMDD operate together within a realistic domain.

It is not a production airline optimizer.

It exists to make the architecture concrete.

The examples in `CGO.md` are based on this domain.

---

## Why the Flight Domain?

The flight network domain exposes structural complexity clearly:

- Airports (facts)
- Flights (relationships)
- Aircraft assignments
- Time constraints
- Cascading downstream effects

It is complex enough to demonstrate:

- structural validation
- snapshot isolation
- deterministic rule execution
- advisory reasoning via the Spine
- approval boundaries via PolicyGate

Yet it remains small enough to reason about precisely.

---

## High-Level Flow

FNO demonstrates the full lifecycle across CGO and LLMDD:

```
Domain Facts Ingested
        ↓
Graph Substrate (CGO)
        ↓
Deterministic Snapshot
        ↓
LLM Advisory Reasoning (Spine)
        ↓
Execution Truth (HistoryRecord)
        ↓
PolicyGate Approval
        ↓
Commit Path (Approved Mutation)
        ↓
CommitAuditView (Mutation Truth)
```

The flight network itself does not mutate during advisory reasoning.

Only approved mutation flows through the Commit Path.

---

## What FNO Demonstrates

FNO shows:

1. Deterministic ingestion of domain facts
2. Structural validation of relationships
3. Snapshot-based reasoning
4. Rule-based internal graph mutation (via ProposalMonitor)
5. Separation of advisory reasoning from approved mutation
6. Full authority boundary enforcement

Multiple queries can operate over the same flight network:

- validation queries
- optimization proposals
- compliance checks
- simulation scenarios

The substrate remains stable.  
The intent varies.

---

## What FNO Is Not

FNO is not:

- a scheduling solver
- a machine learning system
- a domain-optimized product

It is a reference surface used to illustrate architectural guarantees.

---

## Relationship to Other Documents

- See `CGO.md` for deterministic substrate guarantees.
- See the `LLMDD README` for full lifecycle and authority separation.

FNO exists to make those abstractions tangible.
