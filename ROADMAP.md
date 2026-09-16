# Roadmap — Skool (revised plan)

> Supersedes the phased roadmap in [INITIAL_PLAN.md §4–§6](INITIAL_PLAN.md). The guiding
> decisions (§1), stack (§2) and module layout (§3) there still hold. This document is
> what to build next, in what order, and why; [tasks.md](tasks.md) stays the punch list.

**Sizing:** S ≈ 1–2 focused days · M ≈ a week · L ≈ 2+ weeks. Observed pace with AI pairing
has been well ahead of the original estimates, so treat these as relative, not calendar.

---

## 1. Where we are

Phases 0–4 are built and on `origin/main`. Phase 5 (fees) is built but **not committed**, along
with the 164-test suite, three security/correctness fixes it uncovered, and the docker-compose
fixes. 32 paths sit uncommitted on top of `d72f03f`. Nothing in the frontend is tested.

The build order drifted from the plan's own MVP cut ([INITIAL_PLAN.md §5](INITIAL_PLAN.md)):
fees were built ahead of three things the MVP lists as *included* — a guardian view of grades and
attendance, SMS absence alerts, and a light timetable. Meanwhile several requirements in the
[original prompt](school-management-system-prompt.md) were never scheduled in any phase.

---

## 2. Feature inventory against the prompt

Status legend: ✅ built · 🟡 partial · ❌ absent. Decision legend: **Keep** (as is) ·
**Add** (schedule it) · **Finish** (partial → complete) · **Defer** (post-pilot) · **Cut** (not for this product yet).

### §1 Angolan context

| Requirement | Status | Decision | Notes |
|---|---|---|---|
| pt-AO default, EN toggle | ✅ | Keep | |
| Grade levels + curricular tracks | ✅ | Keep | |
| Trimester calendar | ✅ | Keep | Year start date is per-school data, not code |
| BI / NIF value objects | ✅ | Keep | |
| Província → Município → Bairro | ✅ | Keep | Reference data only via Flyway |
| Kwanza + payment rails | 🟡 | Finish → Ph 9 | Invoicing real; adapters are skeletons; bank CSV is a stub |
| Offline-tolerant teacher/student flows | ✅ | Keep | Attendance + quizzes; **no frontend tests** |
| Lightweight on 3G | ❓ | Add → Ph 12 | Never measured; add a bundle budget + Lighthouse check |
| SMS fallback for guardians | ❌ | Add → Ph 7 | Events exist; nothing consumes them |
| Lei 22/11 (consent, access, erasure) | ❌ | Add → Ph 10 + 12 | Consent at signup should not wait for hardening |
| National exam tracking (Exame de Admissão) | ❌ | Add → Ph 11 | Never planned in any phase |

### §2 Administrative modules

| Module | Status | Decision | Notes |
|---|---|---|---|
| School & multi-campus | 🟡 | Defer | School = tenant works; campus grouping waits for a second school asking |
| Enrolment: online pre-registration, document checklist, waitlist | ❌ | Add → Ph 10 | `PENDING` exists as a status but `enrol()` confirms immediately — the workflow is dead code |
| SIS | ✅ | Keep | |
| Staff & HR: profiles + assignments | ✅ | Keep | |
| Staff & HR: contracts, payroll export | ❌ | Defer | Post-pilot |
| Class & timetable, rooms, conflict detection | ❌ | Add → Ph 8 | Turmas exist; no timetable at all. MVP said "light" |
| Attendance + automated absence alerts | 🟡 | Finish → Ph 7 | Capture done; alerts need notifications |
| Grading, boletim | ✅ | Keep | |
| **Pauta** (class-level trimester sheet) | ❌ | Add → Ph 8 | Boletim is per-student; the pauta is what the direction signs |
| Fees & invoicing | 🟡 | Finish → Ph 9 | See §1 row |
| Library | ❌ | Cut | Optional in prompt; no pilot demand |
| Reporting & DPE exports | ❌ | Add → Ph 11 | |
| Communication hub: announcements, circulars, calendar, broadcast | ❌ | Add → Ph 6 | Subject board is per-subject only; nothing school-wide exists |

### §3 Student learning portal

