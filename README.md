# Kino Backend

REST API built with Java 17, Spring Boot, Spring Security, JWT, Spring Data JPA and PostgreSQL.

## Requirements

- JDK 17 or later
- Maven 3.9+
- PostgreSQL 14 or later

Start a local PostgreSQL database with Docker Compose:

```powershell
docker compose up -d postgres
```

Or create the local database and user manually (adjust credentials as needed):

```sql
CREATE USER kino WITH PASSWORD 'kino_dev_password';
CREATE DATABASE kino OWNER kino;
```

Run from this directory:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/kino"
$env:DB_USERNAME="kino"
$env:DB_PASSWORD="kino_dev_password"
$env:JWT_SECRET="replace-with-a-random-secret-at-least-32-bytes-long"
mvn spring-boot:run
```

The API listens on `http://localhost:8081` (port 8080 is often taken by other local tools such as Jenkins; override it with `PORT`). CORS allows the Angular dev server at `http://localhost:4200` for GET, POST, PUT, DELETE and OPTIONS, including the `Authorization` header. Both Kino frontend domains (`https://kino-streaming-frontend.vercel.app` and `https://kino-streamingg-frontend.vercel.app`) are always allowed through `CORS_ADDITIONAL_ORIGINS`, so renaming the Vercel project between them does not break login. Set `CORS_ALLOWED_ORIGINS` to a comma-separated list to allow other origins, for example `https://kino-streamingg-frontend.vercel.app,https://kino-streamingg-frontend-*.vercel.app,http://localhost:4200`. Entries containing `*` are patterns (useful for Vercel preview deployments); spaces, quotes, `;` separators and trailing slashes are tolerated. For every `https://<project>.vercel.app` entry the backend also allows `https://<project>-*.vercel.app` (the per-deployment URL opened by Vercel's "Visit" button and the git branch URLs; disable with `CORS_ALLOW_VERCEL_PREVIEWS=false`), and it always allows `http://localhost:<any port>` / `http://127.0.0.1:<any port>` so a local Angular dev server can use the deployed API (disable with `CORS_ALLOW_LOCALHOST=false`). The API uses bearer tokens, not cookies, so these extra origins cannot reuse a signed-in session. The backend logs the effective origins at startup (`CORS allowed origins: ...`), which is the first thing to check if the browser reports a CORS error.

`.env.example` lists every supported environment variable. `JWT_SECRET` must be at least 32 bytes long; the application refuses to start otherwise.

### Supabase database

The backend can use a Supabase PostgreSQL database instead of the local Docker one. Only the backend connects to the database; the Angular frontend keeps calling this REST API.

1. Run `src/main/resources/db/supabase/schema.sql` once in the Supabase SQL Editor. It creates the tables, indexes and constraints, and enables Row Level Security so the public Supabase Data API cannot read them.
2. Copy `.env.example` to `.env` (the backend loads it automatically) and fill in the **Session pooler** connection values from Supabase > Connect:

   ```properties
   DB_URL=jdbc:postgresql://aws-<n>-<region>.pooler.supabase.com:5432/postgres?sslmode=require
   DB_USERNAME=postgres.<project-ref>
   DB_PASSWORD=<your-supabase-database-password>
   DDL_AUTO=validate
   ```

3. Run `mvn spring-boot:run`. With `DDL_AUTO=validate`, startup fails if the schema does not match the entities.

Use the Session pooler (port 5432) rather than the direct connection (`db.<project-ref>.supabase.co`), which only resolves over IPv6 on the free plan and fails with `UnknownHostException` on IPv4-only networks. Copy the pooler host exactly: it can start with `aws-0-` or `aws-1-`. If you use the Transaction pooler (port 6543), add `&prepareThreshold=0` to `DB_URL`.

Run the test suite (uses an in-memory H2 database, no PostgreSQL needed):

```powershell
mvn test
```

### Docker

Build and run the API as a container:

