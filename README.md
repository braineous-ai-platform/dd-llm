# LLMDD (Deterministic Decisions for LLM Systems)

![LLMDD Architecture](parallax-image.jpg)

LLMDD is a deterministic decisioning framework for production systems that use large language models.

Large language models are non-deterministic by nature. Production systems are not.

LLMDD introduces a strict deterministic boundary between LLM reasoning and system mutation.  
It ensures every proposed change is:

- explainable
- attributable
- reproducible
- explicitly approved before side effects occur

LLM variability is allowed.  
System truth is not.

## What Problem LLMDD Solves

### Non-deterministic reasoning meets deterministic systems

Large language models are probabilistic reasoning engines.

Enterprise systems are not.

Airlines, banks, logistics networks, healthcare systems — these operate under strict operational constraints.  
Decisions must be traceable. State changes must be attributable. Failures must be explainable.

When probabilistic reasoning directly mutates deterministic systems, the boundary becomes unclear.


### Example: Flight Network Optimization (FNO)

Consider a flight network optimization scenario.

An LLM evaluates route data and proposes:

- rerouting aircraft
- reassigning aircraft to different legs
- delaying or advancing departures
- consolidating or splitting passenger loads

These are not cosmetic changes.

Aircraft are shared assets across multiple routes.  
A single aircraft reassignment can affect several downstream flights.  
A minor delay adjustment on one leg can cascade across the network.

If one probabilistic reasoning outcome selects Aircraft A instead of Aircraft B:

- crew schedules may shift
- downstream connections may be missed
- backup aircraft capacity may be reduced
- operational buffers may disappear

Even a single incorrect routing or delay decision can escalate across the entire network.


### The production risk

In a real airline control environment, a human operator must make the final decision.

The control operator understands context that may not exist in data:
weather nuances, crew fatigue constraints, regional congestion, regulatory pressure, real-time risk posture.

If an LLM system bypasses that authority and directly mutates schedules:

- who approved the change?
- why was this aircraft selected?
- what scoring logic justified the recommendation?
- can the reasoning be reproduced?

If these questions cannot be answered deterministically, trust collapses.


### The missing boundary

The problem is not that LLMs reason probabilistically.

The problem is allowing probabilistic reasoning to directly alter system state.

What is missing in most LLM-enabled architectures is a strict boundary between:

- reasoning
- approval
- mutation

LLMDD exists to enforce that boundary.


## GET STARTED (5 Minutes)

### 1. Clone the repository

Clone the LLMDD repository and enter the project root:

```bash
git clone https://github.com/braineous-ai-platform/dd-llm.git
cd dd-llm
```

### 2. Build and start the local stack

Build the project and start the local runtime using the provided script:

```bash
./run-llmdd.sh
```

This will compile the code and bring up the required Docker services (including MongoDB) for the quick start.


This repository contains the LLMDD runtime, Docker stack, and smoke test scripts used in this quick start.

### 3. Verify the stack is healthy

Confirm the LLMDD runtime is up and responding:

```bash
./health_check.sh
```

A successful response returns:

```
HTTP/1.1 200 OK
{"status":"UP"}
```

This confirms the service is running and ready for the smoke test.

### 4. Run the smoke test (runtime signal check)

Run the provided smoke test script:

```bash
./smoke_test.sh
```

This script exercises the runtime end-to-end at the API surface:

- health check
- query endpoint
- commit audit endpoint

**Note:** In a fresh environment, the query step may return `HTTP 400` if no domain facts have been ingested into the graph substrate yet.  
That is expected in the core LLMDD runtime: the system should fail loudly and deterministically when required context is missing.

A successful health check confirms the stack is running and reachable.

### 5. Verify commit audit truth surface responds

The smoke test also calls a sample commit audit endpoint.

In a fresh environment, you may see:

- `HTTP 404` with `commitId not found`

This confirms the audit surface is reachable and responds deterministically, even when the requested commit does not exist yet.


### 5. What you just observed

You verified that:

- the runtime is alive
- endpoints are reachable
- deterministic execution surfaces respond
- mutation does not occur implicitly
- audit surfaces respond predictably

To see a full successful reasoning + approval + mutation flow with seeded graph data, continue to:

👉 [FNO.md](FNO.md) — Flight Network Optimization reference application  
👉 [CGO.md](CGO.md) — Graph substrate and ingestion model



## Architecture Overview

LLMDD enforces a strict separation between reasoning, approval, and mutation.

