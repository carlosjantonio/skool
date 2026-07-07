CREATE TABLE enrollments (
    id                UUID        PRIMARY KEY,
    tenant_id         UUID        NOT NULL,
    student_id        UUID        NOT NULL REFERENCES students (id) ON DELETE CASCADE,
    academic_year_id  UUID        NOT NULL,
    turma_id          UUID        NOT NULL,
    status            VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    enrolled_at       TIMESTAMPTZ,
    withdrawn_at      TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_enrollments_student_year UNIQUE (student_id, academic_year_id),
    CONSTRAINT ck_enrollments_status CHECK (status IN ('PENDING','ENROLLED','WITHDRAWN','GRADUATED'))
);

CREATE INDEX idx_enrollments_tenant_year ON enrollments (tenant_id, academic_year_id);
CREATE INDEX idx_enrollments_turma       ON enrollments (turma_id);
