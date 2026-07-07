# Skool — Task Tracker

Status of every phase in [INITIAL_PLAN.md](INITIAL_PLAN.md). Check the plan itself for the full rationale behind each phase; this file is the punch list.

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

## Phase 4 — Student Learning Portal Core ⏳

**Goal:** The differentiating feature set from Section 3 of the prompt.

### Assessment module
- [ ] Question bank organized by subject + grade level, reusable across classes
- [ ] Quiz creation with multiple question types (multiple choice, true/false, short answer, essay)
- [ ] Timed test delivery with auto-submit
- [ ] Randomized question + option order per-student
- [ ] Auto-grade objective questions
- [ ] Manual-grade essay workflow with rubric
- [ ] Offline-tolerant test-taking (cache quiz locally, sync answers on reconnect)
- [ ] Result analytics for teachers (per-question difficulty, class averages)
- [ ] Per-student performance history
- [ ] `QuizPublished` and `QuizSubmitted` events
- [ ] Anti-cheating basics: tab-switch detection, time limits, randomization

### Assignments module
- [ ] Create → submit → grade → feedback loop
- [ ] Student file upload (uses Documents module)
- [ ] Deadline reminders
- [ ] Teacher grading UI with rubric
- [ ] `AssignmentDue` event

### Forum module
- [ ] One forum per subject per class (e.g., "Matemática – 10ª A")
- [ ] Threads with posts and replies
- [ ] Upvoting / helpful-marking
- [ ] Teacher moderation (pin, delete, hide, mark as verified answer)
- [ ] Notification when a teacher or peer replies to your post
- [ ] `ForumReplyPosted` event

### Subject Board module
- [ ] Announcements per subject
- [ ] Class materials (PDFs, slides, links) via Documents module
- [ ] Due-date-aware feed
- [ ] Low-bandwidth mode for attachments
- [ ] `AnnouncementPosted` event

### Student portal UI
- [ ] Personal dashboard (upcoming tests, pending assignments, recent grades, forum activity)
- [ ] Consolidated subject board feed + per-subject view
- [ ] Quiz-taking interface (with offline shell)
- [ ] Grade view with drill-down to per-subject history

**Exit criteria (per plan):** teacher creates a 20-question timed quiz, student takes it on a phone with the network cut mid-quiz and answers sync on reconnect; a subject forum has an active teacher-moderated thread.

---

## Phase 5 — Fees & Payments ⏳

**Goal:** Financial operations for the school.

### Fees module
- [ ] Fee schedules (propinas mensais, matrícula, exames)
- [ ] Scholarships / bolsas
- [ ] Kwanza invoicing
- [ ] Defaulter reports
- [ ] `InvoiceIssued`, `PaymentReceived`, `InvoiceOverdue` events

### Payment adapters
- [ ] `PaymentAdapter` interface
- [ ] `StubPaymentAdapter` — admin marks invoice paid manually
- [ ] Multicaixa Express reference-payment adapter (skeleton — real integration gated on procurement)
- [ ] Unitel Money adapter (skeleton)
- [ ] Africell Money adapter (skeleton)
- [ ] Bank transfer reconciliation (BAI / BFA / BIC / Standard Bank Angola CSV import)

**Exit criteria (per plan):** admin generates monthly propinas for all enrolled students; a payment marked as received via stub adapter updates the invoice and fires `PaymentReceived`.

---

## Phase 6 — Notifications + Reporting ⏳

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

## Phase 7 — Hardening + Rollout ⏳

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
`848f44e` (Phase 0) · `17e4931` (Phase 1 backend) · `4b34da8` (PWA skeleton) · `e5d8637` (Phase 2) · `43ee5d2` (Phase 3) · `edd484b` (Phase 3b) — all on `origin/main`.