```powershell
docker build -t kino-backend .
docker run --rm -p 8081:8081 `
  -e DB_URL="jdbc:postgresql://host.docker.internal:5432/kino" `
  -e DB_USERNAME="kino" -e DB_PASSWORD="kino_dev_password" `
  -e JWT_SECRET="replace-with-a-random-secret-at-least-32-bytes-long" `
  kino-backend
```

### Deploy to Render

The repository includes a Render Blueprint (`render.yaml`) that deploys the Docker image as a web service in the `virginia` region (same AWS region as the Supabase project), with `/api/health` as health check.

1. Render > **New** > **Blueprint**, connect GitHub and select this repository. Render reads `render.yaml`.
2. Fill in the secret values it asks for: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (Supabase Session pooler values) and `CORS_ALLOWED_ORIGINS` (frontend URL). `JWT_SECRET` is generated automatically.
3. Click **Apply**. When the deploy finishes, `https://<service-name>.onrender.com/api/health` must return `{"status":"UP","database":"UP"}`.

Render injects `PORT` (Spring reads it automatically). On the free plan the service sleeps after 15 minutes without traffic, and the first request afterwards takes about a minute. To avoid that:

- **Keep-alive**: `.github/workflows/keep-alive.yml` keeps one GitHub Actions job running that calls `/api/health` every 4 minutes for almost 6 hours; a new run is queued every 30 minutes and takes over when the previous one ends (`concurrency` keeps only one running), so the service never reaches the 15-minute sleep timeout. GitHub delays scheduled runs (often by hours), which is why a single short ping per run was not enough. The repository is public, so these Actions minutes are free; one always-on service fits in Render's 750 free instance hours per month. For extra safety add a free external monitor (UptimeRobot or cron-job.org) on the same URL every 5 minutes. Set the `KINO_HEALTH_URL` repository variable if the service URL changes.
- **Faster cold starts**: the Docker image runs the extracted jar (`-Djarmode=tools extract`), limits JIT compilation (`-XX:TieredStopAtLevel=1`) for the free plan's small CPU, bootstraps JPA repositories in the background and disables JMX. Responses are gzip-compressed.

## Endpoints

- `GET /api/live`, `GET /api/live/{id}` and `GET /api/ai/trending-insights` are public (see below).
- `GET /api/health` also reports `build` (build time of the running version) and `cors` (the origins understood from `CORS_ALLOWED_ORIGINS`), the quickest way to check a deployment.
- `GET /api/health` is public and returns `{ status, database, timestamp }`; it is used by Render as health check.

- `POST /api/auth/register` creates an account (every account can publish videos and go live, so it gets the `CREATOR` role) and returns `{ accessToken, tokenType, email, displayName, role }`. Repeated spaces in the name are collapsed; an email that is already registered returns `409` with the message also under `validationErrors.email`.
- `POST /api/auth/login` authenticates and returns the same response. Send the token as `Authorization: Bearer <token>` (the scheme is case-insensitive and surrounding spaces are ignored).
- `GET /api/videos` returns the public video catalog.
- `GET /api/videos/trending` returns up to 20 videos ordered by recorded playback count.
- `GET /api/videos/{id}` returns the video details and streaming URL.
- `POST /api/videos/{id}/views` requires a bearer token, increments the view count, and records the viewer/video pair once for recommendations.
- `GET /api/feed/recommendations` requires a bearer token and returns `{ strategy, generatedAt, recommendations }`, ranked with collaboration and popularity signals.
- `GET /api/videos/search?q=` returns up to 50 videos whose title contains the query (case-insensitive). A blank query returns an empty list.
- `GET /api/users/me` requires a bearer token and returns the authenticated user's `id`, `displayName`, `email` and `role`.
- `GET /api/creator/videos`, `GET /api/creator/videos/analytics`, and `POST /api/creator/videos` require a bearer token (any signed-in account).
- `DELETE /api/creator/videos/{id}` requires a bearer token, deletes a video owned by the caller together with its recorded interactions, and returns `204`. Videos owned by another creator return `404`.

### Live streaming

