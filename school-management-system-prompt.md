# Prompt: School Management System for the Angolan Context

## Project Overview
Build a full-featured **School Management System (SMS)** designed specifically for schools operating in Angola, covering both administrative operations and a student digital learning portal. The system must reflect Angola's educational structure (Ministério da Educação), connectivity realities, payment ecosystem, and language (Portuguese as the primary language, with English as an optional secondary language for international schools).

The platform should serve four main user roles: **Administrators**, **Teachers**, **Students**, and **Parents/Guardians**, with a fifth optional role for **Ministry/Inspection reporting**.

---

## 1. Angolan Context Requirements (must be built in from the start)

- **Language:** Portuguese (pt-AO) as default locale for UI, notifications, and generated documents (report cards, certificates). Support toggling to English.
- **Curriculum alignment:** Structure grade levels according to the Angolan system:
  - Ensino Primário (1ª–6ª classe)
  - I Ciclo do Ensino Secundário (7ª–9ª classe)
  - II Ciclo do Ensino Secundário (10ª–13ª classe), including tracks like Ciências Físicas e Biológicas, Ciências Económico-Jurídicas, Humanidades, etc.
- **Academic calendar:** Support Angola's school year structure (typically starting in February/March and split into trimesters — 1º, 2º, 3º Trimestre), not semesters.
- **Identification:** Student and staff profiles should support **Bilhete de Identidade (BI)** number and **NIF** (for staff/payroll) fields instead of assuming a Western SSN-style ID.
- **Administrative geography:** Address fields structured as Província → Município → Comuna/Bairro (not "State/City/Zip").
- **Currency & payments:** All fees, invoicing, and payroll in **Kwanza (AOA)**. Integrate (or stub with clear extension points for) local payment rails:
  - Multicaixa Express / referência de pagamento (ATM reference payments)
  - Unitel Money / Africell Money mobile wallets
  - Bank transfer reconciliation (BAI, BFA, BIC, Standard Bank Angola, etc.)
- **Connectivity resilience:** Angola has uneven and often expensive/slow internet access outside Luanda. The system must:
  - Be **offline-first or offline-tolerant** for core teacher/student functions (attendance, grade entry, quiz-taking with local caching and sync-on-reconnect).
  - Be **lightweight** — optimized assets, minimal data payloads, works acceptably on 3G.
  - Support **SMS notifications** as a fallback channel for parents without smartphones/data (e.g., absence alerts, grade summaries, fee due reminders), alongside email/push/WhatsApp.
- **Data protection:** Comply with Angola's **Lei de Proteção de Dados Pessoais (Lei n.º 22/11)** — consent tracking, data minimization, right to access/erasure for guardians of minors.
- **National exams:** Support tracking/reporting for **Exame de Admissão ao Ensino Superior** and other national exam results at the appropriate grade levels.

---

## 2. Core Administrative Modules

1. **School & Multi-Campus Management** — support single school or a group of schools/campuses under one institution.
2. **Enrollment & Registration** — online pre-registration ("matrícula"), document upload (BI, vaccination card, previous school transcript), waitlist management.
3. **Student Information System (SIS)** — full profile, guardians/encarregados de educação (support multiple guardians), emergency contacts, health notes.
4. **Staff & HR** — teacher/staff profiles, contracts, qualifications, class assignments, payroll-adjacent data (not full payroll, but exportable to a payroll system).
5. **Class & Timetable Management** — turmas, class scheduling, room/resource allocation, conflict detection.
6. **Attendance Tracking** — daily/per-class attendance, automated absence alerts to guardians (SMS + app).
7. **Grading & Report Cards (Pauta/Boletim)** — trimester-based grading matching Angolan report card formats, configurable grading scale (0–20, the standard Angolan scale), auto-calculated averages, printable PDF boletins.
8. **Fee Management & Invoicing** — tuition (propinas), fee schedules, discounts/scholarships (bolsas), Kwanza invoicing, payment tracking via local payment methods, defaulter reports.
9. **Library Management** (optional module) — book catalog, lending/returns.
10. **Reporting & Ministry Compliance** — exportable statistical reports in formats useful for submission to the Ministério da Educação or provincial education directorates (DPE).
11. **Communication Hub** — announcements, circulars, event calendar, SMS/email/push broadcast to specific classes or the whole school.

