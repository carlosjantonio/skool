CREATE TABLE staff (
    id            UUID         PRIMARY KEY,
    tenant_id     UUID         NOT NULL,
    bi            VARCHAR(14),
    nif           VARCHAR(10),
    full_name     VARCHAR(255) NOT NULL,
    phone         VARCHAR(32),
    email         VARCHAR(255),
    qualification VARCHAR(255),
    hire_date     DATE,
    user_id       UUID,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_staff_tenant ON staff (tenant_id);
CREATE INDEX idx_staff_user   ON staff (user_id);

CREATE TABLE staff_assignments (
    id                UUID        PRIMARY KEY,
    tenant_id         UUID        NOT NULL,
    staff_id          UUID        NOT NULL REFERENCES staff (id) ON DELETE CASCADE,
    turma_id          UUID        NOT NULL,
    subject_id        UUID,
    academic_year_id  UUID        NOT NULL,
    role              VARCHAR(16) NOT NULL CHECK (role IN ('TEACHER','HEAD_TEACHER','ASSISTANT')),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_staff_assignments_tenant_year ON staff_assignments (tenant_id, academic_year_id);
CREATE INDEX idx_staff_assignments_turma       ON staff_assignments (turma_id);
CREATE INDEX idx_staff_assignments_staff       ON staff_assignments (staff_id);
