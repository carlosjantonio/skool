CREATE TABLE students (
    id                UUID         PRIMARY KEY,
    tenant_id         UUID         NOT NULL,
    bi                VARCHAR(14),
    full_name         VARCHAR(255) NOT NULL,
    date_of_birth     DATE         NOT NULL,
    sex               VARCHAR(1)   NOT NULL CHECK (sex IN ('M','F')),
    comuna_ou_bairro  VARCHAR(128),
    address_line1     VARCHAR(255),
    health_notes      TEXT,
    photo_document_id UUID,
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_students_tenant ON students (tenant_id);
CREATE INDEX idx_students_bi     ON students (bi);

CREATE TABLE guardians (
    id         UUID         PRIMARY KEY,
    tenant_id  UUID         NOT NULL,
    bi         VARCHAR(14),
    full_name  VARCHAR(255) NOT NULL,
    phone      VARCHAR(32)  NOT NULL,
    email      VARCHAR(255),
    user_id    UUID,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_guardians_tenant ON guardians (tenant_id);
CREATE INDEX idx_guardians_email  ON guardians (email);

CREATE TABLE student_guardians (
    student_id           UUID        NOT NULL REFERENCES students (id)  ON DELETE CASCADE,
    guardian_id          UUID        NOT NULL REFERENCES guardians (id) ON DELETE CASCADE,
    relationship         VARCHAR(16) NOT NULL,
    is_primary           BOOLEAN     NOT NULL DEFAULT FALSE,
    is_emergency_contact BOOLEAN     NOT NULL DEFAULT FALSE,
    PRIMARY KEY (student_id, guardian_id)
);

CREATE INDEX idx_student_guardians_guardian ON student_guardians (guardian_id);
