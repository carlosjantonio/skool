# ADR-0001: Modular monolith with microservice-shaped modules

- **Status:** Accepted
- **Date:** 2026-07-07
- **Deciders:** Carlos (solo build)

## Context

The [system prompt](../../school-management-system-prompt.md) specifies a 17-service microservice architecture (Sections 4–5). At the same time, its footnote and Section 5 both recommend starting as a "modular monolith" given a solo build with AI assistance — the distributed-systems tax (Kafka, Eureka, per-service databases, deployment orchestration) is heavy to carry before the first pilot school exists.

We need to pick a starting posture.

## Decision

Build a **modular monolith** organised as one Spring Boot deployable with one Maven module per bounded context, mirroring the 14 domain services from the prompt (identity, academic-structure, sis, staff, attendance, grading, assessment, assignments, forum, subject-board, fees, notifications, documents, reporting) plus three `common-*` shared-kernel modules.

Boundaries are enforced by:

1. Package convention: every module exposes `ao.skool.<module>.api` and hides `ao.skool.<module>.internal`.
2. An ArchUnit test in the `app` module that fails the build if any module imports another module's `internal.*` package.
3. Per-module Flyway migrations kept under `db/migration/<module>/`.

Cross-module communication happens via:

- **Direct API calls** through the exposed `api` package interfaces (thin service facades).
- **Domain events** via Spring's `ApplicationEventPublisher` — see [ADR-0002](0002-in-process-event-bus.md).

## Consequences

**Positive**
- One `mvn package`, one deployable, one Postgres instance to run in dev and prod.
- No Kafka, Eureka, or Config Server to operate until the domain is proven.
- Boundaries are still real — the ArchUnit test prevents the "big ball of mud" drift.
- The eventual split to microservices is a deployment change, not a redesign.

**Negative**
- One deployment can't be scaled per-module — if the assessment engine gets hot, the whole app scales.
- A bug in one module can crash the whole app.
- We must resist the temptation to reach into another module's internals for a quick win — the ArchUnit test is the guardrail.

**Trigger to split**
When any single module needs its own scaling profile, its own team, or its own release cadence, extract it — starting with `notifications` (already reactive/event-driven) and `assessment` (compute-heavy for concurrent quizzes).