Deterministic context is assembled **before** any LLM reasoning occurs.  
LLMDD uses a graph substrate (CGO) to provide a stable, idempotent truth container for LLM queries.

A query flows through clearly defined phases:

```
User / System Query
        │
        ▼
   ┌────────────────────────┐
   │  Graph Substrate (CGO) │
   │ deterministic context  │
   │ + stable fact IDs      │
   └────────────────────────┘
        │
        ▼
   ┌────────────────────┐
   │       Spine        │
   │                    │
   │  ┌──────────────┐  │
   │  │      LLM     │  │
   │  │  reasoning   │  │
   │  └──────────────┘  │
   │                    │
   │ execution + scoring│
   └────────────────────┘
        │
        ▼
   HistoryRecord
   (execution truth)
        │
        ▼
   ┌──────────────┐
   │  PolicyGate  │
   │  approval    │
   └──────────────┘
        │
        ▼
   ┌──────────────┐
   │  Commit Path │
   │  approved    │
   │  mutation    │
   └──────────────┘
        │
        ▼
   CommitAuditView
   (mutation truth)
```

Two separate truth surfaces exist:

- **HistoryRecord** — what the system evaluated and recommended
- **CommitAuditView** — what was actually approved and mutated

No mutation occurs before approval.

## The Spine

### What it is, and why it exists

The Spine is the deterministic core of LLMDD.

It answers a single production question:

How do you safely use non-deterministic LLMs inside systems that must remain explainable, auditable, and reproducible?

The answer is not “better prompts” or “more agents”.  
The answer is a strict deterministic boundary.

The Spine is that boundary.

It plays a role similar to an RDBMS in the early internet era:
not intelligent, not adaptive — but trusted.

Remove the Spine, and you don’t have a system.
You have output.



## The core idea: deterministic interaction, not deterministic intelligence

LLMs are non-deterministic by nature. That’s expected.

The risk is not variability in reasoning.
The risk is allowing that variability to leak into the system record.

Production systems must always be able to answer:

- what happened
- why it happened
- who approved it
- can it be reproduced

If those answers depend on:
“which run”, “which prompt tweak”, “which model mood”, or “which tool call order”
—you do not have a production system.

You have output without accountability.

The Spine makes the interaction surface deterministic,
even when the reasoning engine is not.

Determinism and non-determinism are not enemies.
They must simply be separated by a disciplined boundary.



## What the Spine actually is

The Spine is a small, boring, deterministic query engine whose only superpower is truth hygiene.

It defines a stable, typed contract for query execution.

Every Spine run makes four things explicit:

- **What was asked**  
  (query kind, versioned identity, intent)

- **What context was provided**  
  (graph context, anchors, related facts — not raw prompt text)

  Conceptually, an LLM query is evaluated inside a deterministic container:

  ```
  select(llm_query /* free text */)
  from llm
  where factId = cgo:factId
    and relatedFacts = cgo:[f1, f2, f3, ...]
  ```

- **What the system did**  
  (execution record)

- **What the system recommends**  
  (proposal + score), without performing approved mutation

The output of the Spine is a reproducible execution record — not an action.



## The constitutional rule: no mutation before approval

This rule is non-negotiable.

No tool call or workflow step is allowed to mutate system state before approval.

It does not matter if it “usually works.”
A single uncontrolled mutation is enough to destroy trust.

The Spine is deliberately scoped to:

- propose
- score
- record

Never commit.

Approved mutation belongs downstream — and only downstream.



## Inputs, outputs, and the “truth surface”

### Inputs (conceptual)

The Spine evaluates a query inside a deterministic envelope composed of:

- **Meta**  
  Versioned identity of the query (kind, version, description).  
  Prevents semantic drift between similarly named queries.

- **GraphContext**  
  Deterministic container of facts and relationships.  
  These are stable objects — not prompt blobs.

- **QueryTask**  
  A typed task contract (validate, classify, transform, propose, etc.).  
  The task defines intent, not prose.

- **factId / anchor / relatedFacts**  
  Stable identifiers that bind the query to specific world state.



### Outputs (conceptual)

The Spine produces a deterministic execution truth surface composed of:

- **QueryExecution**  
  Canonical record that a specific query ran with specific inputs.

- **ScorerResult**  
  Deterministic scoring summary (ok / warn / fail, WHY codes).  
  Enables operators and upstream systems to understand outcomes without interpreting raw LLM output.

- **HistoryRecord**  
  The persisted truth surface that downstream components rely on.

