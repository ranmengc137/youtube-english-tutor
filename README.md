# YouTube English Tutor

Spring Boot app to generate quizzes from YouTube videos. It fetches transcripts (yt-dlp), generates questions (OpenAI), and uses RAG over transcript chunks (stored in Postgres) to show relevant snippets for wrong answers.

## Stack
- Java 17, Spring Boot (Web, Thymeleaf, Data JPA)
- Postgres (primary), H2 (dev if you switch URL)
- yt-dlp for transcripts
- OpenAI for questions and embeddings

## Config
`src/main/resources/application.properties` (or env vars):
- `spring.datasource.url=jdbc:postgresql://localhost:5432/youtube_english_tutor`
- `spring.datasource.username` / `spring.datasource.password` (set via env/local override)
- `app.ytdlp.binary=yt-dlp` (path to yt-dlp)
- `app.openai.model=gpt-5.1`
- `app.openai.api-key` (or `OPENAI_API_KEY`)
- `app.openai.embedding-model=text-embedding-3-small`
- RAG chunking: `app.rag.chunk-size=500`, `app.rag.chunk-overlap=100`, `app.rag.max-snippet-length=400`
- Transcript download path: `app.download.default-path=downloads`
- Metrics export dir: `app.metrics.export-dir=metrics-exports`

## Schema
Postgres DDL: `db/postgres-schema.sql`
- `tests`, `questions`, `wrong_questions`, `transcript_chunks`, `observability_events`

## Running
1) Ensure Postgres is up and the DB exists; apply `db/postgres-schema.sql`.
2) Install yt-dlp and ensure it’s on PATH or set `app.ytdlp.binary`.
3) Export OpenAI key: `export OPENAI_API_KEY=...`
4) Start app: `mvn spring-boot:run`
5) Open `http://localhost:8080`

## Flow
- Create Test: provide YouTube URL → fetch transcript (yt-dlp) → generate questions (OpenAI/dummy) → chunk transcript, embed, store.
- Take Test: answer questions.
- Results: scores, show correct vs yours, RAG snippet for wrong answers with highlighted answer terms.

## Notes
- Credentials are intentionally blank in `application.properties`; set via env or local overrides.
- Network calls to OpenAI/yt-dlp must be reachable.

## Changelog
- 2025-12-12: Added anonymous `learner_id` HttpOnly cookie, observability event logging (retrieval/judge) with per-request learner context, and `observability_events` table DDL scaffolding for metrics/export work.
- 2025-12-12: Added `/admin/metrics` dashboard (last 7d counts/latency/errors) and nightly CSV export of `observability_events` to `app.metrics.export-dir` (default `metrics-exports/observability-YYYY-MM-DD.csv`).
