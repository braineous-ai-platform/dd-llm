## 1. Why the AI Transaction Runtime Exists

The Spine evaluates LLM queries inside deterministic system boundaries.

Before reasoning begins, LLMDD assembles a deterministic `GraphContext` from CGO.  
This context provides stable fact identities, explicit relationships, and a validated structural snapshot of the modeled world.

Once this context exists, the Spine must execute reasoning in a disciplined and reproducible way.

Direct prompt execution is not sufficient for production systems.

If reasoning is triggered by ad-hoc prompt assembly:

- execution boundaries become unclear
- decision identity becomes difficult to trace
- reasoning context may drift across runs
- downstream approval and mutation flows lose accountability

A deterministic reasoning system requires a controlled runtime envelope in which LLM queries execute.

The **AI Transaction Runtime** provides that envelope.

It acts as a **transient transaction container** inside the Spine.  
Within this container, one or more LLM queries can execute against a deterministic context snapshot.

The runtime coordinates:

- query execution
- deterministic scoring
- execution identity creation
- recording of execution truth

The runtime does **not**:

- approve system changes
- perform mutation
- enforce policy
- distribute events

Those responsibilities belong to downstream components in the LLMDD lifecycle.

The runtime exists only long enough to coordinate query execution and produce an execution record.

Once execution truth is recorded as a `HistoryRecord`, the transaction container dissolves and authority shifts to downstream governance and mutation systems.

Conceptually:

```
GraphContext (CGO)
        ↓
AI Transaction Runtime (Spine)
        ↓
HistoryRecord (execution truth)
        ↓
PolicyGate (approval)
        ↓
Commit Path (approved mutation)
        ↓
Kafka (distribution boundary)
```

The purpose of the runtime is therefore narrow and deliberate:

to contain non-deterministic reasoning inside a deterministic execution boundary before authority and mutation decisions occur.

## 2. Where the Runtime Fits

The AI Transaction Runtime operates inside the **Spine**, which is the deterministic execution boundary of LLMDD.

CGO provides a deterministic graph substrate.  
The Spine evaluates queries against that substrate.  
The AI Transaction Runtime is the mechanism the Spine uses to execute those queries in a controlled and traceable way.

The runtime sits between **deterministic context assembly** and **execution truth persistence**.

High-level placement within the LLMDD lifecycle:

```
Domain Facts
        ↓
Graph Substrate (CGO)
        ↓
Deterministic GraphContext Snapshot
        ↓
Spine
   ↓
AI Transaction Runtime
   ↓
QueryExecution + ScorerResult
        ↓
HistoryRecord (execution truth)
        ↓
PolicyGate (approval authority)
        ↓
Commit Path (approved mutation)
        ↓
Kafka (system mutation boundary)
```

Within this sequence:

- **CGO** guarantees structural correctness of the world model.
- **The Spine** governs deterministic execution discipline.
- **The AI Transaction Runtime** coordinates LLM query execution inside a transient transaction container.
- **HistoryRecord** becomes the durable execution truth surface.
- **PolicyGate** determines whether proposed actions may mutate system state.
- **The Commit Path** performs approved mutation.
- **Kafka** provides the observable mutation boundary for downstream systems.

The runtime therefore sits at a precise point in the architecture:

between **deterministic context construction** and **durable execution truth**.

It does not assemble graph context.  
It does not decide approval.  
It does not mutate system state.

Its responsibility is narrow and deliberate:

execute LLM queries within a disciplined runtime boundary and produce an auditable execution record for downstream authority systems.

## 3. The Transaction Container

The AI Transaction Runtime executes queries inside a **transient transaction container**.

The purpose of the container is not persistence.  
It exists to coordinate execution boundaries.

Even a single query executes inside this container.

Conceptually:

```
TX
 └── LLM Query
```

Multiple queries may also execute within the same container when orchestration requires it:

