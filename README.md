# LLMDD (Deterministic Decisions for LLM Systems)

![This is an image](parallax-image.jpg)

LLMDD is a deterministic decisioning framework and runtime for production systems that use large language models. It separates non-deterministic LLM reasoning from deterministic execution, approval, and audit, ensuring every system change is explainable, attributable, and reproducible.

# The Spine

## What it is, and why it exists

The Spine is the deterministic core of LLMDD.

It exists to solve one problem cleanly:

How do you use non-deterministic LLMs inside production systems that must remain explainable, auditable, and reproducible?

The answer is not “better prompts” or “more agents”.
The answer is a strict deterministic boundary.

The Spine is that boundary.

It plays the same role that an RDBMS played in the early internet era:
not intelligent, not adaptive, but trusted.

If you remove the Spine, you don’t have a system.
You have output.


## The core idea: deterministic interaction, not deterministic intelligence

LLMs are non-deterministic by nature. That’s fine.

The mistake is letting non-determinism leak into the system record.

Production systems must be able to answer:

- what happened
- why it happened
- who approved it
- can it be reproduced

If the answer to any of these depends on:
“which run”, “which prompt tweak”, “which model mood”, or “which tool call order”
—you don’t have a production system.

You have vibes.

The Spine exists to make the interaction surface deterministic, even when the reasoning engine is not.

Determinism and non-determinism are not enemies.
They must simply be separated by a disciplined boundary.


## What the Spine actually is

The Spine is a small, boring, deterministic query engine whose only superpower is truth hygiene.

It defines a stable, typed contract for a query execution:

- What was asked
  (query kind, versioned identity, intent)

- What context was provided  
  (graph context, anchors, related facts — not raw prompt text).  
  These form the deterministic container for an LLM query, where the query itself is free text, conceptually like:
  select(llm_query /* free text */)
  from llm
  where factId = cgo:factId
  and relatedFacts = cgo:[f1, f2, f3, ...]

- What the system did
  (execution record)

- What the system recommends
  (proposal + score), without changing the world

The output of the Spine is a reproducible execution record, not an action.


## The constitutional rule: no side effects before approval

This rule is non-negotiable.

If any tool call or workflow step changes external state before approval,
you’ve recreated LangChain chaos.

It does not matter if it “usually works”.
The first incident will destroy trust.

So the Spine is deliberately scoped to:

- propose
- score
- record

Never commit.

Side effects belong downstream, after approval, and only after approval.


## Inputs, outputs, and the “truth surface”

### Inputs (conceptual)

- Meta  
  Versioned identity of the query (kind, version, description).
  This prevents “same name, different meaning” drift.

- GraphContext  
  Deterministic container of facts and relationships.
  These are stable objects — not prompt blobs.

- QueryTask  
  A typed task contract (validate, classify, transform, propose, etc.).
  The task defines intent, not prose.

- factId / anchor / relatedFacts  
  Stable identifiers tying the query to the world state you care about.


### Outputs (conceptual)

- QueryExecution  
  Canonical record of “this query ran with these inputs”.

- ScorerResult  
  Deterministic scoring summary (ok / warn / fail, WHY codes).
  This is how operators and upstream systems understand results without reading LLM poetry.

- HistoryRecord  
  The stored truth surface that downstream components rely on.

Downstream systems should never need to re-run an LLM to understand what happened.
They should read the record.


## What “deterministic” means here

Deterministic does not mean:
“The LLM output will always be identical.”

Deterministic means:

- The record format is stable
- The execution identity is stable
- The scoring rules are stable
- The approval boundary is stable
- The history is append-only and legible

LLM variability is allowed — but it is contained.
It cannot corrupt system semantics.


## What the Spine refuses to do

The Spine explicitly refuses to:

- Perform side effects
- Hide tool calls behind magic
- Store opaque blobs as “truth”
- Depend on reflection or internal hacks
- Blur public contracts

