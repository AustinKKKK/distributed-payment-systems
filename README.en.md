# Distributed Payment Processing System

[한국어](./README.md) | [English](./README.en.md)

Started from a single-server, fully synchronous baseline and evolved it step by step through Kafka, Redis, and Primary/Replica separation. Each tier was driven by one principle: **hit a wall, measure it, understand why, fix it** — not by a fixed volume target.

## Full System Architecture

![Full System Architecture](./full-architecture-tier1-4.svg)

## Summary by Tier

| Tier | Problem | Approach | Result |
|---|---|---|---|
| Tier 1 | Connection pool exhaustion capped throughput | HikariCP pool tuning (10→50) | 2.4x throughput (3.6K → 8.5K req/s) |
| Tier 2 | A slow external call collapsed throughput, 3.75–6.35% failure rate | Decoupled accept/process with Kafka | Accept throughput 167x (24.5 → 4,082 req/s), 0% failures |
| Tier 3 | Every balance query recomputed the full ledger | Redis caching (evict-on-write) | 38.4x throughput (776 → 29,787 req/s) |
| Tier 4 | Single DB carrying both read and write load | Primary/Replica split + simulated replication lag | No throughput gain on a single local machine (hypothesis disproven, root-caused); reproduced and fixed a real read-your-own-writes bug |

**Tier 4 is not a clean win — it's a case where the hypothesis was wrong, measured honestly, and a real correctness bug was hit and fixed along the way.** Splitting primary/replica on one laptop didn't help because both databases still share the same CPU and disk; that result is recorded as-is rather than smoothed over.

## Details per tier

- [Tier 1 — Single server, synchronous processing](./tier1)
- [Tier 2 — Async processing with Kafka](./tier2)
- [Tier 3 — Caching with Redis](./tier3)
- [Tier 4 — Primary/Replica separation and replication lag](./tier4)

## Stack

Java 21 · Spring Boot · PostgreSQL · Redis · Apache Kafka · Docker Compose · k6 (load testing)

## Core design principles

- Append-only, double-entry ledger — balance is always derived from ledger rows, never a stored column
- Idempotency enforced physically via a DB UNIQUE constraint, not an application-level check
- Every tool (Kafka, Redis, replica) introduced only once a measured bottleneck justified it
- Predictions made before load testing, and recorded even when wrong