```
TX
 ├── LLM Query
 ├── LLM Query
 └── LLM Query
```

The container ensures that all reasoning steps operate within a consistent execution scope.

Within the transaction container, the runtime coordinates:

- query execution
- scoring
- execution identity generation
- creation of execution truth artifacts

The container itself is **not persisted**.

It does not become part of the system record.

The durable artifact produced by execution is the `HistoryRecord`, which combines:

- `QueryExecution`
- `ScorerResult`

Once `HistoryRecord` is written, the runtime container dissolves.

```
AI Transaction Runtime (TX)
        ↓
QueryExecution
        ↓
ScorerResult
        ↓
HistoryRecord
        ↓
TX dissolves
```

Downstream systems operate on the execution truth that remains.

The container exists only long enough to ensure that LLM reasoning occurs inside a controlled and traceable execution boundary.

This design preserves a critical property of LLMDD:

non-deterministic reasoning is contained inside a deterministic execution envelope.

## 4. Query as the Primitive

Within the AI Transaction Runtime, the primary unit of reasoning is the **query**.

Prompts still exist.  
However, prompts are treated as an internal execution mechanism rather than the system-facing abstraction.

Traditional LLM integrations expose prompts directly as the operational boundary:

```
Prompt → Model → Response
```

In LLMDD, the system interacts with LLM reasoning through structured **queries** instead:

```
Query → PromptBuilder → LLM → Execution
```

The query represents the reasoning task evaluated by the Spine.  
Prompt construction remains an implementation detail handled inside the runtime.

This shift allows the system to reason about LLM execution in a disciplined and reproducible way.

Conceptually, an LLM query can be viewed using a lightweight SQL mental model:

```
select(llm_query /* free text */)
from llm
where factId = cgo:factId
  and relatedFacts = cgo:[f1, f2, f3, ...]
```

In this framing:

- **llm_query** represents the reasoning task expressed in natural language.
- **factId** anchors the query to a primary fact within the CGO graph.
- **relatedFacts** provide additional structured context drawn from the deterministic graph snapshot.

This model does **not** introduce a new query language.

It provides a conceptual way to understand LLM reasoning as a structured evaluation over deterministic facts rather than an isolated prompt invocation.

The runtime then translates the query into the concrete prompt sent to the model.

```
Query
  ↓
PromptBuilder
  ↓
LLM
  ↓
QueryExecution
```

By treating queries as the primitive rather than prompts, LLMDD maintains a clear separation between:

- **deterministic system context**
- **probabilistic reasoning execution**

This separation allows LLM reasoning to remain flexible while preserving deterministic system guarantees.

## 5. Execution Identity

Every query executed inside the AI Transaction Runtime produces a stable execution identity.

This identity is represented by the `QueryExecution` record.

`QueryExecution` establishes that a specific query ran against a specific deterministic context.

It captures:

- the immutable `QueryRequest`
- execution identity (`execution_id`)
- execution timestamps as required

Conceptually:

```
QueryRequest
      ↓
QueryExecution
```

The execution identity becomes the anchor that downstream systems use to understand what occurred.

While the transaction container is transient, the execution identity persists.

```
AI Transaction Runtime (TX)
        ↓
QueryExecution (execution_id)
        ↓
ScorerResult
        ↓
HistoryRecord
```

The runtime container dissolves after execution truth is recorded, but the `execution_id` continues to travel through the remainder of the system lifecycle.

Downstream components reference this identity to maintain traceability:

- **HistoryRecord** preserves execution truth
- **PolicyGate** evaluates approval decisions based on execution results
- **Commit Path** records mutation attempts and outcomes
- **CommitAuditView** aggregates mutation truth

Because `QueryExecution` is part of the deterministic Spine contracts, execution identity must remain stable and unambiguous.

Execution identity allows the system to answer essential production questions:

- what query ran
- what context it used
- when execution occurred
- what outcome was produced