---

## 3. Student Learning Portal (Core Focus)

### 3.1 Quizzes & Online Tests
- Teachers can create quizzes/tests with multiple question types: multiple choice, true/false, short answer, essay/long-form (manually graded).
- Timed tests with auto-submit, randomized question order/option order to reduce cheating.
- Auto-grading for objective questions; manual grading workflow with rubric support for essays.
- Question banks organized by subject and grade level, reusable across classes/terms.
- Offline-tolerant test-taking: cache the quiz locally once loaded, sync answers when connection is restored (important given connectivity gaps).
- Result analytics for teachers (per-question difficulty, class averages) and for students (their own performance history).
- Anti-cheating basics: tab-switch detection (best-effort, not invasive), question randomization, time limits.

### 3.2 Subject Forums & Discussion Boards
- A dedicated **forum per subject per class** (e.g., "Matemática – 10ª Classe A") where students can post questions and reply.
- Teacher moderation tools: pin posts, delete/hide inappropriate content, mark a reply as "verified answer."
- Upvoting/helpful-marking for peer answers.
- Notification when a teacher or peer replies to a student's post.

### 3.3 Subject Board (Announcements/Resources per Subject)
- A **subject-level board** distinct from the forum: teachers post announcements, homework assignments, class materials (PDFs, slides, links), and due dates.
- Students see a consolidated feed of all subject boards they're enrolled in, plus a per-subject view.
- File attachments optimized for low bandwidth (compressed PDFs, optional low-res mode).

### 3.4 Additional Student Portal Features (recommended additions)
- **Assignment submission & tracking** — students upload homework, teachers grade and give feedback, deadline reminders.
- **Digital gradebook view for students/parents** — real-time access to grades, attendance, and teacher comments.
- **Personal dashboard** — upcoming tests, pending assignments, recent grades, forum activity, announcements — all in one view.
- **Study resources library** — teacher-curated materials per subject (videos, PDFs, past exams).
- **Parent/Guardian portal** — view child's grades, attendance, fee status, and receive alerts; support parents with multiple children in the same school.
- **Messaging between teacher and guardian** — direct, moderated messaging thread per student.
- **Mobile-responsive / PWA** — installable as a Progressive Web App so it behaves like a native app without requiring an app store, critical given variable Play Store/data access.
- **Multi-child household support** — one parent account linked to multiple students, possibly across different classes or even different campuses.
- **Academic calendar & event reminders** — exams, holidays, parent-teacher meetings (reuniões de pais).
- **Gamification (optional)** — badges/streaks for quiz completion and forum participation to encourage engagement.

---

## 4. Technology Stack (Fixed Decision)

