-- Question bank: reusable questions organised by subject + grade level.
-- Payload columns are TEXT (JSON strings) rather than JSONB to keep the schema
-- portable across Postgres + H2 (tests). Serialization/deserialization happens
-- in the service layer with Jackson.
CREATE TABLE questions (
    id             UUID        PRIMARY KEY,
    tenant_id      UUID        NOT NULL,
    subject_id     UUID        NOT NULL,
    grade_level    VARCHAR(16) NOT NULL,
    prompt         TEXT        NOT NULL,
    question_type  VARCHAR(24) NOT NULL,
    -- JSON payload with type-specific data:
    --   MULTIPLE_CHOICE: {"options":[{"key":"A","text":"…","correct":true}, …]}
    --   TRUE_FALSE:      {"correct":true}
    --   SHORT_ANSWER:    {"acceptedAnswers":["…","…"]}
    --   ESSAY:           {"rubric":"…"}  -- graded manually
    payload        TEXT        NOT NULL,
    points         NUMERIC(4, 2) NOT NULL DEFAULT 1.00,
    created_by     UUID,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_questions_type CHECK (question_type IN ('MULTIPLE_CHOICE','TRUE_FALSE','SHORT_ANSWER','ESSAY')),
    CONSTRAINT ck_questions_points CHECK (points > 0)
);

CREATE INDEX idx_questions_tenant_subject ON questions (tenant_id, subject_id);
CREATE INDEX idx_questions_grade_level    ON questions (grade_level);


-- A quiz is a teacher-authored assessment tied to a subject × turma × academic year.
CREATE TABLE quizzes (
    id                UUID        PRIMARY KEY,
    tenant_id         UUID        NOT NULL,
    subject_id        UUID        NOT NULL,
    turma_id          UUID        NOT NULL,
    academic_year_id  UUID        NOT NULL,
    trimester_key     VARCHAR(4)  NOT NULL,
    title             VARCHAR(200) NOT NULL,
    instructions      TEXT,
    time_limit_seconds INTEGER,
    randomize_questions BOOLEAN   NOT NULL DEFAULT TRUE,
    randomize_options   BOOLEAN   NOT NULL DEFAULT TRUE,
    status            VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    opens_at          TIMESTAMPTZ,
    closes_at         TIMESTAMPTZ,
    created_by        UUID,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at      TIMESTAMPTZ,
    CONSTRAINT ck_quizzes_status CHECK (status IN ('DRAFT','PUBLISHED','CLOSED')),
    CONSTRAINT ck_quizzes_trim   CHECK (trimester_key IN ('T1','T2','T3'))
);

CREATE INDEX idx_quizzes_tenant_turma ON quizzes (tenant_id, turma_id);
CREATE INDEX idx_quizzes_subject      ON quizzes (subject_id);


-- Ordered list of question ids in a quiz. Same question can appear in multiple quizzes.
CREATE TABLE quiz_questions (
    quiz_id      UUID    NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
    question_id  UUID    NOT NULL REFERENCES questions(id),
    position     INTEGER NOT NULL,
    PRIMARY KEY (quiz_id, question_id)
);

CREATE INDEX idx_quiz_questions_quiz ON quiz_questions (quiz_id, position);


-- One row per student attempt. Randomization order is frozen at start_at.
CREATE TABLE quiz_attempts (
    id            UUID        PRIMARY KEY,
    tenant_id     UUID        NOT NULL,
    quiz_id       UUID        NOT NULL REFERENCES quizzes(id),
    student_id    UUID        NOT NULL,
    status        VARCHAR(16) NOT NULL DEFAULT 'IN_PROGRESS',
    started_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    submitted_at  TIMESTAMPTZ,
    auto_score    NUMERIC(6, 2),
    manual_score  NUMERIC(6, 2),
    total_points  NUMERIC(6, 2),
    -- JSON array of question ids in the student-specific order
    question_order TEXT       NOT NULL,
    -- Number of times the student switched tabs / lost focus (anti-cheating signal)
    tab_switch_count INTEGER  NOT NULL DEFAULT 0,
    CONSTRAINT ck_attempts_status CHECK (status IN ('IN_PROGRESS','SUBMITTED','GRADED')),
    CONSTRAINT uk_attempts_quiz_student UNIQUE (quiz_id, student_id)
);

CREATE INDEX idx_attempts_tenant_student ON quiz_attempts (tenant_id, student_id);
CREATE INDEX idx_attempts_quiz           ON quiz_attempts (quiz_id);


-- Client-generated ids so an offline attempt can flush without dedup pain.
CREATE TABLE quiz_answers (
    id            UUID        PRIMARY KEY,
    attempt_id    UUID        NOT NULL REFERENCES quiz_attempts(id) ON DELETE CASCADE,
    question_id   UUID        NOT NULL REFERENCES questions(id),
    -- Raw answer payload:
    --   MULTIPLE_CHOICE: {"selectedKeys":["A","C"]}
    --   TRUE_FALSE:      {"answer":true}
    --   SHORT_ANSWER:    {"text":"…"}
    --   ESSAY:           {"text":"…"}
    response      TEXT        NOT NULL,
    is_correct    BOOLEAN,
    points_earned NUMERIC(4, 2),
    teacher_feedback TEXT,
    answered_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_answers_attempt_question UNIQUE (attempt_id, question_id)
);

CREATE INDEX idx_answers_attempt ON quiz_answers (attempt_id);
