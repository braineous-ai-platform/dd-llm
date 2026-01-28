# LLMDD (Deterministic Decisions for LLM Systems)

![This is an image](parallax-image.jpg)

LLMDD is an opinionated, deterministic **control-plane** for LLM behavior.

It separates:
- **Catalog**: versioned instructions + response contracts (what the model *should* do)
- **PolicyGate**: enforcement + receipts (what we *allowed/denied/changed*)
- **Commit Router**: commits as events (what we *decided*, auditably, for replay)

**Goal:** repeatability, auditability, and safe evolution of LLM behavior **outside model weights**.

## Status
Active build. Spine-first: models → persistence → processor → REST → Kafka.

## Modules (WIP)
- `dd-module-policygate`
- `dd-module-commit-router`

## Run locally (WIP)
Docs will land after the spine is locked green.
