-- Assignments ("trabalhos"): teacher creates → student submits (optionally with a
-- Document from the documents module) → teacher grades with rubric + feedback.
CREATE TABLE assignments (
    id                UUID         PRIMARY KEY,
    tenant_id         UUID         NOT NULL,
    subject_id        UUID         NOT NULL,
    turma_id          UUID         NOT NULL,
    academic_year_id  UUID         NOT NULL,
    trimester_key     VARCHAR(4)   NOT NULL,
    title             VARCHAR(200) NOT NULL,
    description       TEXT,
    rubric            TEXT,
    max_score         NUMERIC(6, 2) NOT NULL DEFAULT 20.00,
    due_at            TIMESTAMPTZ  NOT NULL,
    allow_late        BOOLEAN      NOT NULL DEFAULT TRUE,
    status            VARCHAR(16)  NOT NULL DEFAULT 'OPEN',
    created_by        UUID,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_assignments_status CHECK (status IN ('DRAFT','OPEN','CLOSED')),
    CONSTRAINT ck_assignments_trim   CHECK (trimester_key IN ('T1','T2','T3'))
);

CREATE INDEX idx_assignments_tenant_turma ON assignments (tenant_id, turma_id);
CREATE INDEX idx_assignments_due          ON assignments (due_at);


CREATE TABLE assignment_submissions (
    id              UUID         PRIMARY KEY,
    tenant_id       UUID         NOT NULL,
    assignment_id   UUID         NOT NULL REFERENCES assignments (id) ON DELETE CASCADE,
    student_id      UUID         NOT NULL,
    document_id     UUID,        -- optional link into the documents module
    notes           TEXT,
    submitted_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    is_late         BOOLEAN      NOT NULL DEFAULT FALSE,
    score           NUMERIC(6, 2),
    feedback        TEXT,
    graded_by       UUID,
    graded_at       TIMESTAMPTZ,
    status          VARCHAR(16)  NOT NULL DEFAULT 'SUBMITTED',
    CONSTRAINT ck_submissions_status CHECK (status IN ('SUBMITTED','GRADED','RETURNED')),
    CONSTRAINT uk_submissions_assignment_student UNIQUE (assignment_id, student_id)
);

CREATE INDEX idx_submissions_tenant_student ON assignment_submissions (tenant_id, student_id);
CREATE INDEX idx_submissions_assignment     ON assignment_submissions (assignment_id);
