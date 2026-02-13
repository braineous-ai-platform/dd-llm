## Why a Graph Substrate Exists

Large language models are non-deterministic by nature.  
Production systems are not.

Before probabilistic reasoning evaluates a request, the system must define:

- what facts exist
- how those facts relate
- which facts are authoritative
- what constraints apply to state

If context is constructed implicitly through prompt assembly, reproducibility degrades.

If relationships are inferred without structure, validation becomes ambiguous.

If identifiers are unstable, replay becomes impossible.

A deterministic reasoning system requires a stable container for truth.

CGO provides that container.

It defines a typed, validated graph substrate that:

- stores facts by stable identity
- encodes relationships explicitly
- prevents structural ambiguity
- produces replayable snapshots

CGO does not perform approval.  
It does not authorize mutation.  
It does not decide policy.

It ensures that reasoning occurs inside a deterministic structure.

LLM variability is allowed.  
Graph truth is not.

## The Problem CGO Solves

Many LLM systems use retrieval-augmented generation (RAG) to provide context.

In RAG-style architectures, relevant documents or records are retrieved and appended to a prompt before reasoning occurs.

This approach improves contextual relevance.  
It does not enforce structural guarantees.

Retrieved context is typically:

- assembled at query time
- ordered dynamically
- formatted as text
- validated implicitly (if at all)

Two executions with slightly different retrieval ordering or formatting may produce different reasoning surfaces.

This variability is expected in probabilistic systems.

The limitation is not retrieval itself.  
The limitation is the absence of deterministic structural validation.

Production systems often require guarantees such as:

- all referenced entities exist
- relationships are structurally valid
- identifiers are stable
- constraints are enforced before reasoning occurs

Prompt-assembled context cannot validate structural consistency before invocation.

CGO introduces a graph substrate that:

- stores facts as stable, typed objects
- encodes relationships explicitly
- validates structural consistency before reasoning
- produces a deterministic snapshot used for evaluation

The purpose is not to replace retrieval.  
It is to provide a deterministic container in which reasoning occurs.

Retrieval may remain probabilistic.  
The substrate is not.

## Where CGO Fits Inside LLMDD

LLMDD separates reasoning, approval, and mutation into distinct phases.

CGO operates at the foundation of that separation.

Before any LLM reasoning occurs, LLMDD assembles deterministic context from CGO.

This context includes:

- facts stored by stable identifier
- explicit relationships
- validated structural constraints
- a reproducible graph snapshot

The Spine does not construct context dynamically from text.

It consumes a deterministic `GraphContext` provided by CGO.

The high-level flow is:

Query  
→ Graph Substrate (CGO)  
→ Deterministic Context Snapshot  
→ LLM Reasoning (Spine)  
→ Scoring  
→ HistoryRecord  
→ PolicyGate  
→ Commit Path

CGO’s responsibility ends once a validated graph snapshot is produced.

It does not:

- score outcomes
- authorize changes
- perform mutation
- enforce policy

It ensures that reasoning occurs over validated structure.

Within LLMDD, CGO plays two roles:

1. **Graph Substrate** — a persistent, typed truth container
2. **Ingestion Model** — a deterministic mechanism for introducing facts and relationships into that container

The ingestion model guarantees that:

- facts are created with stable identity
- relationships are validated before persistence
- structural invariants are enforced before use

Reasoning depends on ingestion discipline.

If ingestion is non-deterministic, reasoning cannot be reproducible.

CGO ensures the substrate is stable before the Spine begins.

## What CGO Guarantees

CGO does not guarantee correct reasoning.

It guarantees structural correctness before reasoning occurs.

It enforces:

- typed facts
- explicit relationships
- stable identifiers
- validated graph invariants
- reproducible snapshots

To make this concrete, consider the Flight Network Optimization (FNO) domain.

In this model:

- Airports are facts (nodes)
- Flights are relationships (edges)

Each flight connects:

- origin airport
- destination airport
- departure time
- arrival time
- aircraft identifier

Before any LLM evaluates scheduling changes, CGO ensures:

- the origin airport exists
- the destination airport exists
- flight references are valid
- timestamps are structurally valid
- required attributes are present

Structural validation happens before reasoning.

Now consider different queries over the same network:

- “Identify flights where arrival time precedes departure time.”
- “Propose rerouting options to reduce total delay exposure.”
- “Validate aircraft assignment consistency across legs.”

The underlying graph remains the same.  
The query intent varies.

Each query is evaluated against a deterministic snapshot of the graph at a specific point in time.

The LLM produces a proposal or evaluation result based on that snapshot.

That result is then:

- scored deterministically
- recorded as execution truth
- evaluated by PolicyGate for approval
- never mutated implicitly

CGO does not execute SQL-style data filtering.  
It is not a wrapper over a relational database.

Relational databases store and retrieve rows.

CGO provides:

- structured graph context
- deterministic validation
- snapshot isolation for reasoning
- reproducible evaluation envelopes

Reasoning may vary.  
Structure does not.

CGO guarantees that every query — regardless of intent — runs over a validated, stable graph.