- **Language & Runtime:** **Java 25**, using modern language features (virtual threads for high-concurrency I/O, records for DTOs, pattern matching for cleaner service logic).
- **Framework:** **Spring Boot 3.x** across all services.
- **Build tool:** Maven (multi-module reactor build, one module per service, sharing common parent POM for dependency version alignment) or Gradle multi-project — pick one and apply consistently.
- **Persistence:** **Spring Data JPA + Hibernate**, **PostgreSQL** as the primary datastore — **database-per-service** (each microservice owns its own schema/database instance; no service reaches into another's tables directly).
- **Migrations:** **Flyway**, versioned per service.
- **Security:** **Spring Security** + **JWT** issued by the Identity Service and validated by each downstream service (stateless auth — no shared session store).
- **Inter-service communication:**
  - **Synchronous:** REST (OpenFeign or RestClient) for request/response calls that need an immediate answer (e.g., API Gateway → a service).
  - **Asynchronous:** **Apache Kafka** (or RabbitMQ if you prefer simpler ops) as an event bus for cross-service side effects (e.g., "GradeRecorded" event triggers the Notification Service without Grading Service knowing Notification Service exists). This is the key mechanism that keeps services decoupled.
- **Service discovery & config:** **Spring Cloud Netflix Eureka** (discovery) + **Spring Cloud Config Server** (centralized, environment-specific configuration).
- **API Gateway:** **Spring Cloud Gateway** — single public entry point, JWT validation, routing to internal services, rate limiting.
- **Resilience:** **Resilience4j** (circuit breakers, retries, bulkheads) on all inter-service REST calls so one service degrading doesn't cascade.
- **Containerization:** Docker — one image per service; **Docker Compose** for local dev, with a clear path to Kubernetes later if scale demands it.
- **Observability:** Spring Boot Actuator + a shared logging/tracing approach (e.g., Micrometer + OpenTelemetry) so issues can be traced across service boundaries — important once you have 10+ independently deployed services.

---

## 5. Microservices Architecture

Each service below should be **independently deployable, own its own database, and expose a versioned REST API**. Cross-service coordination happens via the **API Gateway** (for client-facing requests) and **Kafka events** (for background/async side effects) — never via direct database access between services.

| # | Service | Core Responsibility | Owns Data For |
|---|---------|---------------------|----------------|
| 1 | **Identity & Access Service** | Authentication, JWT issuance/refresh, RBAC (Admin, Director, Secretary, Teacher, Student, Guardian, Ministry viewer), password/reset flows | Users, roles, permissions, credentials |
| 2 | **School & Academic Structure Service** | Schools/campuses, provinces/municípios, turmas (classes), subjects, timetable, room/resource allocation | Schools, classes, subjects, timetables |
| 3 | **Student Information Service (SIS)** | Student profiles, guardians/encarregados, enrollment ("matrícula"), BI/NIF, emergency contacts, health notes | Students, guardians, enrollment records |
| 4 | **Staff & HR Service** | Teacher/staff profiles, contracts, qualifications, class/subject assignments | Staff records, assignments |
| 5 | **Attendance Service** | Daily/per-class attendance capture, absence flagging | Attendance records |
| 6 | **Grading & Report Card Service** | Grade entry, trimester averages, boletim/pauta generation (PDF), Angolan 0–20 scale logic | Grades, report cards |
| 7 | **Quiz & Assessment Service** | Quiz/test creation, question banks, timed test delivery, auto-grading, offline sync of answers | Quizzes, questions, submissions, results |
| 8 | **Assignment Service** | Homework/assignment creation, student submissions, feedback, deadlines | Assignments, submissions |
| 9 | **Forum Service** | Per-subject/per-class discussion forums, posts, replies, moderation, upvotes | Forum posts/threads |
| 10 | **Subject Board / Content Service** | Announcements, class materials, links, subject-level feed | Board posts, materials metadata |
| 11 | **Fee & Payments Service** | Invoicing, propinas, Kwanza billing, payment adapter (Multicaixa/Unitel Money/bank transfer reconciliation), defaulter tracking | Invoices, payments, fee schedules |
| 12 | **Notification Service** | Consumes events from other services (grade posted, absence recorded, fee due, forum reply) and dispatches via SMS (Africa's Talking), email, push, in-app | Notification logs, delivery status |
| 13 | **Document/File Storage Service** | Upload/retrieval of BI scans, transcripts, assignment attachments, board materials (wraps S3-compatible/MinIO storage) | File metadata, storage references |
| 14 | **Reporting & Ministry Compliance Service** | Aggregates data (read-only, via events or scheduled pulls) into statistical exports for Ministério da Educação / DPE submissions | Aggregated/derived reporting data only |
| 15 | **API Gateway** | Single public entry point, JWT validation, request routing, rate limiting | None (stateless routing layer) |
| 16 | **Config Server** | Centralized externalized configuration for all services, per environment | Configuration only |
| 17 | **Discovery Server (Eureka)** | Service registry so the Gateway and services can find each other dynamically | Service registry only |

### Notes on independence
- **No shared database.** Each service in the table above gets its own PostgreSQL schema or instance. If Service A needs data owned by Service B, it either calls B's API or consumes an event B previously published — it never queries B's tables.
- **The Reporting Service should be read-only and eventually-consistent** — it builds its own denormalized view from events published by other services, rather than querying them live, so a reporting outage never affects operational services.
- **The Notification Service is purely reactive** — it has no business logic of its own about *when* to notify; it just listens for domain events (`AttendanceMarkedAbsent`, `GradePosted`, `InvoiceOverdue`, `ForumReplyPosted`) and decides *how* to deliver them (SMS vs push vs email) based on guardian/user preference.
- **Start with a "modular monolith" option if timeline is tight:** given you're building solo with Claude's help, consider building all 17 as **separate Maven modules within one deployable unit first** (a well-structured monolith with service-like internal boundaries), then physically splitting modules into independent deployments once the domain boundaries have proven stable. This avoids paying the full distributed-systems tax (network calls, eventual consistency, deployment orchestration) before you actually need it — but design the module boundaries from day one exactly as described above, so the split later is a deployment change, not a redesign.

---

## 6. Technical & Non-Functional Requirements

- **Roles & permissions:** Fine-grained RBAC (Admin, Director/Diretor, Secretary/Secretaria, Teacher, Student, Guardian, Ministry viewer), enforced centrally by the Identity Service and validated per-request at the Gateway.
- **Multi-tenancy:** Design so the system could serve multiple independent schools if scaled beyond one institution (tenant ID as a first-class field across all services).
- **Localization-ready:** All strings externalized for translation; date/number formatting per pt-AO locale.
- **Scalability & low-resource hosting:** Consider that hosting may be cost-constrained; design for efficient resource use (Java 25 virtual threads help here for I/O-heavy services), and the possibility of local/regional hosting (data residency considerations).
- **Security:** Password policies, encrypted storage of sensitive documents (BI copies, health notes), audit logs for grade changes, JWT expiry/rotation.
- **Accessibility:** Basic accessibility (readable fonts, sufficient contrast, alt text) given varied device quality.
- **Offline support:** Since assessment/attendance/grading happen at the edge (classrooms with poor connectivity), the **client** (web/PWA) is responsible for local caching and sync — the backend services should expose idempotent sync endpoints (e.g., accept a batch of offline-queued attendance records with client-generated timestamps/UUIDs to prevent duplicate processing on retry).

---

## 7. Deliverables to Request from the Builder

1. Multi-module Maven/Gradle project structure reflecting the 17 services above, with a shared parent module for common dependencies/versions.
2. Per-service: JPA entity model, Flyway migration scripts, REST API (OpenAPI/Swagger spec), and a clear list of events it publishes/consumes.
3. Docker Compose setup wiring together all services, Postgres instances, Kafka, Eureka, Config Server, and the Gateway for local development.
4. UI wireframes or component list for: Admin dashboard, Teacher dashboard, Student portal, Parent portal.
5. A working MVP prioritizing: Identity, School/Academic Structure, SIS, Attendance, Grading, Quiz/Assessment, Forum, Subject Board, and Fee/Payments — the Reporting and Notification services can follow once core domain events exist to feed them.
6. A rollout/migration plan for schools moving from paper-based or spreadsheet-based records.

---

*Note: Given this is being built solo with AI assistance, seriously weigh the "modular monolith first" approach in Section 5 — it keeps the architecture correct without requiring you to operate a full distributed system (Kafka, Eureka, multiple databases, multiple deployments) from day one.*