LLM output may vary across executions.

Execution identity and execution truth surfaces do not.

## 6. Runtime Lifecycle

The AI Transaction Runtime executes queries inside a disciplined and bounded lifecycle.

The lifecycle begins once a deterministic `QueryRequest` enters the Spine and ends when execution truth is recorded.

Execution proceeds through the following stages.

### 1. Receive QueryRequest

Execution begins with an immutable `QueryRequest`.

A `QueryRequest` binds together:

- `Meta` (query identity and version)
- `QueryTask` (typed intent)
- `GraphContext` (deterministic snapshot from CGO)
- fact bindings (`factId`, anchors, related facts)

The request defines the full deterministic envelope in which reasoning will occur.

### 2. Begin Runtime Container

The AI Transaction Runtime creates a transient execution container.

This container establishes the execution scope for one or more queries and ensures that all reasoning steps operate within a consistent boundary.

```
TX
 └── QueryExecution
```

The container is not persisted and does not become part of system state.

### 3. Execute LLM Reasoning

The runtime invokes the configured LLM through the internal execution pipeline.

Conceptually:

```
Query
  ↓
PromptBuilder
  ↓
LLM
  ↓
Execution Result
```

The prompt is constructed internally based on the query and deterministic context.

The model produces a reasoning result which is then evaluated by deterministic scoring rules.

### 4. Produce Execution Artifacts

Execution generates the core deterministic artifacts defined by the Spine:

- `QueryExecution`
- `ScorerResult`

These artifacts represent what the system executed and how the outcome was evaluated.

### 5. Persist Execution Truth

The runtime combines execution artifacts into a durable execution truth surface.

```
QueryExecution
      +
ScorerResult
      ↓
HistoryRecord
```

The `HistoryRecord` is written as an append-only record that downstream systems can rely on.

This record represents the complete execution truth for the query.

### 6. End Runtime Responsibility

Once the `HistoryRecord` is written, the responsibility of the AI Transaction Runtime ends.

At this point:

- execution truth exists
- scoring outcomes exist
- the system has a stable record of what occurred

Authority for further action shifts to downstream components.

PolicyGate evaluates approval decisions.

The Commit Path performs mutation only when explicit approval occurs.

The runtime itself does not participate in approval or mutation.

Its lifecycle is complete once execution truth has been recorded.

## 7. Runtime Dissolution

The AI Transaction Runtime exists only for the duration of query execution.

Once execution truth has been recorded, the transaction container dissolves.

The runtime container is intentionally **transient**.  
It is not persisted, stored, or referenced as a durable system artifact.

Conceptually:

```
AI Transaction Runtime (TX)
        ↓
QueryExecution
        ↓
ScorerResult
        ↓
HistoryRecord
        ↓
TX dissolves
```

The system record begins with `HistoryRecord`.

All downstream components operate on execution truth rather than runtime state.

This design preserves a strict separation between:

- **execution coordination**, which is temporary
- **execution truth**, which is durable

The transaction container exists only to ensure that reasoning occurs inside a controlled execution boundary.

Once that boundary has produced a durable execution record, the container no longer serves a purpose and disappears from the system lifecycle.

The surviving artifacts are:

- `QueryExecution`
- `ScorerResult`
- `HistoryRecord`

These artifacts provide the deterministic truth surface required for approval, auditing, and mutation control.

Downstream systems therefore interact with the results of execution rather than the runtime environment in which execution occurred.

This keeps the system record simple, traceable, and reproducible.

The runtime coordinates reasoning.

The record preserves truth.

## 8. Downstream Authority Flow

Once the AI Transaction Runtime records execution truth, authority shifts to downstream components.

The runtime itself does not determine whether a proposed outcome should mutate system state.

Its responsibility ends after producing a `HistoryRecord`.

Downstream governance systems operate on this execution truth surface.

The next stage in the lifecycle is **PolicyGate**.