It does not guarantee that the LLM’s reasoning is optimal.

It guarantees that reasoning occurs within a disciplined, deterministic substrate.

## Core Substrate Model

CGO models domain state as a graph composed of:

- Facts
- Relationships

These are explicit, typed constructs.

They are not inferred from text.  
They are not generated dynamically at query time.  
They are introduced through a controlled ingestion model.

---

### Facts

A Fact represents a stable domain entity.

Examples (FNO domain):

- Airport
- Aircraft
- Crew
- FlightLeg

Each Fact:

- has a stable identifier
- is typed
- contains structured attributes
- exists independently of any specific query

Facts are nodes in the graph.  
They represent state — not interpretation.

---

### Relationships

A Relationship encodes a structured connection between Facts.

Examples:

- FlightLeg connects Airport A → Airport B
- AircraftAssignment connects Aircraft → FlightLeg
- CrewAssignment connects Crew → FlightLeg

Relationships are:

- explicitly defined
- typed
- validated before persistence
- directional when required

Relationships are part of the deterministic substrate.  
They are not discovered during reasoning.

---

### Graph Snapshot

Reasoning does not operate on a live, mutable graph.

Before evaluation, CGO produces a deterministic snapshot.

A snapshot:

- references specific Fact identities
- references specific Relationship instances
- reflects a structurally consistent state
- is immutable during execution

If ingestion changes occur after snapshot creation, they do not affect the current evaluation.

This provides snapshot isolation for reasoning.

---

### Ingestion Discipline

Facts and Relationships enter the graph through deterministic ingestion.

Ingestion enforces:

- identity stability
- type correctness
- structural validity
- invariant checks

Invalid structure is rejected before reasoning begins.

The substrate is therefore structurally consistent before evaluation.

---

### Proposals and ProposalMonitor

Within CGO, graph mutation is controlled and deterministic.

Rule execution may produce **Proposals** — intended graph changes derived from deterministic business rules.

These proposals do not immediately mutate the graph.

They pass through an internal gate: **ProposalMonitor**.

ProposalMonitor evaluates proposals for:

- structural integrity
- invariant enforcement
- domain constraint compliance
- conflict detection
- consistency preservation

Only when all checks succeed are proposals applied atomically to the graph.

Mental mapping:

ProposalMonitor is to the CGO reasoning kernel  
what a transaction manager is to a relational database.

In a database system:
- constraints are validated
- transactions are committed atomically

In CGO:

- Subsequent queries may therefore produce different reasoning outcomes — not because structure drifted, but because world state changed in real time.
- invariants are validated
- graph mutation is applied atomically

This analogy is conceptual.

CGO is not a relational database engine.  
It does not expose CRUD semantics.  
It is not a general-purpose storage layer.

It is a deterministic reasoning substrate.

Important boundary:

LLM outputs do not mutate the graph.

LLM reasoning operates over a graph snapshot.  
Graph mutation inside CGO occurs only through deterministic rule execution governed by ProposalMonitor.

Substrate mutation is internal consistency management.

System mutation — real-world side effects — is handled downstream by LLMDD through PolicyGate and the Commit Path.

The separation is intentional.

### Not a Cache, Not Static

CGO is not an LLM cache.

It is not a frozen context snapshot stored for reuse.

It is a live, mutating, guarded graph.

The substrate evolves as new facts and relationships are ingested.

Each reasoning execution operates over a deterministic snapshot of that graph at a specific point in time.

Snapshots are immutable during execution.  
The underlying graph is not.

As system data changes through controlled ingestion:

- facts may be added
- relationships may evolve
- structural constraints may be re-evaluated

Subsequent queries may therefore produce different reasoning outcomes — not because structure drifted, but because world state changed.

Determinism does not mean static.

It means:

- each execution runs over a structurally validated snapshot
- each snapshot is reproducible
- each mutation is guarded
- state evolution is controlled

LLM results may vary as ingested system data varies.

The substrate remains consistent.

## Validation Model

Determinism in CGO is enforced through explicit validation phases.

Validation occurs at multiple levels.

---

### 1. Ingestion Validation

Before a Fact or Relationship enters the graph:

- type correctness is verified
- required attributes are present
- referenced identifiers exist
- structural invariants are enforced

Invalid structure is rejected immediately.

Example:

If a `Flight` relationship references:

- origin = "JFK"
- destination = "LHR"

but the `Airport` fact `"LHR"` does not exist in the graph,  
the ingestion is rejected.

The relationship is never persisted.

The graph does not enter a partially valid state.

---

### 2. Snapshot Consistency Validation

Before reasoning begins:

- referenced facts must exist
- relationship bindings must resolve
- graph invariants must hold

A snapshot is produced only if the substrate is structurally consistent.

If constraints fail, execution does not proceed.

Reasoning never occurs over inconsistent state.

---

### 3. Proposal Validation (Internal Mutation Control)

When deterministic rules generate Proposals, they are validated before mutation.

Example:

A rule proposes updating a `Flight` such that:

- arrival_time = 08:00
- departure_time = 09:30