If a test needs reflection, the API is wrong.

The Spine is intentionally small so it can be trusted, ported, and reasoned about.


## How the Spine fits into LLMDD

The lane discipline is strict:

Spine → HistoryStore (truth surface)
PolicyGate → approval decision (human or automated)
Commit path → side effects
KafkaDD / DLQ → failure truth if commits fail

The Spine is the front door.
PolicyGate is the bouncer.
The commit path is the only place the world changes.

The commit path emits an explicit commit event that represents the only sanctioned state change,
which downstream or upstream systems may observe and act upon.



## One-line mental model

The Spine is a deterministic query record and scoring engine designed to contain non-determinism and prevent side effects before approval.

# Core Contracts

This section defines the minimal set of contracts that make the Spine deterministic.

These contracts are intentionally boring.
They exist to make the system legible, testable, and auditable.

If any of these contracts become ambiguous, the Spine has failed.


## Meta

Meta defines the identity of a query.

It answers a simple question:
“What kind of query is this, exactly?”

Meta includes:
- a stable query kind
- a version
- a human-readable description

Meta exists to prevent semantic drift.

If two queries share a name but differ in meaning, Meta versions must diverge.
If the meaning changes, the version changes.

Meta is not decoration.
It is part of the execution identity.


## QueryTask

QueryTask defines what the system is being asked to do.

It is a typed contract, not free text.

Examples (conceptual):
- validate
- classify
- transform
- propose

QueryTask encodes intent in a way the system can reason about deterministically.

Free-text LLM input may exist inside the task,
but the task itself is never ambiguous.

If the system cannot tell what kind of task it is executing, the task is invalid.

This does not mean the LLM query cannot be executed.
The LLM may still produce output, but the Spine treats it as semantically weak.

In such cases:
- execution may proceed
- scoring may degrade or surface warnings
- determinism of the record is preserved

Crucially, approval authority does not live here.

At PolicyGate, a human (or approved automation) may still choose to approve the commit,
explicitly acknowledging the weaker task semantics,
and mutate the system safely.

This preserves a strict separation:
the Spine records and scores,
PolicyGate decides.



## GraphContext

GraphContext is the deterministic container of facts and relationships relevant to the query.

It is not a prompt.
It is not prose.
It is not model-specific.

GraphContext contains:
- facts
- identifiers
- relationships

All facts are referenced by stable IDs.

This is how the Spine separates:
“what the world is”
from
“how the LLM reasons about it”.

GraphContext is part of the reproducibility contract.


## QueryRequest

QueryRequest is the immutable input to the Spine.

It binds together:
- Meta
- GraphContext
- QueryTask
- factId
- anchor
- relatedFacts

QueryRequest answers:
“What exactly did we ask the system to evaluate?”

Once created, a QueryRequest does not change.

If inputs change, a new QueryRequest must be created.
Mutation is forbidden.


## QueryExecution

QueryExecution is the canonical record that a query was executed.

It captures:
- the QueryRequest
- execution identity
- timestamps or ordering metadata as needed

QueryExecution answers:
“This query ran.”

It does not imply success.
It does not imply approval.
It simply establishes that execution occurred.


## ScorerResult

ScorerResult is the deterministic evaluation of the execution outcome.

It exists so humans and systems do not have to read LLM output to understand results.

ScorerResult includes:
- status (ok / warn / fail)
- WHY codes
- structured messages

ScorerResult is not probabilistic.
Given the same inputs, it must produce the same result.

If scoring cannot be deterministic, it does not belong in the Spine.


## HistoryRecord

HistoryRecord is the stored truth surface of the Spine.

It combines:
- QueryExecution
- ScorerResult

HistoryRecord is append-only.

Once written, it is never mutated or deleted.
Corrections require new records.

Downstream systems must rely on HistoryRecord,
not re-execution or re-prompting,
to understand what happened.


## Contract boundaries (non-negotiable)