| Feature | Status | Decision | Notes |
|---|---|---|---|
| Quizzes: 4 types, timed, randomised, auto/manual grade, bank, offline, analytics, anti-cheat | ✅ | Keep | 27 tests pin it |
| Forum + moderation + upvotes | ✅ | Keep | Reply notification → Ph 7 |
| Subject board + low-bandwidth flag | ✅ | Keep | Flag only; no actual compression — fine for now |
| Assignments loop | ✅ | Keep | Deadline reminder → Ph 7 |
| Student dashboard + gradebook | ✅ | Keep | |
| **Guardian** gradebook / attendance view | ❌ | Add → Ph 6 | Endpoints already permit GUARDIAN; only the UI is missing. Listed in the original MVP |
| Guardian fee status | ✅ | Keep | Uncommitted, unverified in browser |
| Teacher ↔ guardian messaging | ❌ | Add → Ph 6 | |
| Academic calendar & event reminders | ❌ | Add → Ph 6 (+ Ph 7 for reminders) | |
| Study resources library | 🟡 | Keep | Board materials cover it; revisit if teachers ask for cross-turma sharing |
| Multi-child households | ✅ | Keep | |
| PWA | ✅ | Keep | |
| Gamification | ❌ | Cut | Optional in prompt |

### §4–§6 Platform & non-functional

| Item | Status | Decision | Notes |
|---|---|---|---|
| Modular monolith, ArchUnit boundaries, per-module Flyway | ✅ | Keep | |
| Kafka / Eureka / Gateway / Config Server | ❌ | Defer | Per ADR-0002 trigger; not before a second deployable |
| Multi-tenancy | 🟡 | Finish → Ph 5 | One leak fixed; the pattern (URL id + no tenant filter) recurs — needs a sweep |
| Audit log | 🟡 | Finish → Ph 5 + 12 | Only grades and fees write rows; enrolment, provisioning, moderation, role changes do not |
| JWT RS256, refresh token in HTTP-only cookie | ❌ | Add → Ph 12 | Plan said RS256 + cookie; built HS256 + localStorage. XSS exposure |
| Password policy, BI-scan encryption at rest | ❌ | Add → Ph 10 / 12 | |
| Testcontainers (Postgres) | ❌ | Add → Ph 5 | Plan said Testcontainers; suite uses H2. H2 already diverged once (`VALUE` keyword) |
| Frontend tests | ❌ | Add → Ph 5 | Zero. The offline queues are the riskiest client code in the product |
| Accessibility | ❌ | Add → Ph 12 | |
| UI wireframes / component list (prompt §7.4) | ❌ | Cut | Screens exist; a wireframe now would document, not design |
| Rollout / migration plan (prompt §7.6) | ❌ | Add → Ph 12 | |

---

## 3. Scope decisions

**Add**, because a pilot school needs them and nothing in the current code covers them:
guardian gradebook/attendance view · school-wide announcements + circulars · academic calendar ·
teacher↔guardian messaging · notifications (SMS/email/in-app) · timetable-light with conflict
detection · pauta PDF · pre-registration + document checklist + waitlist · national exam results ·
Lei 22/11 consent capture.

**One architecture decision needed (write ADR-0003):** school-wide announcements, calendar events
and teacher↔guardian messaging share an audience — families and the whole school — that is
distinct from per-subject content. Recommendation: a new `communications` module (Flyway prefix
`V14`) rather than stretching `subject-board` with a nullable subject. It maps to prompt §2.11 and
keeps the notification module purely a *delivery* concern, as designed.

**Defer** (revisit after the pilot): real Multicaixa / Unitel / Africell integrations (procurement),
bank CSV reconciliation beyond a single-bank importer, multi-campus grouping, staff contracts and
payroll export, WhatsApp and push channels, DPE exports beyond the enrolment summary, Kafka and
the service split.

**Cut** for now: library module, gamification, UI wireframes as a deliverable.

---

## 4. Phases

Each phase ends only when: the suite is green, every new endpoint has a role-gate test *and* a
cross-tenant test, `docs/events.md` lists any new event, and `tasks.md` is updated. That
definition of done is what was missing before — the tenant leak and the `grades` table silently
absent from the test schema both got through because nothing required them to be checked.

### Phase 5 — Land & stabilise (S)  ·  pilot line

**Goal:** `main` reflects reality; the quality floor stops things slipping through again.

