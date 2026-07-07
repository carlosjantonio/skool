CREATE TABLE documents (
    id            UUID         PRIMARY KEY,
    tenant_id     UUID         NOT NULL,
    filename      VARCHAR(255) NOT NULL,
    content_type  VARCHAR(128) NOT NULL,
    size_bytes    BIGINT       NOT NULL,
    document_type VARCHAR(32)  NOT NULL,
    owner_type    VARCHAR(32)  NOT NULL,
    owner_id      UUID         NOT NULL,
    storage_key   VARCHAR(512) NOT NULL,
    uploaded_by   VARCHAR(64),
    uploaded_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_documents_tenant ON documents (tenant_id);
CREATE INDEX idx_documents_owner  ON documents (owner_type, owner_id);