- `GET /api/live` (public) lists the broadcasts that are live now; `GET /api/live/{id}` returns one broadcast (live or ended).
- `POST /api/creator/live` (signed in) starts a broadcast with `title`, `category`, optional `description` / `thumbnailUrl` and `sourceType`:
  - `CAMERA`: the creator's browser captures the camera and sends the video to each viewer over WebRTC. No `playbackUrl` is needed.
  - `HLS` (default when omitted): the creator streams with OBS to a media service (Cloudflare Stream, Mux, AWS IVS, nginx-rtmp…) and sends its public `.m3u8` as `playbackUrl` (required, otherwise `400` under `validationErrors.playbackUrl`).

  A creator can only have one live broadcast; a second one returns `409`.
- `GET /api/creator/live/current` returns the caller's live broadcast, or `204` when they are offline.
- `POST /api/creator/live/{id}/end` ends the caller's broadcast.

#### Camera broadcasts (WebRTC signaling)

The video never passes through Kino: the backend only relays one SDP offer and one SDP answer per viewer (ICE candidates are included in the SDP, non-trickle), and then the two browsers talk directly.

1. Viewer (signed in): `POST /api/live/{id}/viewers` with `{ "sdp": "<offer>" }` returns `201 { sessionId, answer: null }`; then polls `GET /api/live/{id}/viewers/{sessionId}` until `answer` is set.
2. Creator's browser: polls `GET /api/creator/live/{id}/viewers/pending` every couple of seconds (it also works as a heartbeat) and answers each viewer with `POST /api/creator/live/{id}/viewers/{sessionId}/answer` and `{ "sdp": "<answer>" }` (`204`).

A camera broadcast is listed in `GET /api/live` only while the creator's browser keeps polling (30-second timeout), so closing the tab hides it. Unanswered requests expire after 60 seconds. The signaling state is kept in memory, which works with the single Render instance; with several instances it would have to move to a shared store. Browsers connect with public STUN servers; viewers behind very restrictive networks may need a TURN server (configured in the frontend).

Before deploying this version on an existing database, run these migrations in the Supabase SQL Editor, in order: `2026-11-13-live-streams.sql`, `2026-11-13-camera-live.sql` and `2026-11-13-everyone-creator.sql` (all in `src/main/resources/db/supabase/`, all idempotent).

### Kino IA

`GET /api/ai/trending-insights` (public) returns `{ generatedAt, source, summary, mostWatched, rising, topCategories, totalViews, liveNow }`: the most watched videos, videos published in the last 7 days that are gaining views, the categories with the most views and a short summary. The result is cached for `AI_CACHE_TTL` (5 minutes by default).