- Commit in logical slices: Phase 4 race fix → fees module + UI → docker-compose → test suite + the three fixes.
- Walk `/admin/fees`, `/admin/defaulters`, `/guardian/invoices` in the browser; fix what's broken.
- Fees backend tests: billing fan-out idempotency, scholarship precedence, overpayment refusal, overdue sweep, defaulter aggregation, each adapter's `initiate`.
- **Tenant-isolation sweep.** Every repository query that takes an id from a URL and has no `tenantId` column is a candidate. Confirmed or likely exposed today: `GET /api/attendance?turmaId=` (`findByTurmaIdAndDateRecorded`), attendance summary counts, `GET /api/student/quizzes/available` (`findByTurmaIdAndStatus`), `EnrollmentRepository.findByTurmaId`, `StaffAssignmentRepository.findByTurmaId`, `GradeRepository.findByTurmaIdAndSubjectIdAndTrimesterKey`, fees `findByStudentId…`. Fix at the service layer (assert ownership) or add the column to the query; add a `CrossTenant…Test` per module so the pattern cannot recur unnoticed.
- Audit coverage: wire `AuditLogWriter` into enrolment, withdrawal, portal-user provisioning, forum moderation, quiz publish, invoice/payment edits.
- Frontend test harness: Vitest + Testing Library. First tests on `db/offlineDb.ts`, the attendance queue and the quiz-answer queue (deterministic v5 ids, flush-on-reconnect, replay safety).
- Postgres parity job in CI: Testcontainers spins Postgres 16, Flyway applies every migration from empty, `SchemaIntegrityTest` runs against it. Keep H2 for the fast API suite.
- Sync `docs/events.md` (stale field names, missing `InvoiceOverdue`, `PaymentReceived`, `QuizSubmitted`, `StudentEnrolled`).

**Exit:** a fresh clone runs `mvn verify` and `npm test` green; `git status` is clean; every module has at least one cross-tenant test.

### Phase 6 — Families: guardian portal, school communications, calendar (M)  ·  pilot line

**Goal:** a parent opens the app and sees everything about their child; the school can talk to everyone.

- Guardian portal pages: per-child grades (trimester table + boletim download), attendance summary, fee status, upcoming calendar. Endpoints already permit `GUARDIAN`; add a `GuardianDirectory`-backed ownership check so a guardian can only read their linked children.
- `communications` module (ADR-0003): school-wide announcements and circulars with audience (whole school / grade level / turma / guardians only), academic calendar events (exams, holidays, reuniões de pais) with `EventReminderDue`, teacher↔guardian message threads scoped to one student, with the director able to read.
- Student and guardian dashboards merge the school feed with the subject boards.
- Communication hub UI for admin/director: post, schedule, target.

**Exit:** a guardian with two children in different turmas sees both children's grades, attendance and invoices, the school calendar, and a circular sent to their child's turma; a teacher and that guardian exchange messages about one child.

### Phase 7 — Notifications (M)  ·  pilot line

**Goal:** the events that already fire reach people.

