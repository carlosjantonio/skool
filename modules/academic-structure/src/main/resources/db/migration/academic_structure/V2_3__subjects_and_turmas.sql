CREATE TABLE subjects (
    id          UUID         PRIMARY KEY,
    tenant_id   UUID         NOT NULL,
    name        VARCHAR(128) NOT NULL,
    code        VARCHAR(32)  NOT NULL,
    grade_level VARCHAR(16)  NOT NULL,
    CONSTRAINT uk_subjects_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX idx_subjects_tenant ON subjects (tenant_id);

CREATE TABLE turmas (
    id                UUID        PRIMARY KEY,
    tenant_id         UUID        NOT NULL,
    school_id         UUID        NOT NULL REFERENCES schools (id),
    academic_year_id  UUID        NOT NULL REFERENCES academic_years (id),
    name              VARCHAR(32) NOT NULL,
    grade_level       VARCHAR(16) NOT NULL,
    track             VARCHAR(40) NOT NULL,
    capacity          INTEGER     NOT NULL CHECK (capacity > 0),
    CONSTRAINT uk_turmas_year_school_name UNIQUE (academic_year_id, school_id, name)
);

CREATE INDEX idx_turmas_tenant_year ON turmas (tenant_id, academic_year_id);
