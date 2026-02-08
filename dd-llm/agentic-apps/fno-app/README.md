# FNO — Flight Network Orchestrator

This module demonstrates how flight data flows through the system end-to-end:
from ingestion, to graph substrate construction, to query execution, to observation.

No prior context is required.
Run it, observe the output, then explore the code.

---

## Quick start

### 1) Start the backend

```bash
./run.sh
```

This starts the FNO backend locally (Docker).

---

### 2) Run the smoke test

```bash
./smoke.sh
```

This executes a complete black-box flow:

- data ingestion
- graph substrate construction
- query execution
- history observation (why_snapshot aggregates)

---

## Developer flow (high level)

FNO exposes CGO through three observable phases.
If you ran `smoke.sh`, you have already seen all of them.

### 1) Ingestion phase

- you submit domain data (e.g. flights)
- CGO validates structure and builds a graph substrate
- the result is a `GraphSnapshot` (nodes + inferred relationships)

This phase answers: **“What does the system know right now?”**

---

### 2) Query phase

- you submit a task against the existing substrate
- CGO evaluates the task deterministically
- no graph mutation occurs in this phase

This phase answers: **“Is this state valid or consistent?”**

---

### 3) Observation phase

- you read accumulated execution history
- scores and outcomes are exposed via `why_snapshot`
- the graph itself is not modified

This phase answers: **“Why did the system behave this way?”**

---

## What to look for in the output

When running `smoke.sh`, read the console output carefully.

### Ingest

You will see:

- the ingest request payload
- a `GraphSnapshot` response

  - nodes (e.g. `Airport:*`, `Flight:*`)
  - inferred relationships between flights

This shows how the **graph substrate is formed from data**.

---

### Query

You will see:

- a task-level request
- a structured response indicating whether the task is valid

At this stage, query execution is **deterministic and rule-based**.
LLM-backed reasoning is **not yet integrated** and will be introduced in a future release.

---

### Observe

You will see:

- a `why_snapshot`

  - total events
  - scoring aggregates

This shows how **system history accumulates over time**.

---

## Data

`flights.json` is intentionally kept at the repository root.

- it matches the ingest contract
- it is easy to modify
- changing it and re-running `smoke.sh` immediately reflects in the output

---

## Notes

- `smoke.sh` is the single source of truth for current system behavior
- advanced reasoning capabilities are intentionally out of scope for this version
- this README focuses on **observable behavior**, not internal implementation

---

## Supported environment

- macOS
- Docker Desktop
- bash + curl (preinstalled)

Windows is not supported.

---

## Next: CGO integration details

If you want to integrate CGO into your own service,
start with **Chapter A — API Integration**, which explains:

- the public CGO API surface
- Facts, Relationships, Rulepacks, and GraphView
- how applications submit data and consume results

👉 👉 See [**_CGO Documentation — Chapter A (API Integration)_** in the repository root README](../../../../README.md).

Run it first.
Then explore the code.
