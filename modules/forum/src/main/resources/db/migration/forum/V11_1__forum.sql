-- One forum per (subject, turma). Threads are top-level; posts are the flat list of
-- replies under a thread. This is a discussion board, not a Reddit-style tree — the
-- teacher wants to skim a Q&A, not chase a 6-level reply chain.
CREATE TABLE forums (
    id                UUID         PRIMARY KEY,
    tenant_id         UUID         NOT NULL,
    subject_id        UUID         NOT NULL,
    turma_id          UUID         NOT NULL,
    academic_year_id  UUID         NOT NULL,
    title             VARCHAR(200) NOT NULL,
    description       TEXT,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_forums_subject_turma UNIQUE (subject_id, turma_id, academic_year_id)
);

CREATE INDEX idx_forums_tenant_turma ON forums (tenant_id, turma_id);


CREATE TABLE forum_threads (
    id            UUID         PRIMARY KEY,
    tenant_id     UUID         NOT NULL,
    forum_id      UUID         NOT NULL REFERENCES forums (id) ON DELETE CASCADE,
    title         VARCHAR(200) NOT NULL,
    body          TEXT         NOT NULL,
    author_id     UUID         NOT NULL,   -- user id
    author_name   VARCHAR(255) NOT NULL,
    pinned        BOOLEAN      NOT NULL DEFAULT FALSE,
    hidden        BOOLEAN      NOT NULL DEFAULT FALSE,
    upvote_count  INTEGER      NOT NULL DEFAULT 0,
    reply_count   INTEGER      NOT NULL DEFAULT 0,
    last_activity_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_threads_forum        ON forum_threads (forum_id, last_activity_at DESC);
CREATE INDEX idx_threads_tenant       ON forum_threads (tenant_id);


CREATE TABLE forum_posts (
    id                UUID         PRIMARY KEY,
    tenant_id         UUID         NOT NULL,
    thread_id         UUID         NOT NULL REFERENCES forum_threads (id) ON DELETE CASCADE,
    body              TEXT         NOT NULL,
    author_id         UUID         NOT NULL,
    author_name       VARCHAR(255) NOT NULL,
    author_role       VARCHAR(24)  NOT NULL,  -- STUDENT / TEACHER / etc.
    upvote_count      INTEGER      NOT NULL DEFAULT 0,
    hidden            BOOLEAN      NOT NULL DEFAULT FALSE,
    marked_verified   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_posts_thread ON forum_posts (thread_id, created_at ASC);


-- One row per (thread_or_post, user) upvote — prevents double-voting.
CREATE TABLE forum_upvotes (
    target_type  VARCHAR(8) NOT NULL,   -- 'THREAD' | 'POST'
    target_id    UUID       NOT NULL,
    user_id      UUID       NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (target_type, target_id, user_id)
);