`source` is `kino-analytics` while no AI provider is connected (the summary is written from Kino's statistics) and `ai` when the summary comes from the AI.

Kino ships two AI clients; pick one with `AI_PROVIDER`:

| `AI_PROVIDER` | Client | Default `AI_MODEL` | Key from |
|---|---|---|---|
| `gemini` | Google Gemini API | `gemini-flash-lite-latest` | https://aistudio.google.com/apikey |
| `groq` | OpenAI-compatible | `llama-3.3-70b-versatile` | https://console.groq.com/keys |
| `openrouter` | OpenAI-compatible | `meta-llama/llama-3.3-70b-instruct:free` | https://openrouter.ai/keys |
| `mistral` | OpenAI-compatible | `mistral-small-latest` | https://console.mistral.ai/api-keys |
| `github` | OpenAI-compatible (GitHub Models) | `openai/gpt-4o-mini` | GitHub personal access token with `models:read` |
| `cerebras` | OpenAI-compatible | `llama-3.3-70b` | https://cloud.cerebras.ai |
| `openai` | OpenAI-compatible | `gpt-4o-mini` | https://platform.openai.com/api-keys |
| `openai-compatible` | Any `/chat/completions` API (Together, DeepSeek, Ollama…) | set `AI_MODEL` | set `AI_BASE_URL` too |

To turn it on, set in the Render dashboard (or `.env` locally): `AI_ENABLED=true`, `AI_PROVIDER=<provider>` and `AI_API_KEY=<key>`. `AI_MODEL` and `AI_BASE_URL` are optional overrides (provider model names change over time; check the provider's model list if a call fails). Never commit the key: only the backend uses it, the frontend never sees it.

`GET /api/health` reports `ai: { enabled, provider, model, apiKeyConfigured, active }` (never the key), so after saving the variables you can confirm that `active` is `true`. `GET /api/ai/trending-insights` then returns `source: "ai"`; results are cached for `AI_CACHE_TTL`.

If the provider fails, times out (20 s), hits a rate limit or returns no text, Kino logs a warning and falls back to its own summary, so the dashboard keeps working. Another provider can be added with a Spring `@Component` implementing `com.kino.backend.ai.AiTextGenerator`.

#### Chat and recommendations

- `POST /api/ai/chat` (public) with `{ "messages": [{ "role": "user" | "assistant", "content": "..." }] }` answers as Kino IA: `{ reply, source, suggestions, generatedAt }`. The last message must come from the user; only the last 12 messages are sent to the model, each one up to 1000 characters. The prompt includes the current statistics and the 60 most watched videos, so the model only recommends real titles; `suggestions` are the catalog videos mentioned in the reply. Without an AI provider (or if it fails), Kino answers with built-in rules: most watched, new releases, a category ("Recomiéndame un documental"), live streams or general recommendations.
- `GET /api/ai/recommendations?category=Documental` (public) returns `{ category, source, summary, recommendations: [{ video, reason, score }], generatedAt }`. Videos are ranked by popularity plus a bonus for new releases (7 and 30 days); the AI writes the summary when it is active. Results are cached per category for `AI_CACHE_TTL`.
- Each client can ask `AI_CHAT_REQUESTS_PER_MINUTE` questions per minute (12 by default, per user when signed in and per IP otherwise); above that the API answers `429`.

#### Workers

| Worker | What it does | Settings |
|---|---|---|
| `AiWorker` | Bounded thread pool (`kino-ai-worker-*`) that answers chat questions off the request threads. A question that takes longer than the timeout gets the built-in answer; when the queue is full the API answers `503`. | `AI_WORKER_THREADS` (2), `AI_WORKER_QUEUE` (50), `AI_WORKER_TIMEOUT` (`PT25S`) |
| `InsightsRefreshWorker` | Scheduled job that, while at least one browser is connected over WebSocket, recomputes the trending statistics (reusing the cached AI summary, so it does not spend AI quota) and pushes them when they change. | `INSIGHTS_REFRESH_ENABLED` (true), `INSIGHTS_REFRESH_INTERVAL` (`PT20S`) |

#### WebSocket

`/ws/kino` (for example `wss://kino-backend-alq2.onrender.com/ws/kino`) is a plain WebSocket that accepts the same origins as CORS. Every message is JSON `{ "type", "requestId"?, "data" }`:

| Direction | `type` | Payload |
|---|---|---|
| server → client | `welcome` | `{ features, timestamp }` right after connecting |
| server → client | `insights` | the same body as `GET /api/ai/trending-insights`, on connect and whenever the statistics change |
| client → server | `ping` | answered with `pong` (the frontend sends one every 25 s so proxies keep the socket open) |
| client → server | `insights` | asks for the current insights |
| client → server | `chat` | `{ "type": "chat", "requestId": "abc", "messages": [...] }`, answered with `chat-reply` (`data` is the chat response) or `chat-error` (`data.message`) with the same `requestId` |

`GET /api/health` also reports `realtime: { path, connections }` and `workers: { ai: { threads, active, queued, completed }, insightsRefresh: { enabled, lastRun, broadcasts } }`.

### gRPC API

Besides REST, the backend exposes the same data over gRPC (contract in `src/main/proto/kino/v1/kino.proto`, Java code generated at build time):

- `kino.v1.CatalogService`: `ListTrendingVideos` (optional `category`, `limit` 1-50) and `GetVideo`.
- `kino.v1.InsightsService`: `GetTrendingInsights` (the same Kino IA analysis as `/api/ai/trending-insights`).
- `kino.v1.LiveService`: `ListLiveStreams` and `WatchLiveStreams`, a server-streaming call that pushes a live snapshot every `interval_seconds` (1-60) up to `max_updates`.

Patterns applied: versioned package (`kino.v1`), unary and server-streaming calls, errors mapped to gRPC status codes (`INVALID_ARGUMENT`, `NOT_FOUND`, `FAILED_PRECONDITION`…), a logging interceptor that records method, status and latency, the standard `grpc.health.v1` health service, server reflection (works with `grpcurl` and Postman), message size limits, keepalive and graceful shutdown tied to the Spring lifecycle.

The gRPC server is disabled by default (`GRPC_ENABLED=false`) because Render's free web service exposes only the HTTP port; enable it locally or on a host that exposes `GRPC_PORT` (default `9090`):

```powershell
grpcurl -plaintext localhost:9090 list
grpcurl -plaintext -d '{"limit":5}' localhost:9090 kino.v1.CatalogService/ListTrendingVideos
grpcurl -plaintext -d '{"interval_seconds":5,"max_updates":3}' localhost:9090 kino.v1.LiveService/WatchLiveStreams
```

The generated Java classes are committed in `src/main/java/com/kino/backend/grpc/v1`, so Maven, Docker and IDEs build without running `protoc`. After changing `kino.proto`, regenerate them with `mvn -Pgrpc-codegen generate-sources`. `protoc` cannot read Windows paths with non-ASCII characters (for example `Programación`); if that command fails with `PROTOC FAILED ... directory does not exist`, run it from a drive letter mapped with `subst K: "C:\path\Kino_Streaming"`.

### Demo content

`src/main/resources/db/supabase/seed-demo-content.sql` loads 16 videos from 5 creators across every category, with view counts, publication dates (some in the last 7 days, so they appear as rising) and viewer interactions, so the dashboard, the recommendations and Kino IA have data to analyze. Videos are public sample clips (Blender open movies, MDN, test-videos.co.uk, samplelib) and thumbnails come from picsum.photos. Run it in the Supabase SQL Editor; it is idempotent. Demo accounts use `@demo.kino.app` emails and an unusable password hash, so nobody can sign in with them. `remove-demo-content.sql` deletes everything it created.

## Error format

Every error is returned as JSON with the same shape, including authentication (`401`) and authorization (`403`) failures:

```json
{
  "timestamp": "2026-11-12T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Revisa los campos enviados",
  "validationErrors": { "videoUrl": "Debe ser una URL HTTP o HTTPS válida" }
}
```

Validation messages are always in Spanish (they do not depend on the server locale), so the frontend can show `validationErrors` next to each field.

Malformed JSON bodies and invalid path parameters (for example a non-UUID video id) return `400`, unknown routes return `404`, unsupported HTTP methods return `405`, and unexpected failures return a generic `500` message while the details are logged on the server.

Every account can publish videos and go live: new registrations get `CREATOR`, and `/api/creator/**` only requires a signed-in account (older `VIEWER` accounts work too; `2026-11-13-everyone-creator.sql` promotes them). Ownership is checked per video and per broadcast. Video storage is represented by `videoUrl` and `thumbnailUrl`; the binary upload to S3 or another object store and payment processing are not implemented. The frontend player streams directly from the supplied URL, which must be publicly reachable and supported by the browser.

The recommendation endpoint uses a lightweight hybrid ranker: recorded viewer/video pairs contribute collaborative scores, while persisted playback counts contribute popularity scores. It runs locally without an external AI provider or trained machine-learning model; it is an integration-ready baseline, not a production recommendation service. Playback counts are incremented per play request, while each account/video pair is stored once for collaborative filtering.

Before setting `DDL_AUTO=validate` on an existing database, apply `src/main/resources/db/manual/2026-11-11-video-streaming-metadata.sql`. Never use the development JWT key or database password outside local development.