- QueryRequest is immutable
- QueryExecution records execution, not approval
- ScorerResult is deterministic
- HistoryRecord is append-only
- No contract performs side effects

If a side effect occurs inside any of these contracts,
the design is invalid.


## Why these contracts exist

These contracts enforce three guarantees:

1) Reproducibility  
   The same request produces the same record shape and scoring outcome.

2) Explainability  
   Humans can answer “why” without reading raw LLM output.

3) Authority separation  
   Execution, approval, and commitment are distinct phases with distinct owners.


## One-line summary

Core Contracts define the minimum deterministic surface required to safely embed non-deterministic reasoning inside a production system.

# Spine Execution Flow

This section describes how the Spine executes a query end-to-end.

The goal is not to explain every implementation detail.
The goal is to make the phase boundaries and responsibility shifts unambiguous.

The Spine executes queries.
It does not approve actions.
It does not mutate the world.


## The phases

A single Spine run has three phases:

1) Build a deterministic request
2) Execute and score
3) Persist the truth surface


## Phase 1 — Build the deterministic request

Inputs are assembled into an immutable QueryRequest:

- Meta identifies the query kind and version
- QueryTask defines the intent (typed contract)
- GraphContext provides the deterministic fact container
- factId / anchor / relatedFacts bind the query to stable IDs

This is the moment where ambiguity must be contained.

The LLM query itself may be free text,
but it always lives inside the deterministic envelope of the request.

If inputs change, create a new QueryRequest.
Mutation is forbidden.


## Phase 2 — Execute and score

The Spine runs the QueryRequest through the query engine.

Execution produces two things:

- QueryExecution (the canonical record that execution occurred)
- ScorerResult (the deterministic evaluation of what happened)

Important boundaries:

- The Spine will call a configured LLM.

An LLM here means a large language model used for reasoning over free-text queries,
for example:
- OpenAI GPT models
- Anthropic Claude models
- Google Gemini models
- locally hosted or self-managed LLMs

The Spine does not depend on a specific vendor or model.
The LLM is a pluggable reasoning engine invoked during execution.

LLM output may vary between runs.
The Spine’s responsibility is to contain that variability
inside a deterministic execution record and scoring boundary.


ScorerResult is how the system remains legible.
Humans and systems should not need to interpret raw LLM text to understand outcomes.

A weak or ambiguous task does not block execution.
It degrades scoring or surfaces warnings,
and pushes authority to the approval boundary.

The Spine records and scores.
PolicyGate decides.


## Phase 3 — Persist the truth surface

The Spine persists a HistoryRecord, which combines:

- QueryExecution
- ScorerResult

HistoryRecord is append-only.

Once written, it is never mutated or deleted.
Corrections require a new record.

This is the Spine’s truth surface.
Downstream components must read this record,
not rerun the LLM,
to determine what happened.


## The handoff boundary: from execution to approval

After HistoryRecord is written, the Spine is done.

At this point:

- execution truth exists
- scoring truth exists
- no side effects have occurred

Approval authority lives outside the Spine.

PolicyGate reads the HistoryRecord and makes an explicit approval decision.

That approval decision is the only bridge between:
non-deterministic reasoning and deterministic system mutation.


## Failure handling (conceptual)

The Spine treats failures as first-class truth.

Examples:
- invalid inputs
- missing facts
- LLM transport errors
- scoring failures

Failures must still produce a legible record whenever possible.

If execution cannot proceed, the system must still be able to explain why.
If scoring cannot be produced deterministically, it does not belong in the Spine.

The goal is not “always succeed”.
The goal is “always explain”.


## One-line summary

Spine Execution Flow is: build an immutable request, execute and score deterministically, persist an append-only truth surface, then hand off authority to PolicyGate for approval.

# PolicyGate Approval Flow

This section defines how LLMDD transitions from execution truth to approved system mutation.

PolicyGate is the explicit approval boundary.
It is the place where authority lives.