Downstream systems should never need to re-run an LLM to understand what happened.  
They should read the record.



## What “deterministic” means here

Deterministic does not mean:
“LLM output will always be identical.”

Deterministic means:

- The record format is stable
- The execution identity is stable
- The scoring rules are stable
- The approval boundary is stable
- The history is append-only and legible

LLM variability is allowed — but it is contained.

It cannot alter system semantics or bypass authority boundaries.



## What the Spine refuses to do

The Spine explicitly refuses to:

- Perform approved mutation
- Hide tool calls behind abstraction magic
- Store opaque blobs as “truth”
- Depend on reflection or internal hacks
- Blur public contracts

If a test requires reflection, the API is wrong.

The Spine is intentionally small so it can be trusted, ported, and reasoned about.



# Core Contracts

Core Contracts define the minimal deterministic surface of the Spine.

They are intentionally boring.  
They exist to make the system legible, testable, and auditable.

If any contract becomes ambiguous, the Spine has failed.

---

## Meta

Meta defines the identity of a query.

It answers:
“What kind of query is this?”

Meta includes:

- stable query kind
- version
- human-readable description

Meta prevents semantic drift.  
If meaning changes, the version changes.

Meta is part of execution identity — not decoration.

---

## QueryTask

QueryTask defines the system’s intent.

It is a typed contract, not free text.

Examples:

- validate
- classify
- transform
- propose

Free-text LLM input may exist inside the task, but the task itself is never ambiguous.

If intent cannot be determined, the task is invalid.

Execution may still proceed, but scoring may degrade or surface warnings.  
Approval authority does not live here.

The Spine records and scores.  
PolicyGate decides.

---

## GraphContext

GraphContext is the deterministic container of facts and relationships relevant to the query.

It is not a prompt.  
It is not prose.  
It is not model-specific.

All facts are referenced by stable IDs.

GraphContext separates:

- what the world is
- how the LLM reasons about it

It is part of the reproducibility contract.

---

## QueryRequest

QueryRequest is the immutable input to the Spine.

It binds:

- Meta
- GraphContext
- QueryTask
- factId / anchor / relatedFacts

Once created, it does not change.

If inputs change, create a new QueryRequest.  
Mutation is forbidden.

---

## QueryExecution

QueryExecution records that a query ran.

It captures:

- the QueryRequest
- execution identity
- timestamps as needed

It establishes that execution occurred.  
It does not imply success or approval.

---

## ScorerResult

ScorerResult is the deterministic evaluation of execution outcome.

It includes:

- status (ok / warn / fail)
- WHY codes
- structured messages

Given the same inputs, scoring must be stable.

If scoring cannot be deterministic, it does not belong in the Spine.

---

## HistoryRecord

HistoryRecord is the persisted execution truth surface.

It combines:

- QueryExecution
- ScorerResult

It is append-only.

Corrections require new records.  
Downstream systems rely on HistoryRecord — not re-execution — to understand what happened.

---

## Contract Boundaries (non-negotiable)

- QueryRequest is immutable
- QueryExecution records execution, not approval
- ScorerResult is deterministic
- HistoryRecord is append-only
- No contract performs approved mutation

Violation of these rules invalidates the design.

---

## Why These Contracts Exist

They enforce three guarantees:

1. **Reproducibility** — same request, same record structure and scoring
2. **Explainability** — “why” is legible without reading raw LLM output
3. **Authority separation** — execution, approval, and mutation remain distinct

---

## One-line summary

Core Contracts define the minimum deterministic surface required to safely embed non-deterministic reasoning inside a production system.

---

# Spine Execution Flow

The Spine executes queries.  
It does not approve actions.  
It does not perform approved mutation.

---

## The Phases

1. Build a deterministic request
2. Execute and score
3. Persist execution truth

---

## Phase 1 — Build the deterministic request

Inputs are assembled into an immutable QueryRequest:

- Meta
- QueryTask
- GraphContext
- fact bindings

The LLM query lives inside this deterministic envelope.

If inputs change, a new QueryRequest is created.

---

## Phase 2 — Execute and score

The Spine evaluates the QueryRequest.

It invokes a configured LLM for reasoning.  
The LLM is pluggable and vendor-agnostic.

Execution produces:

- QueryExecution
- ScorerResult

LLM output may vary.  
Execution identity and scoring boundaries do not.

The Spine records and scores.  
PolicyGate decides.

---

## Phase 3 — Persist execution truth

The Spine writes a HistoryRecord.

It is append-only.  
It represents execution truth.

