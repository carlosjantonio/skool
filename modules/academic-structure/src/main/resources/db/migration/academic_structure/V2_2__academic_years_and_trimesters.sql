CREATE TABLE academic_years (
    id         UUID        PRIMARY KEY,
    tenant_id  UUID        NOT NULL,
    name       VARCHAR(32) NOT NULL,
    start_date DATE        NOT NULL,
    end_date   DATE        NOT NULL,
    is_current BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_academic_years_tenant_name UNIQUE (tenant_id, name)
);

CREATE INDEX idx_academic_years_tenant ON academic_years (tenant_id);

CREATE TABLE trimesters (
    id                UUID        PRIMARY KEY,
    academic_year_id  UUID        NOT NULL REFERENCES academic_years (id) ON DELETE CASCADE,
    trimester_key     VARCHAR(4)  NOT NULL,
    start_date        DATE        NOT NULL,
    end_date          DATE        NOT NULL,
    CONSTRAINT uk_trimesters_year_key UNIQUE (academic_year_id, trimester_key),
    CONSTRAINT ck_trimesters_key CHECK (trimester_key IN ('T1', 'T2', 'T3'))
);

CREATE INDEX idx_trimesters_year ON trimesters (academic_year_id);
