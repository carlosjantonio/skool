# Initial Plan — Skool (Angolan School Management System)

> Companion to [school-management-system-prompt.md](school-management-system-prompt.md). This document translates the spec into a phased, executable plan for a solo builder working with AI assistance.

---

## 1. Guiding Decisions

These are locked-in choices that shape every phase below. Revisit only if a phase surfaces a concrete blocker.

| # | Decision | Rationale |
|---|----------|-----------|
| D1 | **Modular monolith first**, split to microservices later | Solo build; Section 5 of the prompt explicitly endorses this. Keeps the 17-service *domain boundary* map, but ships as one deployable. Avoids Kafka/Eureka/multi-DB ops tax before there's a user. |
| D2 | **Maven multi-module** (one module per bounded context) | Simpler than Gradle for JVM-only Spring shops; parent POM aligns Spring Boot/BOM versions. |
| D3 | **PostgreSQL, one schema per module** (not per DB yet) | Keeps ownership clean via schema search paths + Flyway per module. Splits cleanly to DB-per-service later without entity redesign. |
| D4 | **In-process event bus first** (Spring `ApplicationEventPublisher`), Kafka later | Domain events are the seam. Publishing them from day one means the eventual Kafka swap is a transport change, not a rewrite. |
| D5 | **pt-AO is the default locale**, English is opt-in | Every user-facing string routed through `MessageSource` from day one. Reversing this later is painful. |
| D6 | **Offline-tolerance is a client concern**; backend exposes **idempotent batch sync endpoints** with client-generated UUIDs | Matches Section 6 of prompt. Prevents dupes on retry without a stateful sync server. |
| D7 | **PWA (not native)** for the student/parent app | Section 3.4 requirement; skips Play Store friction on low-data devices. |
| D8 | **Payment integrations are adapters behind an interface** — stub locally, real in prod | Multicaixa/Unitel Money/bank reconciliation have long procurement cycles. Don't block product on them. |
| D9 | **Tenant ID is a first-class column** on every domain table from day one | Prompt Section 6 asks for multi-tenancy. Retrofitting it is a nightmare. |
| D10 | **Audit log is a cross-cutting module**, not a service concern | Grade changes, fee edits, and permission changes all need immutable trails for Lei 22/11 compliance. |

---

## 2. Recommended Tech Stack Baseline

Confirms Section 4 of the prompt with concrete versions:

- **JDK:** Java 25 (virtual threads on by default via `spring.threads.virtual.enabled=true`)
- **Framework:** Spring Boot 3.3.x (latest 3.x at build time)
- **Build:** Maven 3.9+, multi-module reactor, parent POM
- **DB:** PostgreSQL 16, HikariCP, one schema per module
- **Migrations:** Flyway, migrations live under each module's `src/main/resources/db/migration/<module>/`
- **Security:** Spring Security + JWT (RS256), refresh tokens in HTTP-only cookies
- **API docs:** springdoc-openapi, one aggregate Swagger UI at the gateway path
- **Testing:** JUnit 5, Testcontainers (Postgres), Spring MockMvc, REST Assured for smoke
- **Observability:** Actuator, Micrometer, OTLP exporter (Grafana Tempo/Jaeger optional in dev)
- **Frontend (student/parent PWA):** React + Vite + TypeScript, Workbox for service worker, IndexedDB (Dexie) for offline cache
- **Frontend (admin/teacher):** Same stack; single SPA with role-gated routes
- **SMS provider:** Africa's Talking (has AO coverage) — behind an interface
- **File storage:** MinIO in dev, S3-compatible in prod
- **Container:** Docker + Docker Compose for local; one image now, per-service images after the split

---

## 3. Repository Layout (Modular Monolith)

```
skool/
├── pom.xml                              # parent POM: versions, plugins
├── docker-compose.yml                   # postgres + minio + app
├── docs/
│   ├── adr/                             # architecture decision records
│   └── events.md                        # canonical domain event catalog
├── app/                                 # the single deployable
│   └── src/main/java/ao/skool/app/
│       └── SkoolApplication.java
├── common/                              # shared kernel — keep small
│   ├── common-domain/                   # Money(Kwanza), Tenant, IDs, base events
│   ├── common-web/                      # error handlers, request logging, i18n
│   └── common-security/                 # JWT filter, RBAC annotations
├── modules/
│   ├── identity/                        # Service #1
│   ├── academic-structure/              # #2  schools, turmas, subjects, timetable
│   ├── sis/                             # #3  students, guardians, matrícula
│   ├── staff/                           # #4
│   ├── attendance/                      # #5
│   ├── grading/                         # #6  0–20 scale, trimester, boletim PDF
│   ├── assessment/                      # #7  quizzes, question bank
│   ├── assignments/                     # #8
│   ├── forum/                           # #9
│   ├── subject-board/                   # #10
│   ├── fees/                            # #11  Kwanza, payment adapters
│   ├── notifications/                   # #12  event consumer → SMS/email/push
│   ├── documents/                       # #13  MinIO wrapper
│   └── reporting/                       # #14  read-model from events
└── web/
    ├── admin-teacher-app/               # React PWA
    └── student-parent-app/              # React PWA (may share code)
```