The Spine records and scores.
PolicyGate decides.
The commit path mutates the world.


## Purpose

PolicyGate exists to answer one question:

“Should this proposed change be allowed to mutate the world?”

It consumes the Spine’s truth surface (HistoryRecord),
and produces an approval decision that can be audited.


## Inputs

PolicyGate operates on execution truth, not raw LLM output.

Inputs (conceptual):

- HistoryRecord  
  The append-only record of what was executed and how it was scored.

- Policy / rules  
  Deterministic rules that describe what is allowed to be approved.
  These can be simple today (human-only), and automated later.

- Approval context  
  Who is approving, why they are approving, and under what policy.


## Decision surface

PolicyGate evaluates:

- the QueryTask intent (what kind of action is being proposed)
- the ScorerResult (ok / warn / fail + WHY codes)
- the proposal payload (what would change if committed)
- policy constraints (domain rules and safety boundaries)

PolicyGate is allowed to approve even when scoring is weak,
but approval must be explicit and attributable.

This is intentional.
The system is designed to allow human judgment,
while keeping the system mutation boundary deterministic.


## Outputs

PolicyGate produces an approval artifact that downstream systems can act on.

Outputs (conceptual):

- CommitRequest  
  The approved intent to mutate the world, with an explicit approver.

- CommitEvent (optional at this stage)  
  A record that an approved commit was initiated.

PolicyGate does not perform the mutation itself.
It only authorizes it.


## Authority and accountability

PolicyGate is the accountability boundary.

A valid approval must be able to answer:

- who approved
- what they approved
- why they approved
- what policy allowed it
- what execution truth it was based on

If any of these are missing, approval is invalid.

This is how LLMDD keeps a deterministic human thread in the loop.


## Failure handling

PolicyGate failures are not silent.

Examples:
- missing HistoryRecord
- invalid approver identity
- policy rule violation
- malformed proposal payload

Failures must produce legible outcomes.

PolicyGate must never “half approve”.
Either an approval artifact exists, or it does not.


## Relationship to automation

Today, PolicyGate can be human-only.

Over time, PolicyGate can be partially or fully automated,
but the contract does not change:

- approval is explicit
- policy is deterministic
- decisions are attributable
- world mutation happens only downstream


## One-line summary

PolicyGate reads execution truth (HistoryRecord), applies deterministic policy and explicit approval, and emits an authorization artifact (CommitRequest) that is required before any world mutation can occur.

# Commit Path and Audit Truth Surfaces

This section defines how approved intent becomes an actual system mutation,
and how that mutation is recorded as an auditable truth surface.

This is the final phase where the world is allowed to change.

Everything before this point is advisory.
Everything after this point is accountable.


## Purpose

The commit path exists to do exactly one thing:

Apply an approved change to the world,
and record what actually happened.

This phase turns authorization into execution,
and execution into audit truth.


## Inputs

The commit path operates only on approved artifacts.

Inputs (conceptual):

- CommitRequest  
  The authorization artifact emitted by PolicyGate.
  It represents explicit approval to mutate the world.

- Execution context  
  Any required runtime context needed to perform the mutation
  (connectivity, credentials, environment).

The commit path does not read raw LLM output.
It does not reinterpret intent.
Approval has already happened.


## Commit execution

The commit path performs the mutation described by the CommitRequest.

Examples (conceptual):
- updating a record
- invoking an external system
- emitting a domain event
- applying a configuration change

Important boundaries:

- The commit path is allowed to cause side effects.
- It must be idempotent or safely retryable.
- It must surface success or failure explicitly.

If the commit cannot be executed safely,
it must fail loudly and record why.


## Commit artifacts

The commit path produces three core artifacts:

- CommitEvent  
  Records that a commit attempt occurred.
  This represents the intent to execute after approval.

- CommitReceipt  
  Records the outcome of the commit execution.
  This is the authoritative result of the mutation.

- CommitAuditView  
  A read model that aggregates commit event, request, and receipt
  into a single, consumable audit surface.


