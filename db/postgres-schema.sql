CREATE TABLE IF NOT EXISTS tests (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP,
    learner_id TEXT,
    video_url TEXT,
    video_title TEXT,
    score INTEGER,
    total_questions INTEGER,
    transcript TEXT
);

CREATE INDEX IF NOT EXISTS idx_tests_learner_id ON tests(learner_id);

CREATE TABLE IF NOT EXISTS questions (
    id BIGSERIAL PRIMARY KEY,
    test_id BIGINT REFERENCES tests(id) ON DELETE CASCADE,
    type VARCHAR(50),
    text TEXT,
    options TEXT,
    correct_answer TEXT
);

CREATE TABLE IF NOT EXISTS wrong_questions (
    id BIGSERIAL PRIMARY KEY,
    test_id BIGINT REFERENCES tests(id) ON DELETE CASCADE,
    question_id BIGINT REFERENCES questions(id) ON DELETE CASCADE,
    user_answer TEXT
);

CREATE TABLE IF NOT EXISTS transcript_chunks (
    id BIGSERIAL PRIMARY KEY,
    test_id BIGINT REFERENCES tests(id) ON DELETE CASCADE,
    content TEXT,
    embedding TEXT
);

CREATE TABLE IF NOT EXISTS observability_events (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP,
    learner_id TEXT,
    test_id BIGINT,
    question_id BIGINT,
    event_type VARCHAR(50),
    latency_ms BIGINT,
    token_usage INTEGER,
    retrieval_empty BOOLEAN,
    judge_result TEXT,
    feedback TEXT,
    payload TEXT
);

CREATE TABLE IF NOT EXISTS messages (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP,
    author TEXT,
    content TEXT,
    learner_id TEXT,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_messages_learner_id ON messages(learner_id);
CREATE INDEX IF NOT EXISTS idx_messages_deleted ON messages(deleted);

CREATE TABLE IF NOT EXISTS catalog_videos (
    id BIGSERIAL PRIMARY KEY,
    category VARCHAR(50) NOT NULL,
    video_id TEXT NOT NULL,
    video_url TEXT NOT NULL,
    title TEXT NOT NULL,
    channel_title TEXT,
    thumbnail_url TEXT,
    duration_seconds BIGINT,
    captions_available BOOLEAN,
    difficulty TEXT,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP,
    refreshed_at TIMESTAMP,
    last_seen_at TIMESTAMP,
    source_query TEXT
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_catalog_videos_category_video_id ON catalog_videos(category, video_id);
CREATE INDEX IF NOT EXISTS idx_catalog_videos_category_active ON catalog_videos(category, active);
CREATE INDEX IF NOT EXISTS idx_catalog_videos_video_id ON catalog_videos(video_id);
