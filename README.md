# YouTube English Tutor

Spring Boot app to generate quizzes from YouTube videos. It fetches transcripts (yt-dlp), generates questions (OpenAI), and uses RAG over transcript chunks (stored in Postgres) to show relevant snippets for wrong answers. A YouTube-backed catalog powers “Start Instantly” with a cached pool of short, captioned videos so random picks are instant without live API calls on click.

## Stack
- Java 17, Spring Boot (Web, Thymeleaf, Data JPA)
- Postgres (primary), H2 (dev if you switch URL)
- yt-dlp for transcripts
- OpenAI for questions and embeddings
- YouTube Data API (cached catalog pool; no live search on user clicks)

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
- Metrics export dir: `app.metrics.export-dir=logs`
- Application log file: defaults to `logs/spring.log` (set via `logging.file.path=logs`)
- YouTube catalog:
  - `app.youtube.api-key` (required for ingest)
  - `app.catalog.refresh.enabled=true`
  - `app.catalog.refresh-cron=0 0 */6 * * *` (default: every 6h)
  - `app.catalog.pool-size=100` (min 50, max 200)
  - Optional manual refresh guard: `app.admin.token=` (blank to disable; if set, POST /admin/catalog/refresh requires `X-Admin-Token`)
- Catalog prewarm (transcript + embeddings, nightly):
  - `app.prewarm.enabled=true`
  - `app.prewarm.cron=0 30 2 * * *` (default 2:30 AM)
  - `app.prewarm.nightly-cap=10`
- Pre-generated question packs (sizes, writing included):
  - `app.pregen.enabled=true`
  - `app.pregen.cron=0 10 3 * * *` (default 3:10 AM)
  - `app.pregen.nightly-cap=6`
  - `app.pregen.sizes=5,10,15`

## Recent Changes

- 2025-12-16: Start Instantly Catalog — nightly (cron) YouTube ingest builds a cached pool of captioned, duration-limited videos per category; “Surprise me” / “Shuffle a TED” now pick from DB (fallback samples if empty); Start Instantly browse grid is DB-driven with filters/search; watch page is video-first with background quiz prep and auto handoff to quiz, mini-player on quiz page; manual refresh endpoint added (`POST /admin/catalog/refresh`, optional `X-Admin-Token` guard). Added nightly prewarm (transcript+embeddings) and pre-generated question packs (sizes 5/10/15 with a writing prompt) reused on quiz start; watch page lets users pick question count before quiz.
- 2025-12-16: Per-learner history — tests now store `learner_id`, and list/view/submit/regenerate are filtered to the current anonymous cookie so different browsers cannot see each other’s history.
- 2025-12-15: Result page UX — feedback and flag now show centered toast popups styled to match the app; scroll position is preserved after submitting feedback/flag.
- 2025-12-15: Logging — enabled file logging to `logs/spring.log`, set root level to INFO, and silenced Spring Boot condition-evaluation reports to reduce log noise.
- 2025-12-15: Metrics export — default CSV export directory set to `logs/` (configurable via `app.metrics.export-dir`).

## Schema
Postgres DDL: `db/postgres-schema.sql`
- `tests`, `questions`, `wrong_questions`, `transcript_chunks`, `observability_events`, `catalog_videos`, `catalog_preparations`, `catalog_transcript_chunks`, `catalog_question_packs`

## Running
1) Ensure Postgres is up and the DB exists; apply `db/postgres-schema.sql`.
2) Install yt-dlp and ensure it’s on PATH or set `app.ytdlp.binary`.
3) Export API keys: `export OPENAI_API_KEY=...` and `export APP_YOUTUBE_API_KEY=...` (or set via config).
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
- 2025-12-12: Added `/admin/metrics` dashboard (last 7d counts/latency/errors) and nightly CSV export of `observability_events` to `app.metrics.export-dir` (default `logs/observability-YYYY-MM-DD.csv`).
- 2025-12-16: Scoped tests to the anonymous learner cookie: `tests` table now has `learner_id`, and all test operations (create, list, view, submit, regenerate, wrong-question review) enforce ownership.

## License
MIT. See `LICENSE`.
