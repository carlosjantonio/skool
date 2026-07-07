CREATE TABLE grades (
    id                UUID           PRIMARY KEY,
    tenant_id         UUID           NOT NULL,
    student_id        UUID           NOT NULL,
    subject_id        UUID           NOT NULL,
    turma_id          UUID           NOT NULL,
    academic_year_id  UUID           NOT NULL,
    trimester_key     VARCHAR(4)     NOT NULL,
    value             NUMERIC(4, 2)  NOT NULL,
    weight            NUMERIC(3, 2)  NOT NULL DEFAULT 1.00,
    category          VARCHAR(16)    NOT NULL,
    notes             TEXT,
    recorded_by       UUID,
    recorded_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_grades_value    CHECK (value >= 0.00 AND value <= 20.00),
    CONSTRAINT ck_grades_weight   CHECK (weight > 0),
    CONSTRAINT ck_grades_trim     CHECK (trimester_key IN ('T1','T2','T3')),
    CONSTRAINT ck_grades_category CHECK (category IN ('TEST','EXAM','ASSIGNMENT','PARTICIPATION','OTHER'))
);

CREATE INDEX idx_grades_student_year  ON grades (student_id, academic_year_id);
CREATE INDEX idx_grades_turma_subject ON grades (turma_id, subject_id);
CREATE INDEX idx_grades_tenant_year   ON grades (tenant_id, academic_year_id);
