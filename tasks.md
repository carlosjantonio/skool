# Skool — Task Tracker

Status of every phase. The forward plan — revised scope, re-ordered phases, next actions — is
[ROADMAP.md](ROADMAP.md); [INITIAL_PLAN.md](INITIAL_PLAN.md) keeps the guiding decisions and
module layout. This file is the punch list.

**Legend:** ✅ done · ⏳ pending · 🟡 partially done

---

## Phase 0 — Foundation ✅

**Goal:** Boot a "hello world" that already has the right shape.

- [x] Parent POM with Spring Boot 3.5.x BOM + Java 25 toolchain
- [x] Multi-module reactor: 3 `common-*` modules + 14 domain modules + `app`
- [x] Docker Compose (Postgres 16 + MinIO)
- [x] Flyway wired per module
- [x] i18n scaffolding with `messages_pt_AO.properties` as default, English fallback
- [x] Global RFC 7807 problem+json exception handler
- [x] Actuator + Micrometer metrics
- [x] Tenant resolution (`RequestScopedTenantContext` reading from JWT)
- [x] Audit-log common component (append-only `audit_log` table)
- [x] ArchUnit boundary tests (no cross-module `internal.*` imports; common-* is a leaf)
- [x] GitHub Actions CI running `mvn verify` on JDK 25
- [x] ADR-0001 — Modular monolith with microservice-shaped modules
- [x] ADR-0002 — In-process event bus first, Kafka later
- [x] Domain event catalog (`docs/events.md`)

**Commit:** `848f44e`

---

## Phase 1 — Identity + Academic Structure ✅

**Goal:** People can log in. The concept of a school, class, and subject exists.

### Identity
- [x] Users, roles (Admin, Director, Secretary, Teacher, Student, Guardian, Ministry)
- [x] Password reset flow (hashed refresh tokens with rotation)
- [x] JWT issuance (HS256 via jjwt) + validation filter
- [x] Refresh tokens stored SHA-256-hashed at rest
- [x] Login / refresh / logout endpoints
- [x] Seed default admin (`admin@skool.demo` / `admin123`)