**Rules for modules:**
- A module exposes a **public API package** (`ao.skool.<module>.api`) — DTOs, event types, feign-style clients.
- Everything else (`internal.*`) is package-private or explicitly guarded via ArchUnit tests.
- Modules **only** depend on `common-*` and other modules' `api` packages — never their `internal.*`.
- Every module publishes events through `ApplicationEventPublisher`; consumers use `@EventListener` or `@TransactionalEventListener(AFTER_COMMIT)`.
- ArchUnit test enforces "no cross-module `internal.*` imports" — this is the guardrail that keeps the eventual split cheap.

---

## 4. Phased Roadmap

Each phase ships something demonstrable. No phase depends on a phase after it. Estimates assume solo dev with AI pairing, ~20 focused hours/week.

### Phase 0 — Foundation (1–2 weeks)
**Goal:** Boot a "hello world" that already has the right shape.

- [ ] Parent POM, empty child modules, ArchUnit boundary tests
- [ ] Docker Compose: Postgres, MinIO, app
- [ ] Flyway wired per module (baseline migrations empty)
- [ ] i18n scaffolding with `messages_pt_AO.properties` as default
- [ ] Global exception handler → RFC 7807 problem+json
- [ ] Actuator + basic Micrometer metrics
- [ ] Tenant resolution filter (header or subdomain → `TenantContext` ThreadLocal / scoped bean)
- [ ] Audit-log common component (append-only table + `@Auditable` interceptor)
- [ ] CI: GitHub Actions running `mvn verify` on PR
- [ ] ADR-0001: "Modular monolith with microservice-shaped modules"
- [ ] ADR-0002: "In-process event bus first, Kafka later"

**Exit criteria:** `docker compose up` boots the app; `/actuator/health` green; ArchUnit tests pass on empty modules.

### Phase 1 — Identity + Academic Structure (2–3 weeks)
**Goal:** People can log in. The concept of a school, class, and subject exists.

- [ ] **Identity module:** Users, roles (Admin, Director, Secretary, Teacher, Student, Guardian, Ministry), password reset flow, JWT issuance, refresh tokens
- [ ] Seed script: default admin, one demo school
- [ ] **Academic Structure module:** Schools/campuses, provinces/municípios seeded from official INE Angola list, turmas, subjects, curricular tracks (Ciências, Humanidades, Económico-Jurídicas), academic year with trimesters (1º/2º/3º)
- [ ] Angolan-address value object: Província → Município → Comuna/Bairro
- [ ] BI + NIF value objects with format validation
- [ ] OpenAPI docs generated
- [ ] React app skeleton with login screen, role-gated routing shell

**Exit criteria:** Admin can log in, create a school, define classes and subjects for the current academic year. All UI in pt-AO.

### Phase 2 — SIS + Staff (2–3 weeks)
**Goal:** Enrol students and assign teachers.

- [ ] **SIS module:** Student profile (with BI, health notes, multiple guardians), matrícula workflow, document upload → Documents module
- [ ] **Staff module:** Teacher profiles, qualifications, class/subject assignments
- [ ] **Documents module (minimal):** MinIO wrapper, presigned upload URLs, encryption at rest for BI scans
- [ ] Guardian portal user linking (one guardian, N students)
- [ ] Admin UI: enrolment list, student detail, staff assignment

**Exit criteria:** Admin enrols 10 test students, assigns 3 teachers to 5 subjects across 2 classes. Guardians can log in and see their linked children.

### Phase 3 — Attendance + Grading (3 weeks)
**Goal:** Daily classroom operations work — even offline.

- [ ] **Attendance module:** Per-class daily attendance, batch sync endpoint with client UUIDs (idempotent), events `AttendanceMarkedAbsent`
- [ ] **Grading module:** 0–20 scale, trimester structure, per-subject grade entry, weighted averages, `GradePosted` event
- [ ] Boletim PDF generation (server-side, iText or OpenPDF, pt-AO)
- [ ] Teacher UI: class roster, one-tap attendance, grade entry table
- [ ] PWA offline shell: cache last-viewed class roster, queue attendance mutations in IndexedDB, replay on reconnect
- [ ] Audit log entries on every grade change

**Exit criteria:** A teacher on a flaky connection can mark attendance for a class of 30 offline; syncs cleanly on reconnect without duplicating rows. Boletim PDF renders for one trimester end-to-end.

### Phase 4 — Student Learning Portal Core (4 weeks)
**Goal:** The differentiating feature set from Section 3 of the prompt.

- [ ] **Assessment module:** Question bank, quiz creation, timed delivery, auto-grade objective, manual-grade essay, offline quiz-taking with local answer cache
- [ ] **Assignments module:** Create → submit → grade → feedback loop
- [ ] **Forum module:** Per-subject/per-class threads, replies, upvotes, "verified answer" marker, teacher moderation
- [ ] **Subject Board module:** Announcements, materials, due-date-aware feed
- [ ] Student personal dashboard: upcoming tests, pending assignments, recent grades, forum activity
- [ ] Anti-cheating basics: question randomization, time limits, best-effort tab-switch detection

