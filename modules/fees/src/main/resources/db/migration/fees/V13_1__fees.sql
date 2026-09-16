-- FeeSchedule: reusable billing template a school defines once and applies to
-- many students. Kind + academic year + optional month/trimester keeps the same
-- table usable for propina mensal, matrícula, exame, uniforme, etc.
CREATE TABLE fee_schedules (
    id                UUID          PRIMARY KEY,
    tenant_id         UUID          NOT NULL,
    academic_year_id  UUID          NOT NULL,
    name              VARCHAR(200)  NOT NULL,
    kind              VARCHAR(24)   NOT NULL,
    amount            NUMERIC(14, 2) NOT NULL,
    currency          VARCHAR(3)    NOT NULL DEFAULT 'AOA',
    -- Optional filters for auto-billing: only students in this grade / trimester get
    -- an invoice when the schedule fires.
    grade_level       VARCHAR(16),
    trimester_key     VARCHAR(4),
    period_month      INTEGER,  -- 1-12 for propina mensal
    due_at            TIMESTAMPTZ   NOT NULL,
    active            BOOLEAN       NOT NULL DEFAULT TRUE,
    created_by        UUID,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_fee_kind CHECK (kind IN (
        'PROPINA_MENSAL','MATRICULA','EXAME','UNIFORME','MATERIAL','OUTRO')),
    CONSTRAINT ck_fee_amount CHECK (amount >= 0),
    CONSTRAINT ck_fee_month CHECK (period_month IS NULL OR (period_month BETWEEN 1 AND 12)),
    CONSTRAINT ck_fee_trim CHECK (trimester_key IS NULL OR trimester_key IN ('T1','T2','T3'))
);

CREATE INDEX idx_fee_schedules_tenant_year ON fee_schedules (tenant_id, academic_year_id);


-- Scholarship / bolsa: discount applied to a student's invoices for a validity window.
CREATE TABLE scholarships (
    id           UUID          PRIMARY KEY,
    tenant_id    UUID          NOT NULL,
    student_id   UUID          NOT NULL,
    kind         VARCHAR(24)   NOT NULL,
    -- percentage: 0.00-100.00 (e.g. 50.00 = half off)
    percentage   NUMERIC(5, 2),
    -- fixed_amount alternative
    fixed_amount NUMERIC(14, 2),
    valid_from   DATE          NOT NULL,
    valid_to     DATE,
    reason       TEXT,
    active       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_scholar_kind CHECK (kind IN ('FULL','PERCENTAGE','FIXED')),
    CONSTRAINT ck_scholar_pct  CHECK (percentage IS NULL OR (percentage BETWEEN 0 AND 100)),
    CONSTRAINT ck_scholar_fx   CHECK (fixed_amount IS NULL OR fixed_amount >= 0)
);

CREATE INDEX idx_scholarships_tenant_student ON scholarships (tenant_id, student_id);


-- Invoice: a specific bill against a specific student.
CREATE TABLE invoices (
    id                UUID           PRIMARY KEY,
    tenant_id         UUID           NOT NULL,
    student_id        UUID           NOT NULL,
    fee_schedule_id   UUID           NOT NULL REFERENCES fee_schedules (id),
    reference         VARCHAR(32)    NOT NULL UNIQUE,
    -- Denormalised for reporting — fee schedule can change later.
    title             VARCHAR(200)   NOT NULL,
    amount_gross      NUMERIC(14, 2) NOT NULL,
    amount_discount   NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    amount_net        NUMERIC(14, 2) NOT NULL,
    amount_paid       NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    currency          VARCHAR(3)     NOT NULL DEFAULT 'AOA',
    issued_at         TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    due_at            TIMESTAMPTZ    NOT NULL,
    status            VARCHAR(16)    NOT NULL DEFAULT 'ISSUED',
    scholarship_id    UUID,
    CONSTRAINT ck_invoice_status CHECK (status IN ('ISSUED','PARTIAL','PAID','OVERDUE','CANCELLED')),
    CONSTRAINT ck_invoice_gross  CHECK (amount_gross >= 0),
    CONSTRAINT ck_invoice_net    CHECK (amount_net >= 0),
    CONSTRAINT ck_invoice_paid   CHECK (amount_paid >= 0),
    CONSTRAINT uk_invoice_schedule_student UNIQUE (fee_schedule_id, student_id)
);

CREATE INDEX idx_invoices_tenant_student ON invoices (tenant_id, student_id);
CREATE INDEX idx_invoices_due            ON invoices (due_at);
CREATE INDEX idx_invoices_status         ON invoices (status);


-- Payment: a single payment event against an invoice. An invoice can receive
-- multiple partial payments; total is validated against amount_net.
CREATE TABLE payments (
    id                  UUID           PRIMARY KEY,
    tenant_id           UUID           NOT NULL,
    invoice_id          UUID           NOT NULL REFERENCES invoices (id),
    amount              NUMERIC(14, 2) NOT NULL,
    currency            VARCHAR(3)     NOT NULL DEFAULT 'AOA',
    method              VARCHAR(24)    NOT NULL,
    external_reference  VARCHAR(255),
    received_at         TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    received_by         UUID,
    notes               TEXT,
    CONSTRAINT ck_payment_amount CHECK (amount > 0),
    CONSTRAINT ck_payment_method CHECK (method IN (
        'STUB_MANUAL','MULTICAIXA_EXPRESS','UNITEL_MONEY','AFRICELL_MONEY','BANK_TRANSFER'))
);

CREATE INDEX idx_payments_tenant_invoice ON payments (tenant_id, invoice_id);
CREATE INDEX idx_payments_received       ON payments (received_at);
