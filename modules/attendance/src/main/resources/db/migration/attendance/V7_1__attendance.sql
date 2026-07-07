CREATE TABLE attendance_records (
    id             UUID        PRIMARY KEY,
    tenant_id      UUID        NOT NULL,
    turma_id       UUID        NOT NULL,
    student_id     UUID        NOT NULL,
    date_recorded  DATE        NOT NULL,
    status         VARCHAR(16) NOT NULL,
    notes          TEXT,
    recorded_by    UUID,
    recorded_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_attendance_student_turma_date UNIQUE (student_id, turma_id, date_recorded),
    CONSTRAINT ck_attendance_status CHECK (status IN ('PRESENT','ABSENT','LATE','EXCUSED'))
);

CREATE INDEX idx_attendance_tenant_date ON attendance_records (tenant_id, date_recorded);
CREATE INDEX idx_attendance_turma_date  ON attendance_records (turma_id, date_recorded);
CREATE INDEX idx_attendance_student     ON attendance_records (student_id);