No approved mutation has occurred at this stage.

---

## Handoff Boundary

Once HistoryRecord is written, the Spine is done.

Execution truth exists.  
Scoring truth exists.

Authority now shifts to PolicyGate.

---

## Failure Handling

Failures are first-class outcomes:

- invalid inputs
- missing facts
- LLM transport errors
- scoring failures

The goal is not “always succeed.”  
The goal is “always explain.”

---

## One-line summary

Spine Execution Flow: build immutable request → execute and score → persist append-only execution truth → hand off to PolicyGate.

---

## How the Spine Fits into LLMDD

Lane discipline is strict:

Spine → HistoryRecord (execution truth)  
PolicyGate → approval decision  
Commit Path → approved mutation  
KafkaDD / DLQ → failure truth if commit execution fails

The Spine is the entry point.  
PolicyGate defines authority.  
The Commit Path is the only place system state changes.

Approved mutation emits explicit commit events for downstream observability.

---

## One-line mental model

The Spine is a deterministic execution and scoring engine that contains non-determinism and prevents mutation before approval.

---

# PolicyGate Approval Flow

PolicyGate is the explicit approval boundary.

The Spine records and scores.  
PolicyGate decides.  
The Commit Path performs approved mutation.

---

## Purpose

PolicyGate answers:

“Should this proposed change be allowed to mutate system state?”

It consumes HistoryRecord and produces an explicit, auditable approval decision.

---

## Inputs

- HistoryRecord
- Deterministic policy rules
- Approval context (who, why, under what authority)

PolicyGate operates on execution truth — not raw LLM output.

---

## Decision Surface

PolicyGate evaluates:

- intent (QueryTask)
- scoring outcome
- proposed mutation payload
- policy constraints

Approval may override weak scoring, but it must be explicit and attributable.

---

## Outputs

- CommitRequest (authorization artifact)
- Optional CommitEvent

PolicyGate authorizes mutation.  
It does not perform it.

---

## Authority and Accountability

Valid approval must answer:

- who approved
- what was approved
- why it was approved
- under what policy
- based on which execution truth

Without this, approval is invalid.

---

## Failure Handling

Failures are explicit:

- missing HistoryRecord
- invalid approver
- policy violations
- malformed payload

There is no “half approval.”

---

## Relationship to Automation

PolicyGate may be human-only or automated.

The contract remains constant:

- approval is explicit
- policy is deterministic
- mutation happens downstream

---

## One-line summary

PolicyGate transforms execution truth into explicit authorization for approved mutation.

---

# Commit Path and Audit Truth Surfaces

The Commit Path applies approved mutation and records mutation truth.

Everything before this point is advisory.  
Everything after this point is accountable.

---

## Purpose

Apply approved mutation.  
Record what actually happened.

---

## Inputs

- CommitRequest
- Execution context

The Commit Path does not reinterpret intent.  
Approval has already occurred.

---

## Commit Execution

The Commit Path performs the mutation described by CommitRequest.

It must be:

- idempotent or safely retryable
- explicit about success or failure

Failures must be recorded.

---

## Commit Artifacts

- CommitEvent — mutation attempt
- CommitReceipt — mutation outcome
- CommitAuditView — aggregated mutation truth surface

---

## CommitAuditView

CommitAuditView answers:

“What actually changed, and why?”

It aggregates:

- approval
- attempt
- outcome

It derives:

- final status
- WHY codes
- timestamps

It is the authoritative mutation truth surface.

---

## Relationship to HistoryRecord

HistoryRecord → execution truth (pre-approval)  
CommitAuditView → mutation truth (post-approval)

Together they form a complete causal chain.

---

## Failure Handling and Retries

Failures:

- produce CommitReceipt
- include WHY codes
- remain visible

Retries must be attributable.  
No silent mutation.

---

## Observability

Approved mutation emits observable events.

Transport is implementation detail.  
Causality and accountability are not.

---

## One-line summary

The Commit Path applies approved mutation and produces an auditable mutation truth surface.

---

# End-to-End Lifecycle (Summary)

LLMDD processes a query as follows:

1. Build deterministic QueryRequest
2. Execute and score via Spine
3. Persist execution truth (HistoryRecord)
4. Evaluate via PolicyGate
5. If approved, perform mutation via Commit Path
6. Persist mutation truth (CommitAuditView)
7. Emit observable commit events

---

## One-line lifecycle summary

query → execution truth → explicit approval → approved mutation → auditable mutation truth