### Academic Structure
- [x] 21 Angolan provinces seeded (post-2024 administrative reorganization)
- [x] Municípios reference data (Luanda's 6 + Icolo e Bengo split + provincial capitals)
- [x] Schools as tenants (school.id === tenant_id)
- [x] Academic years with trimesters (1º / 2º / 3º Trimestre)
- [x] Subjects with GradeLevel (CLASSE_1 – CLASSE_13, grouped by Ciclo)
- [x] Curricular tracks (Ciências Físicas e Biológicas, Ciências Económico-Jurídicas, Humanidades, etc.)
- [x] Turmas (school class groups) with capacity
- [x] Angolan-address value object: Província → Município → Comuna/Bairro
- [x] BI + NIF value objects with format validation
- [x] Reference endpoints for provinces / municípios / grade levels / tracks
- [x] OpenAPI docs via springdoc

### PWA scaffold
- [x] Vite + React 18 + TypeScript + react-i18next + react-router
- [x] pt-AO default locale, en toggle
- [x] Fetch wrapper with bearer token + auto-refresh on 401
- [x] `AuthProvider` hydrates session from persisted refresh token
- [x] Login screen, role-gated routing, dashboard with per-role action cards
- [x] Vite dev proxy to backend, CORS bean on backend
- [x] `vite-plugin-pwa` manifest + service worker generation

**Commits:** `17e4931`, `4b34da8`

---

## Phase 2 — SIS + Staff + Documents ✅

**Goal:** Enrol students and assign teachers.

### SIS
- [x] Student profiles with optional BI, DOB, sex, health notes, address
- [x] Guardian profiles with phone, email, BI
- [x] N:M `student_guardians` link with relationship (PAI/MAE/AVO/IRMAO/TIO/TUTOR/OUTRO) + primary/emergency flags
- [x] Enrollment ("matrícula") with lifecycle PENDING → ENROLLED → WITHDRAWN → GRADUATED
- [x] `StudentEnrolled` domain event
- [x] `StudentDirectory` public API for cross-module lookups
- [x] Guardian portal endpoint `/api/guardians/me/children`

### Staff
- [x] Staff entity with BI, NIF, qualification, hire date
- [x] Portal user auto-provisioning (`provisionPortalUser: true` triggers TEACHER role creation)
- [x] `StaffAssignment` linking teacher × subject × turma × academic year with role (TEACHER / HEAD_TEACHER / ASSISTANT)
- [x] `/api/staff/assignments?academicYearId=` for listing

### Documents (MinIO)
- [x] `Document` metadata entity
- [x] MinIO client bean, bucket auto-created on `ApplicationReadyEvent`
- [x] Multipart upload endpoint
- [x] Streaming download
- [x] Delete + list by owner (STUDENT / GUARDIAN / STAFF, ownerId)
- [x] Storage keys shaped `{tenantId}/{docId}/{filename}` for tenant isolation

### Cross-module wiring
- [x] `IdentityUserService` in `identity.api` — first inter-module contract
- [x] SIS `GuardianService` calls `createGuardianUser` when guardian has an email
- [x] Staff `StaffService` calls `createTeacherUser` on portal opt-in
- [x] Temp passwords logged (deferred to Phase 6: notification module dispatches via SMS/email)

**Commit:** `e5d8637`

---

## Phase 3 — Attendance + Grading + Boletim + Audit ✅

**Goal:** Daily classroom operations work — even offline.

### Attendance
- [x] `AttendanceRecord` with client-generated UUIDs
- [x] `(student_id, turma_id, date_recorded)` uniqueness constraint
- [x] `POST /api/attendance/batch` — idempotent upsert (same id = update; new id = insert; duplicate `(s,t,d)` = fail)
- [x] `AttendanceMarkedAbsent` published only on PRESENT→ABSENT (or first-ABSENT) transitions
- [x] Attendance summary endpoint per student for a date range

### Grading
- [x] `Grade` entity with 0–20 CHECK constraint, weight, category (TEST / EXAM / ASSIGNMENT / PARTICIPATION / OTHER)
- [x] Per-trimester keys (T1 / T2 / T3)
- [x] Batch grade save
- [x] Student grade summary: weighted per-subject-per-trimester averages, trimester averages, final average
- [x] `GradePosted` domain event
- [x] Every grade write records a `grade.post` audit row

### Boletim PDF
- [x] Server-side render via OpenPDF
- [x] Angolan report-card layout: school header, student block, subject × trimester grid, trimester averages, final average
- [x] pt-AO labels + Director / Encarregado / Aluno signature block
- [x] `GET /api/grades/boletim?studentId=&academicYearId=` returns `application/pdf`

### Audit log
- [x] `AuditLogEntry` JPA entity in common-security
- [x] `AuditLogWriter` runs in `REQUIRES_NEW` so audit rows survive business-transaction rollback
- [x] Wired into `GradeService.record`

### Cross-module read APIs
- [x] `StudentDirectory.findStudent` (sis.api)
- [x] `AcademicStructureQuery.{findSubject, findAcademicYear, findTurma, findSchool}` (academic_structure.api)

**Commit:** `43ee5d2`

---

## Phase 3b — Teacher UI + Offline PWA ✅

**Goal:** A teacher on a flaky connection can mark attendance for 30 students offline and have it sync cleanly on reconnect.

### Backend
- [x] `GET /api/staff/me` — current teacher's staff record (via user_id lookup)
- [x] `GET /api/staff/me/assignments?academicYearId=` — teacher-scoped assignments
- [x] `GET /api/enrollments` accepts optional `turmaId` filter
- [x] Attendance idempotency upgraded to true UPSERT (bug uncovered by offline test — same id with new status now updates the row instead of silently dropping the edit)

### Frontend
- [x] Dexie (IndexedDB) queue `skool-offline.attendance`
- [x] Deterministic v5 UUIDs (`uuid:v5(NAMESPACE_DNS, "attendance:{s}:{t}:{d}")`) — client and server converge on the same PK
- [x] `useOnline` hook via `navigator.onLine` + online/offline events
- [x] `useAttendanceSync` hook: flushes on reconnect, polls queue depth every 4s
- [x] `OfflineIndicator` chip in header (Ligado / Sem ligação / N na fila / A sincronizar)
- [x] `TeacherClassesPage` — assignments grouped by turma
- [x] `TeacherAttendancePage` — one-tap Presente / Ausente / Atraso / Justificado, enqueue-first optimistic
- [x] `TeacherGradesPage` — subject + trimester picker, 0–20 input, batch save
- [x] pt-AO strings for every teacher-facing message

### Verification
- [x] Ana logs in with provisioned portal password
- [x] Marks 5 students PRESENT online → syncs, DB shows 5 PRESENT
- [x] Force-offline → flips 2 to ABSENT → queue holds 2, DB unchanged, chip shows "Sem ligação"
- [x] Reconnect → queue drains automatically → DB shows 2 ABSENT + 3 PRESENT

**Commit:** `edd484b`

---

## Phase 4 — Student Learning Portal Core ✅

**Goal:** The differentiating feature set from Section 3 of the prompt.

### Assessment module
- [x] Question bank organized by subject + grade level, reusable across classes
- [x] Quiz creation with multiple question types (multiple choice, true/false, short answer, essay)
- [x] Timed test delivery with auto-submit
- [x] Randomized question + option order per-student
- [x] Auto-grade objective questions
- [x] Manual-grade essay workflow with rubric
- [x] Offline-tolerant test-taking (cache quiz locally, sync answers on reconnect)
- [x] Result analytics for teachers (per-question difficulty, class averages) — `GET /api/quizzes/{id}/analytics`
- [x] Per-student performance history (attempts list endpoint)
- [x] `QuizPublished` and `QuizSubmitted` events
- [x] Anti-cheating basics: tab-switch detection, time limits, randomization

### Assignments module
- [x] Create → submit → grade → feedback loop
- [x] Student file upload (uses Documents module — submission stores document_id)
- [x] Deadline reminders — `AssignmentDue` fires; delivery is Phase 7 (notifications) in ROADMAP.md
- [x] Teacher grading UI endpoints with rubric field
- [x] `AssignmentDue` event

### Forum module
- [x] One forum per subject per class (unique on subject_id + turma_id + academic_year_id)
- [x] Threads with posts and replies
- [x] Upvoting / helpful-marking (toggle via `forum_upvotes` join table)
- [x] Teacher moderation (pin, hide, mark as verified answer)
- [x] Reply notification — `ForumReplyPosted` fires; delivery is Phase 7 (notifications) in ROADMAP.md
- [x] `ForumReplyPosted` event

### Subject Board module
- [x] Announcements per subject
- [x] Class materials (PDFs, slides, links) via document_id / external_url
- [x] Due-date-aware feed (`due_at` column, indexed)
- [x] Low-bandwidth flag on entries — client skips prefetch
- [x] `AnnouncementPosted` event

### Student portal UI
- [x] Personal dashboard (upcoming quizzes, class board feed, subject forums)
- [x] Consolidated subject board feed
- [x] Quiz-taking interface with offline shell (IndexedDB + deterministic v5 answer ids)
- [x] Grade view with per-trimester / per-subject table

### Cross-cutting
- [x] SIS: student ↔ user link (migration V4_3) + `POST /api/students/{id}/portal-user`
- [x] Identity: `createStudentUser(...)` on `IdentityUserService`
- [x] `SkoolPrincipal.fullName` piped through JWT so forum authorship works without cross-module lookups

**Exit criteria (per plan):** teacher creates a 20-question timed quiz, student takes it on a phone with the network cut mid-quiz and answers sync on reconnect; a subject forum has an active teacher-moderated thread.

**Commit:** `d72f03f` (race fix on `uk_attempts_quiz_student` still uncommitted — lands in Phase 5)

---

## Phase 5 — Fees & Payments 🟡 (landed, UI verified — tests and overdue scheduler outstanding)

**Goal:** Financial operations for the school.

### Fees module
- [x] Fee schedules (`FeeKind`: PROPINA_MENSAL, MATRICULA, EXAME, UNIFORME, MATERIAL, OUTRO)
- [x] Scholarships / bolsas (FULL / PERCENTAGE / FIXED; best active one applied at billing)
- [x] Kwanza invoicing — `POST /api/fees/schedules/{id}/run-billing` fans out idempotently on `(fee_schedule_id, student_id)` over `StudentDirectory.listEnrolledForYear`
- [x] Defaulter report — `GET /api/payments/defaulters`, aggregated per student
- [x] `InvoiceIssued`, `PaymentReceived`, `InvoiceOverdue` events
- [x] Audit rows on `invoice.issue` and `payment.record`
- [x] Guardian invoice portal — `GET /api/guardian/fees/invoices`
- [ ] Overdue detection is a manual `POST /api/payments/sweep-overdue` — needs a scheduled job (ROADMAP Phase 9)
- [ ] Invoice / receipt PDFs (Phase 9)
- [ ] Backend tests (Phase 5 in ROADMAP)

### Payment adapters
- [x] `PaymentAdapter` interface + `PaymentAdapterRegistry` (`EnumMap` by `PaymentMethod`)
- [x] `StubPaymentAdapter` — settles immediately; admin "mark paid"
- [x] Multicaixa Express / Unitel Money / Africell Money — skeletons returning references; real integration gated on procurement
- [ ] Bank transfer — returns a hard-coded IBAN string; no CSV reconciliation yet (Phase 9)

### UI
- [x] `AdminFeesPage`, `AdminDefaultersPage`, `GuardianInvoicesPage`
- [x] Verified in the browser against the HEAD image: schedules list, run-billing idempotent from the UI (0 emitidas / 6 ignoradas), defaulters total reconciles (5 × 75 000 + 50 000 = 425 000 Kz), guardian sees only her child's invoices and `Pagar` yields a Multicaixa reference without settling. Demo guardian: `maria.silva@skool.demo` (MAE of Aluno 1 Silva); temp password is in the app container log from 16 Sep.

**Exit criteria (per plan):** admin generates monthly propinas for all enrolled students; a payment marked as received via stub adapter updates the invoice and fires `PaymentReceived`. — **Met** (API smoke test `Issued=6`, `Status=PAID`; UI walk on 16 Sep). Commit `372610c`.

---
## Phase 6 — Notifications + Reporting ⏳ (superseded — split into ROADMAP Phases 7 and 11)

**Goal:** The system talks to guardians and to the Ministry.

### Notification module
- [ ] Consumes `AttendanceMarkedAbsent`, `GradePosted`, `InvoiceOverdue`, `ForumReplyPosted`, `UserRegistered`
- [ ] Africa's Talking SMS adapter
- [ ] Email adapter (SMTP or transactional service)
- [ ] Web push adapter
- [ ] WhatsApp adapter (optional)
- [ ] Guardian notification preferences UI
- [ ] Delivery status tracking
- [ ] Handles the deferred "user provisioned — send temp password via SMS/email" case from Phase 2

### Reporting module
- [ ] Read-model built from events (eventually consistent)
- [ ] Enrolment by class, grade distribution, attendance rate exports
- [ ] Ministério da Educação / DPE-shape statistical exports
- [ ] Ministry-viewer read-only role

**Exit criteria (per plan):** marking a student absent triggers an SMS to the guardian in pt-AO within seconds; reporting produces a downloadable DPE-format enrolment summary.

---

## Phase 7 — Hardening + Rollout ⏳ (superseded — ROADMAP Phase 12; consent/CSV import pulled forward to Phase 10)

**Goal:** Pilot with one real school.

- [ ] Load testing with Gatling (1 school × 500 students × 30 teachers)
- [ ] Backup/restore runbook
- [ ] CSV import tooling for schools migrating from spreadsheets (students, staff, subjects)
- [ ] Rollout playbook
- [ ] Lei 22/11 compliance pass:
  - [ ] Consent tracking on guardian signup
  - [ ] Right-to-access data export
  - [ ] Right-to-erasure workflow for minors
  - [ ] Data-retention policy
- [ ] Security review:
  - [ ] Password policy
  - [ ] JWT rotation to RS256 (upgrade from HS256 before the microservice split)
  - [ ] BI-scan encryption at rest
  - [ ] Audit-log immutability enforcement (revoke UPDATE/DELETE at the DB role level)
- [ ] Accessibility pass (WCAG 2.1 AA where feasible)
- [ ] Observability: OTLP exporter to a real tracing backend
- [ ] Pilot: one school runs a full trimester on the system

---

## Test coverage — Phases 0–4 ✅

164 tests in `app/src/test/java/ao/skool/app`, run with
`docker run --rm -v "$PWD":/work -v "$HOME/.m2":/root/.m2 -w /work maven:3.9-eclipse-temurin-25 mvn -pl app -am test`
(the host JDK is 23; the project needs 25).

Everything is driven through MockMvc with real signed JWTs rather than by calling services
directly — the seams most likely to break are the JWT filter, `@PreAuthorize` gates, tenant
resolution and the JSON contracts, and a service-level test skips all of them. Tests are not
`@Transactional`; each test method gets a **fresh tenant UUID** instead, so `REQUIRES_NEW`
writes and constraint handling behave as they do in production.

| Phase | Files | Tests | What is pinned down |
|---|---|---|---|
| 0 | `phase0/` | 35 | BI/NIF/Money/TenantId invariants, public vs. authenticated routes, forged-token rejection, RFC 7807 shape in pt-AO, tenant isolation, **schema integrity guard** |
| 1 | `phase1/` | 25 | JWT claim round-trip + forgery/expiry/issuer rejection, login, refresh **rotation**, logout revocation, hashed-at-rest refresh tokens, academic year/trimester/subject/turma, role gates |
| 2 | `phase2/` | 21 | Student + guardian records, guardian portal scoping, matrícula lifecycle, double-enrolment rejection, withdrawal removing a student from the billable roll, staff + assignments |
| 3 | `phase3/` | 24 | Attendance upsert-by-client-id, duplicate `(student,turma,date)` handling, absence event fired **only on transition**, weighted averages worked out by hand, audit rows, boletim PDF, **cross-tenant student access** |
| 4 | `phase4/` | 56 | Question bank (4 types), quiz lifecycle, **answer-key masking**, frozen per-student question order, idempotent answer writes, auto-grading, manual essay grading, analytics, assignments, forum moderation, subject board |

- [x] `SchemaIntegrityTest` — asserts every mapped entity has a queryable table. Hibernate logs a failed `CREATE TABLE` as a WARN and carries on, so this is the only thing stopping a table going silently missing from the test schema.
- [ ] Frontend tests — `web/skool-app` still has none.
- [ ] Phase 5 (fees) tests — not written yet (ROADMAP Phase 5).

**Three defects found and fixed while writing these:**

1. **Logout never revoked the refresh token.** `/api/auth/logout` required authentication, but
   the PWA calls it with `auth: false` and swallows the failure in a `finally`. A "logged out"
   session's refresh token stayed valid for its full 30-day TTL. Fixed by adding logout to the
   `permitAll` list next to refresh — both authenticate by possession of the opaque token, not
   by access token.
2. **One bad attendance row killed the whole batch.** `AttendanceService.ingest` caught
   `DataIntegrityViolationException` around `save()`, but JPA does not flush there, so the
   violation surfaced at commit — outside the `catch` — and returned 500 instead of the
   documented per-entry `failed` list. The offline queue would have retried that batch forever.
   Fixed with `AttendanceRecordWriter` (`REQUIRES_NEW` + `saveAndFlush`), mirroring the
   `AttemptFactory` pattern already used in assessment.
3. **`StudentDirectory` leaked across tenants.** `findStudent` / `findStudentByUserId` were
   plain `findById` lookups with no tenant filter, and `GradeRepository` does not filter by
   tenant either — so a teacher at school B holding a school A `studentId` + `academicYearId`
   got a 200 with that student's name and marks from
   `GET /api/grades/students/{id}/summary`. The same lookup backs the assignments grading
   view and the fees defaulter report. Both ids are opaque UUIDs so it was not trivially
   reachable, but tenant isolation should not rest on an id being hard to guess — least of
   all for minors' records under Lei 22/11. Both methods now filter on the current tenant and
   return empty (→ 404), so another school's student is indistinguishable from one that does
   not exist. Pinned by `phase3/CrossTenantGradeAccessTest`.

---

## Deferred / cross-cutting

Not tied to a specific phase; picked up when they start blocking work.

- [ ] Kafka migration (per ADR-0002 trigger — when we extract a module or need cross-JVM events). Domain events already implement `DomainEvent` so the transport swap is transparent.
- [ ] Split identity module out first (highest RBAC surface, cleanest boundary)
- [ ] Split notification module out second (already reactive, no synchronous callers)
- [ ] Migration to Spring Cloud Config + Eureka once we have 2+ deployables
- [ ] Structured logging (JSON) once we ship to production
- [ ] Metrics dashboard (Grafana) — Actuator + Micrometer already emit; needs a scraper

---

## Open questions from INITIAL_PLAN.md §7

Still worth answering before Phase 7 (or Phase 5, whichever comes first):

- [ ] Hosting target: local Angolan provider (data residency), AWS Cape Town, Hetzner EU?
- [ ] SMS provider contract: Africa's Talking vs local aggregator via Unitel/Movicel — pricing at expected volume?
- [ ] Pilot school: which specific institution? Their trimester dates + grading conventions drive Phase 3 acceptance testing (already implemented against the default; may need per-school override).
- [ ] Payment integration priority: which of Multicaixa / Unitel Money / bank reconciliation matters most for the pilot?
- [ ] Multi-tenant login UX: subdomain routing (`escoladelta.skool.ao`) or school selector on the login page?

---

**Commits so far:**
`848f44e` (Phase 0) · `17e4931` (Phase 1 backend) · `4b34da8` (PWA skeleton) · `e5d8637` (Phase 2) · `43ee5d2` (Phase 3) · `edd484b` (Phase 3b) · `d72f03f` (Phase 4) — all on `origin/main`. Phase 5, the test suite and three fixes are **uncommitted**.
