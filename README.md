# Skool — Angolan School Management System

Sistema de Gestão Escolar para o contexto angolano — matrícula, pautas, propinas, quizzes offline, fórum por disciplina, comunicação com encarregados de educação.

## Docs

- [Original prompt](school-management-system-prompt.md) — the full spec
- [Initial plan](INITIAL_PLAN.md) — phases, decisions, MVP scope
- [ADRs](docs/adr/) — architecture decisions
- [Domain events](docs/events.md) — cross-module event catalog

## Stack

Java 25 · Spring Boot 3.5 · PostgreSQL 16 · Flyway · Spring Security · Maven multi-module

## Local dev

```sh
docker compose up -d postgres minio     # infra only
mvn -pl app spring-boot:run              # run the app on host

# or run everything in containers:
docker compose --profile full up --build
```

- App: http://localhost:8080
- Health: http://localhost:8080/actuator/health
- Swagger: http://localhost:8080/swagger-ui.html
- MinIO console: http://localhost:9001 (skool / skool-dev-secret)

## Build

```sh
mvn verify              # compiles + runs tests + ArchUnit boundary check
mvn -pl app package     # produces app/target/skool.jar
```

## Module layout

```
common/           # shared kernel — domain primitives, web, security
modules/          # one module per bounded context (14 modules)
app/              # the single deployable that assembles all modules
docs/adr/         # architecture decision records
```

See [INITIAL_PLAN.md §3](INITIAL_PLAN.md#3-repository-layout-modular-monolith) for the module-by-module breakdown.