If domain invariants require:

arrival_time ≥ departure_time,

the `ProposalMonitor` rejects the proposal.

No mutation occurs.  
No partial update is applied.

The substrate remains unchanged.

---

### Failure Philosophy

CGO does not attempt to “repair” invalid structure automatically.

It fails deterministically.

Failures are:

- explicit
- reproducible
- attributable to specific invariant violations

The objective is structural truth preservation — not silent correction.

---

### One-line Summary

CGO validation guarantees that reasoning and mutation occur only over structurally consistent graph state.

## Execution Model

CGO executes deterministic rule-based reasoning over a validated graph snapshot.

Execution inside CGO follows a strict lifecycle:

1. Snapshot creation
2. Rulepack binding
3. Rule evaluation
4. Proposal generation
5. Proposal validation
6. Atomic graph mutation (if applicable)

---

### Rulepack Binding

Rulepacks are bound at execution scope.

They are not global adaptive engines.  
They are provided as part of ingestion or evaluation context.

A Rulepack:

- operates over a specific `GraphView` snapshot
- produces deterministic Proposals
- does not mutate the graph directly
- does not perform external side effects

Conceptually:


Rules are pure functions over the snapshot.

Given identical inputs, rule evaluation produces identical proposals.

---

### Single-Pass Deterministic Evaluation

Rule execution is single-pass over the snapshot.

Rules do not depend on previously applied proposals within the same execution cycle.

There are no iterative mutation rounds.

This guarantees that reasoning remains bounded and reproducible.

---

### Concurrent Proposals and Conflict Detection

Multiple rules may produce proposals concurrently.

Conflict detection and resolution are handled centrally by `ProposalMonitor`.

`ProposalMonitor` ensures:

- invariant preservation
- structural consistency
- atomic application of accepted proposals
- deterministic rejection of conflicting proposals

If conflicts exist, mutation does not partially apply.

The graph remains unchanged unless all accepted proposals can be applied consistently.

---

### Deterministic Execution Identity

Execution identity is determined by:

- snapshot identity
- rulepack binding
- input bindings

Given identical snapshot and rulepack inputs, execution behavior is stable.

CGO does not:

- depend on probabilistic behavior
- adapt based on runtime heuristics
- re-evaluate dynamically mid-execution

It executes deterministic logic over deterministic state.

---

### One-line Summary

CGO executes single-pass deterministic rule evaluation over a validated snapshot and applies graph mutation atomically through centralized conflict detection.


## Boundary: CGO Within LLMDD

CGO provides deterministic substrate guarantees.

It answers:

- Is the modeled world structurally consistent?
- Can we construct a stable snapshot for evaluation?
- Can deterministic rule-based mutation be applied safely inside the substrate?

CGO does not answer:

- What should the LLM recommend?
- Whether a recommendation should be approved
- Whether external system state should mutate

Those responsibilities belong to LLMDD layers.

Within LLMDD:

- **CGO** provides `GraphContext` and deterministic snapshots.
- **The Spine** performs non-deterministic reasoning over that context.
- The Spine records execution truth (`HistoryRecord`) and deterministic scoring.
- **PolicyGate** authorizes whether any recommendation is allowed to mutate external systems.
- **The Commit Path** performs approved mutation and records mutation truth (`CommitAuditView`).

The same CGO substrate can support multiple applications over the same domain graph:

- validation queries
- optimization queries
- compliance queries
- simulation queries

The query intent varies.  
The substrate guarantees do not.

CGO ensures structural truth and reproducible snapshots.  
LLMDD ensures authority separation and auditable mutation.


## Design Principles and Constraints

CGO is intentionally constrained.

Its guarantees depend on discipline at the substrate level.

The following principles are non-negotiable.

---

### 1. Deterministic State Before Reasoning

Reasoning must operate over a structurally validated snapshot.

No evaluation occurs over partially valid or implicitly assembled context.

---

### 2. Explicit Structure Over Implicit Text

Facts and relationships are typed and explicitly modeled.

Context is not constructed from loosely formatted prompt text.

---

### 3. No Hidden Mutation

Graph mutation occurs only through deterministic rule execution and `ProposalMonitor`.

There are no side effects during rule evaluation.

---

### 4. Single-Pass, Bounded Execution

Rule evaluation is single-pass over a snapshot.

No iterative mutation loops.
No adaptive re-evaluation mid-execution.

Execution remains bounded and reproducible.

---

### 5. Centralized Conflict Detection

Concurrent proposals are resolved deterministically.

Partial mutation is not allowed.

Either the substrate remains unchanged, or mutation is applied atomically.

---

### 6. Snapshot Isolation

Each execution operates over an immutable snapshot.

Live graph evolution does not affect in-flight evaluation.

---

### 7. Substrate ≠ System Mutation

Graph mutation maintains internal consistency.

External system mutation is handled downstream by LLMDD.

The boundary is strict.

---

### One-line Summary

CGO is a deterministic, structurally disciplined reasoning substrate designed to prevent drift between modeled state and real-world state.