- `notifications` module: transactional outbox (`AFTER_COMMIT` listeners write a `notification` row; a poller dispatches), delivery status, retry with backoff.
- Channels behind one interface: SMS (Africa's Talking, stub logger in dev), email (SMTP), in-app inbox. Push and WhatsApp deferred.
- Consumers: `AttendanceMarkedAbsent` → guardian SMS in pt-AO · `GradePosted` → daily digest, not per grade · `InvoiceIssued` / `InvoiceOverdue` · `ForumReplyPosted` · `AssignmentDue` reminder (T-24h) · `EventReminderDue` · `UserProvisioned` → temp password by SMS/email (closes the Phase 2 stub that logs passwords to stdout).
- Guardian notification preferences (channel per event type, quiet hours).
- **Decision needed:** SMS provider contract (see §6).

**Exit:** marking a student absent produces an SMS in pt-AO within seconds on the stub, and on the real provider in staging; a provisioned user never sees their temp password in a log again.

### Phase 8 — Classroom completeness: timetable-light and pauta (M)  ·  pilot line

**Goal:** the two things a director asks for at trimester end and term start.

- Timetable: `turma × subject × teacher × weekday × period`, optional room. Conflict detection on save: teacher double-booked, room double-booked, turma double-booked. Teacher "today" view; student/guardian weekly view. No optimiser — schools enter their existing timetable.
- Pauta: class-level trimester grade sheet PDF (subject columns × student rows, averages, pass/fail per Angolan convention), signed block for the director. Same OpenPDF path as the boletim.
- Attendance per timetable period is *not* in scope — daily attendance stays until a pilot asks.

**Exit:** a director prints the T1 pauta for 10ª A; entering a lesson that double-books a teacher is rejected with a message naming the clash.

——— **Pilot-ready line.** A school can run a trimester on Phases 0–8: enrolment (manual), attendance with absence SMS, grades, boletim and pauta, quizzes, forums, boards, guardian portal, calendar, communications, and manual propina tracking via the stub adapter. ———

### Phase 9 — Fees: prototype → pilot-ready (M)

- Backend tests moved here if not finished in Phase 5.
- Overdue detection as a scheduled job, not a manual `sweep-overdue` endpoint.
- Receipt PDF per payment; invoice PDF in pt-AO with school header.
- Guardian-initiated payment UX for whichever rail the pilot chooses (§6); the other adapters stay skeletons.
- Bank reconciliation: one real CSV importer for the pilot's bank, matching on invoice reference; unmatched rows go to a review queue.
- Tenant-configurable IBAN and payment instructions (currently a hard-coded string in `BankTransferAdapter`).

**Exit:** monthly billing runs from a schedule, a guardian pays through the chosen rail (or uploads a transfer proof the secretary reconciles), and the invoice flips to PAID with a receipt.

### Phase 10 — Enrolment workflow, documents, consent (M)

- Public pre-registration form → `PENDING` enrolment (makes the existing status real); secretary approval queue; waitlist per turma when capacity is hit.
- Document checklist on the enrolment (BI, cartão de vacinas, transcript) using the documents module; encryption at rest for BI scans.
- Lei 22/11: consent capture at guardian signup and at pre-registration, versioned consent text, consent audit rows.
- CSV import for schools coming from spreadsheets (students, guardians, staff, subjects) — needed before the pilot's *next* matrícula season.

**Exit:** a guardian pre-registers a child online with documents attached; the secretary approves from a queue; a turma at capacity puts the next applicant on a waitlist.

### Phase 11 — Reporting, Ministry, national exams (M)

- Reporting read model built from events (eventually consistent), never from live queries.
- Exports: enrolment by turma and grade level, attendance rate, grade distribution — in the DPE shape once a real template is obtained.
- Ministry-viewer read-only role.
- National exam results: record Exame de Admissão and other national results per student per year; show on the student profile and boletim where applicable.

**Exit:** a downloadable enrolment summary in the DPE format; a ministry viewer sees aggregates and no personal data.

### Phase 12 — Hardening and pilot operations (L, ongoing)

- Security: RS256 signing, refresh token in an HTTP-only cookie, password policy, audit-log immutability at the DB role level, BI-scan encryption (if not done in 10), dependency scanning in CI.
- Lei 22/11 remainder: right-to-access export, right-to-erasure workflow for minors, retention policy.
- Load test (Gatling): 1 school × 500 students × 30 teachers; bundle-size budget and Lighthouse on 3G throttling.
- Backups and restore runbook; structured JSON logs; OTLP tracing to a real backend.
- Accessibility pass (WCAG 2.1 AA where feasible).
- Rollout playbook for a school moving from paper/spreadsheets.
- Pilot: one school runs a full trimester.

---

## 5. Next actions, in order

1. Commit the uncommitted work in slices (see Phase 5) and push.
2. Verify the three fees pages in the browser; fix anything broken.
3. Write the fees backend tests.
4. Tenant-isolation sweep: audit the repository list in Phase 5, fix, and add one cross-tenant test per module.
5. Wire audit rows into enrolment, provisioning, moderation, publish, invoice/payment edits.
6. Add Vitest + Testing Library; test the two offline queues.
7. Add the Testcontainers Postgres parity job to CI.
8. Sync `docs/events.md`; mark Phase 4 ✅ and Phase 5 🟡 in `tasks.md` (done in this revision).
9. Write ADR-0003 (communications module) and start Phase 6 with the guardian portal pages — pure UI over endpoints that already exist, highest value per hour in the plan.
10. Get answers to the two decisions that now block Phase 7 and Phase 9 (§6).

---

## 6. Decisions that now block work

Reordered from [INITIAL_PLAN.md §7](INITIAL_PLAN.md) by what each one blocks.

| Decision | Blocks | Recommendation if nobody decides |
|---|---|---|
| SMS provider (Africa's Talking vs Unitel/Movicel aggregator) | Phase 7 real delivery | Build against Africa's Talking's API shape behind the interface; the stub covers dev and demo |
| Pilot school | Phase 8 conventions (pauta layout, timetable periods), Phase 12 | Pick one before Phase 8 starts; otherwise build to the national default and expose overrides per school |
| Payment rail priority (Multicaixa / Unitel Money / bank) | Phase 9 | Bank transfer + reference is the lowest-procurement path; make its CSV importer real first |
| Hosting (AO provider / AWS Cape Town / Hetzner) | Phase 12 | Decide before load testing; data residency favours in-country or Cape Town |
| Multi-tenant login UX (subdomain vs selector) | Phase 6 guardian onboarding | School selector on login; subdomains when there are two schools |
