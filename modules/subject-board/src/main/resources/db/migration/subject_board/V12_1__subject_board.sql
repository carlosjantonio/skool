-- The subject board is what a student sees when they open a subject: a stream of
-- announcements + class materials (PDFs, slides, links). One entry per row, typed
-- by "kind" so a single feed query returns them in date order.
CREATE TABLE board_entries (
    id                UUID         PRIMARY KEY,
    tenant_id         UUID         NOT NULL,
    subject_id        UUID         NOT NULL,
    turma_id          UUID         NOT NULL,
    academic_year_id  UUID         NOT NULL,
    kind              VARCHAR(16)  NOT NULL,   -- ANNOUNCEMENT | MATERIAL | LINK
    title             VARCHAR(200) NOT NULL,
    body              TEXT,
    -- MATERIAL entries reference an id from the documents module. Uploads happen
    -- through /api/documents; this table only stores the pointer.
    document_id       UUID,
    -- LINK entries store an external URL (course video, external reading, etc.).
    external_url      VARCHAR(1024),
    -- For due-date-aware surfaces on the student feed (e.g. "próximo dia 12/03").
    due_at            TIMESTAMPTZ,
    pinned            BOOLEAN      NOT NULL DEFAULT FALSE,
    -- Low-bandwidth flag: when true, the student PWA fetches document metadata but
    -- doesn't prefetch the file.
    low_bandwidth     BOOLEAN      NOT NULL DEFAULT FALSE,
    author_id         UUID,
    author_name       VARCHAR(255),
    published_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_board_kind CHECK (kind IN ('ANNOUNCEMENT','MATERIAL','LINK'))
);

CREATE INDEX idx_board_tenant_turma   ON board_entries (tenant_id, turma_id, published_at DESC);
CREATE INDEX idx_board_subject_turma  ON board_entries (subject_id, turma_id, published_at DESC);
CREATE INDEX idx_board_due            ON board_entries (due_at);