PolicyGate evaluates whether a proposed change is allowed to proceed toward system mutation.

Inputs to PolicyGate include:

- the `HistoryRecord`
- deterministic policy rules
- approval context (operator or automated authority)

PolicyGate evaluates:

- the intent of the query (`QueryTask`)
- the deterministic scoring outcome (`ScorerResult`)
- the proposed mutation payload
- policy constraints that govern the system

The result of this evaluation is an explicit approval decision.

Conceptually:

```
HistoryRecord
        ↓
PolicyGate
        ↓
CommitRequest (authorization artifact)
```

Approval does not mutate system state directly.

Instead, PolicyGate produces a **CommitRequest**, which authorizes downstream components to perform mutation.

If approval is denied, the lifecycle ends with execution truth recorded but no mutation applied.

If approval is granted, authority moves to the Commit Path.

This separation ensures that:

- reasoning
- approval
- mutation

remain distinct phases in the system lifecycle.

The AI Transaction Runtime participates only in the reasoning phase.

Approval authority belongs exclusively to PolicyGate.

## 9. Kafka as the Final Mutation Boundary

Once PolicyGate authorizes a proposed change, the system moves into the mutation phase.

Mutation does not occur inside the Spine or the AI Transaction Runtime.  
It occurs downstream through the **Commit Path**.

The Commit Path receives a `CommitRequest` produced by PolicyGate and performs the approved system mutation.

Conceptually:

```
HistoryRecord
        ↓
PolicyGate
        ↓
CommitRequest
        ↓
Commit Path
        ↓
CommitEvent
        ↓
Kafka
```

The Commit Path is responsible for executing the authorized mutation and producing observable mutation artifacts.

These artifacts include:

- `CommitEvent` — the mutation attempt
- `CommitReceipt` — the outcome of the mutation
- `CommitAuditView` — the aggregated mutation truth surface

Kafka acts as the **system mutation boundary**.

Approved mutations are emitted as events into Kafka, where downstream systems can consume them as authoritative system updates.

Kafka therefore provides:

- an observable distribution boundary
- replayable mutation history
- integration with downstream consumers
- deterministic event propagation

The AI Transaction Runtime does not participate in this stage.

Its role ends once execution truth is recorded and authority transitions to PolicyGate.

From that point forward, mutation and distribution occur entirely within the Commit Path and event infrastructure.

This separation ensures that non-deterministic reasoning remains isolated from system mutation, preserving the deterministic guarantees required for production systems.

## 10. End-to-End Lifecycle (Summary)

The AI Transaction Runtime operates within the Spine as the controlled execution boundary for LLM reasoning.

Its responsibility is limited to coordinating query execution and producing deterministic execution truth.

The full LLMDD lifecycle proceeds as follows:

```
Domain Facts
        ↓
Graph Substrate (CGO)
        ↓
Deterministic GraphContext Snapshot
        ↓
Spine
   ↓
AI Transaction Runtime
   ↓
QueryExecution + ScorerResult
        ↓
HistoryRecord (execution truth)
        ↓
PolicyGate (approval authority)
        ↓
Commit Path (approved mutation)
        ↓
Kafka (system mutation boundary)
```

Within this lifecycle:

- **CGO** guarantees structural truth and reproducible graph snapshots.
- **The Spine** executes reasoning inside deterministic contracts.
- **The AI Transaction Runtime** coordinates LLM query execution inside a transient transaction container.
- **HistoryRecord** preserves execution truth.
- **PolicyGate** determines whether proposed actions are authorized.
- **The Commit Path** performs approved mutation.
- **Kafka** provides the observable mutation boundary for downstream systems.

The runtime therefore represents a narrow but critical phase in the system.

It ensures that non-deterministic reasoning occurs inside a disciplined execution boundary before authority and mutation decisions are made.

LLM reasoning may vary.

Execution truth, approval authority, and mutation accountability do not.