**Exit criteria:** Teacher creates a 20-question timed quiz; a student takes it on a phone with the network cut mid-quiz and their answers sync on reconnect. A subject forum has an active teacher-moderated thread.

### Phase 5 — Fees & Payments (2–3 weeks)
**Goal:** Financial ops for the school.

- [ ] **Fees module:** Fee schedules, propinas, scholarships/bolsas, Kwanza invoicing
- [ ] Payment adapter interface + `StubPaymentAdapter` (marks invoices paid via admin action)
- [ ] Multicaixa Express reference-payment adapter (spec + skeleton; real integration gated on procurement)
- [ ] Unitel Money / Africell Money adapter skeletons
- [ ] Bank transfer reconciliation import (CSV upload from BAI/BFA/BIC)
- [ ] Defaulter reports
- [ ] Events: `InvoiceIssued`, `PaymentReceived`, `InvoiceOverdue`

**Exit criteria:** Admin generates monthly propinas for all enrolled students; a payment marked as received via stub adapter updates the invoice and fires `PaymentReceived`.

### Phase 6 — Notifications + Reporting (2–3 weeks)
**Goal:** The system talks to guardians and the Ministry.

- [ ] **Notification module:** Consumes `AttendanceMarkedAbsent`, `GradePosted`, `InvoiceOverdue`, `ForumReplyPosted` events; dispatches per guardian preference to SMS (Africa's Talking) / email / push / WhatsApp
- [ ] Guardian notification preferences UI
- [ ] **Reporting module:** Read-model built from events, Ministério da Educação-shaped statistical exports (enrolment by class, attendance rate, grade distribution)
- [ ] Ministry-viewer read-only role

**Exit criteria:** Marking a student absent triggers an SMS to their guardian in pt-AO within seconds. Reporting dashboard produces a downloadable DPE-format enrolment summary.

### Phase 7 — Hardening + Rollout (ongoing)
**Goal:** Pilot with one real school.

- [ ] Load testing (Gatling) — 1 school × 500 students × 30 teachers
- [ ] Backup/restore runbook
- [ ] Data import tooling for schools migrating from spreadsheets (CSV templates for students, staff, subjects)
- [ ] Rollout playbook (Section 7.6 of prompt)
- [ ] Lei 22/11 compliance pass: consent tracking on guardian signup, right-to-access export, right-to-erasure workflow for minors, data-retention policy
- [ ] Security review: password policy, JWT rotation, BI scan encryption, audit-log immutability
- [ ] Accessibility pass: WCAG 2.1 AA where feasible

**Exit criteria:** One pilot school runs a full trimester on the system.

---

## 5. MVP Scope (What Ships to the First Pilot School)

Trimming Phases 0–4 tightly for the smallest useful cut:

**Included:**
- Identity + roles
- School, class, subject, timetable (light)
- Student enrolment + guardian linking
- Teacher assignment
- Attendance (with offline sync)
- Grading + boletim PDF
- Assignments + Subject Board + Forum
- Basic quiz creation and delivery
- Guardian read-only view of grades/attendance
- SMS absence alerts (behind stub if Africa's Talking not yet contracted)

**Deferred to post-MVP:**
- Full fees/payments (start with manual invoice tracking, add integrations later)
- Reporting/DPE exports
- Library module
- Gamification
- WhatsApp notifications
- Ministry-viewer role
- Push notifications (SMS + email cover it)

---

## 6. Immediate Next Steps (Week 1)

In order — nothing here blocks on external procurement or approvals:

1. `git init`, first commit with prompt + this plan
2. Create parent `pom.xml` with Spring Boot BOM 3.3.x, Java 25 toolchain
3. Scaffold empty modules matching Section 3 layout
4. Write ArchUnit boundary test that fails if any module imports another's `internal.*`
5. `docker-compose.yml` with Postgres 16 + MinIO
6. Boot `SkoolApplication.java` — `/actuator/health` green
7. Wire i18n `MessageSource` with `messages_pt_AO.properties` as default, English fallback
8. Global RFC 7807 exception handler
9. First ADR: "Modular monolith with microservice-shaped modules"
10. GitHub Actions: `mvn verify` on PR

That's Phase 0. Everything after depends on this foundation being solid, so don't skip the ArchUnit test — it's the load-bearing guardrail for the eventual split.

---

## 7. Open Questions to Resolve Before Phase 1

- **Hosting target:** Local Angolan provider (data residency), AWS Cape Town region, or Hetzner EU? Affects payment adapter latency and Lei 22/11 posture.
- **SMS provider contract:** Africa's Talking vs local aggregator via Unitel/Movicel — pricing per SMS at expected volume?
- **Pilot school:** Do we have a specific institution in mind? Their trimester dates and grading conventions should drive Phase 3 acceptance testing.
- **Payment integration timeline:** Which of Multicaixa / Unitel Money / bank reconciliation is most important for the pilot? That one gets real integration in Phase 5; others stay stubs.
- **Multi-tenant from day one, or single-tenant with tenant column?** Both work, but affects the login UX (subdomain routing vs school selector).

Answer these when convenient; none of them block Phase 0.