## CommitAuditView as source of truth

CommitAuditView is the post-approval truth surface.

It answers the question:

“Did the world actually change, and why?”

CommitAuditView aggregates:
- what was approved (CommitRequest)
- what was attempted (CommitEvent)
- what happened (CommitReceipt)

It also derives:
- final status (pending / accepted / rejected)
- WHY codes
- human-readable messages
- timestamps

CommitAuditView is the authoritative read model
for operators, auditors, and downstream systems.


## Relationship to HistoryRecord

HistoryRecord and CommitAuditView are sibling truth surfaces.

- HistoryRecord  
  Execution truth (pre-approval).
  Answers: “What did the system evaluate and recommend?”

- CommitAuditView  
  Mutation truth (post-approval).
  Answers: “What actually changed in the world?”

Neither replaces the other.
Together, they form a complete causal chain.


## Failure handling and retries

Failures in the commit path are first-class outcomes.

Examples:
- external system unavailable
- validation failure at execution time
- partial execution detected
- retry exhaustion

Failures must:
- produce a CommitReceipt
- populate WHY codes and messages
- be visible via CommitAuditView

Silent failure is not allowed.

Retries, if any, must not erase the original outcome.
Each attempt must be attributable.


## Observability and eventing

Commit execution may emit explicit commit events
that upstream or downstream systems can observe and consume.

This enables:
- asynchronous processing
- integrations (e.g. Kafka-based pipelines)
- replay and compensation workflows

Event transport is an implementation detail.
The contract is that commit outcomes are observable.


## One-line summary

The Commit Path applies approved changes to the world and produces an auditable truth surface (CommitAuditView) that definitively answers what was


# End-to-End Lifecycle (Summary)

This section provides a condensed, end-to-end view of how a query flows through LLMDD.

It is a summary layer.
No new concepts are introduced here.

The purpose is to help readers mentally trace a single request
from initial query to audited world mutation.


## Step 1 — Query enters the system

A query is initiated with:
- Meta (query identity and version)
- QueryTask (typed intent)
- GraphContext (deterministic fact container)
- factId / anchor / relatedFacts (stable identifiers)
- free-text LLM query (reasoning input)

These inputs are assembled into an immutable QueryRequest.

At this point:
- nothing has executed
- nothing has been approved
- nothing has changed


## Step 2 — Spine execution

The Spine executes the QueryRequest.

During execution:
- the configured LLM is invoked for reasoning
- output may vary
- execution identity remains stable

The Spine produces:
- QueryExecution
- ScorerResult

No side effects are allowed.


## Step 3 — Execution truth is persisted

The Spine persists a HistoryRecord.

This record is:
- append-only
- deterministic in structure
- independent of LLM variability

HistoryRecord is the system’s execution truth surface.


## Step 4 — Approval decision

PolicyGate reads the HistoryRecord.

Based on:
- query intent
- scoring outcome
- policy rules
- approver judgment

PolicyGate either:
- emits a CommitRequest (approved), or
- emits nothing (rejected)

Approval is explicit and attributable.


## Step 5 — Commit execution

If approved, the commit path executes the CommitRequest.

This is the only phase where:
- side effects occur
- the world is allowed to change

The commit path produces:
- CommitEvent
- CommitReceipt


## Step 6 — Audit truth is produced

Commit artifacts are aggregated into a CommitAuditView.

This view answers definitively:
- what was approved
- what was attempted
- what actually happened
- why it succeeded or failed

CommitAuditView is the post-approval truth surface.


## Step 7 — Observation and integration

Commit outcomes may be emitted as observable events.

Downstream or upstream systems can:
- react asynchronously
- integrate via event streams
- perform replay or compensation

Transport and tooling are implementation details.
Causality and accountability are not.


## One-line lifecycle summary

LLMDD flows as:
query → deterministic execution truth → explicit approval → world mutation → auditable commit truth.



