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

The API listens on `http://localhost:8081` (port 8080 is often taken by other local tools such as Jenkins; override it with `PORT`). CORS allows the Angular dev server at `http://localhost:4200` for GET, POST, PUT, DELETE and OPTIONS, including the `Authorization` header. Set `CORS_ALLOWED_ORIGINS` to a comma-separated list to allow other origins, for example `https://kino-streamingg-frontend.vercel.app,https://kino-streamingg-frontend-*.vercel.app,http://localhost:4200`. Entries containing `*` are patterns (useful for Vercel preview deployments); spaces, quotes, `;` separators and trailing slashes are tolerated. For every `https://<project>.vercel.app` entry the backend also allows `https://<project>-*.vercel.app` (the per-deployment URL opened by Vercel's "Visit" button and the git branch URLs; disable with `CORS_ALLOW_VERCEL_PREVIEWS=false`), and it always allows `http://localhost:<any port>` / `http://127.0.0.1:<any port>` so a local Angular dev server can use the deployed API (disable with `CORS_ALLOW_LOCALHOST=false`). The API uses bearer tokens, not cookies, so these extra origins cannot reuse a signed-in session. The backend logs the effective origins at startup (`CORS allowed origins: ...`), which is the first thing to check if the browser reports a CORS error.

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

- **Keep-alive**: `.github/workflows/keep-alive.yml` calls `/api/health` every 10 minutes from GitHub Actions, so the service never sleeps and the Supabase connection stays warm (one always-on service fits in the 750 free instance hours per month). Enable Actions in the repository; set the `KINO_HEALTH_URL` repository variable if the service URL changes. GitHub may delay scheduled runs by a few minutes and disables them after 60 days without commits, so re-enable the workflow from the Actions tab if needed.
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

**Google Gemini is built in** (`GeminiTextGenerator`). To turn it on:

1. Create an API key in [Google AI Studio](https://aistudio.google.com/apikey).
2. In the Render dashboard (or `.env` locally) set `AI_ENABLED=true`, `AI_PROVIDER=gemini` and `AI_API_KEY=<your key>`. Optionally `AI_MODEL` (default `gemini-2.5-flash`) and `AI_BASE_URL` (default `https://generativelanguage.googleapis.com/v1beta`). Never commit the key: only the backend uses it, the frontend never sees it.
3. Save; Render redeploys. `GET /api/ai/trending-insights` then returns `source: "ai"` (results are cached for `AI_CACHE_TTL`).

If Gemini fails, times out (20 s) or returns no text, Kino logs a warning and falls back to its own summary, so the dashboard keeps working. Another provider can be added with a Spring `@Component` implementing `com.kino.backend.ai.AiTextGenerator`.

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