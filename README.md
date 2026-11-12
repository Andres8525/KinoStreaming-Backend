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

The API listens on `http://localhost:8080`. CORS allows the Angular dev server at `http://localhost:4200` for GET, POST, PUT, DELETE and OPTIONS, including the `Authorization` header. Set `CORS_ALLOWED_ORIGINS` to a comma-separated list to allow other origins.

`.env.example` lists every supported environment variable. `JWT_SECRET` must be at least 32 bytes long; the application refuses to start otherwise.

Run the test suite (uses an in-memory H2 database, no PostgreSQL needed):

```powershell
mvn test
```

### Docker

Build and run the API as a container:

```powershell
docker build -t kino-backend .
docker run --rm -p 8080:8080 `
  -e DB_URL="jdbc:postgresql://host.docker.internal:5432/kino" `
  -e DB_USERNAME="kino" -e DB_PASSWORD="kino_dev_password" `
  -e JWT_SECRET="replace-with-a-random-secret-at-least-32-bytes-long" `
  kino-backend
```

## Endpoints

- `POST /api/auth/register` creates a viewer and returns a bearer token.
- `POST /api/auth/login` authenticates and returns a bearer token.
- `GET /api/videos` returns the public video catalog.
- `GET /api/videos/trending` returns up to 20 videos ordered by recorded playback count.
- `GET /api/videos/{id}` returns the video details and streaming URL.
- `POST /api/videos/{id}/views` requires a bearer token, increments the view count, and records the viewer/video pair once for recommendations.
- `GET /api/feed/recommendations` requires a bearer token and returns `{ strategy, generatedAt, recommendations }`, ranked with collaboration and popularity signals.
- `GET /api/videos/search?q=` returns up to 50 videos whose title contains the query (case-insensitive). A blank query returns an empty list.
- `GET /api/users/me` requires a bearer token and returns the authenticated user's `id`, `displayName`, `email` and `role`.
- `GET /api/creator/videos`, `GET /api/creator/videos/analytics`, and `POST /api/creator/videos` require a bearer token with `CREATOR` role.
- `DELETE /api/creator/videos/{id}` requires a bearer token with `CREATOR` role, deletes a video owned by the caller together with its recorded interactions, and returns `204`. Videos owned by another creator return `404`.

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

Malformed JSON bodies and invalid path parameters (for example a non-UUID video id) return `400`, unknown routes return `404`, unsupported HTTP methods return `405`, and unexpected failures return a generic `500` message while the details are logged on the server.

New public registrations are always assigned `VIEWER`; grant `CREATOR` through a trusted administrative process. Video storage is represented by `videoUrl` and `thumbnailUrl`; the binary upload to S3 or another object store and payment processing are not implemented. The frontend player streams directly from the supplied URL, which must be publicly reachable and supported by the browser.

To promote an account after registration, use a trusted database administrator connection, not a public API request:

```sql
UPDATE users SET role = 'CREATOR' WHERE email = 'creator@example.com';
```

The recommendation endpoint uses a lightweight hybrid ranker: recorded viewer/video pairs contribute collaborative scores, while persisted playback counts contribute popularity scores. It runs locally without an external AI provider or trained machine-learning model; it is an integration-ready baseline, not a production recommendation service. Playback counts are incremented per play request, while each account/video pair is stored once for collaborative filtering.

Before setting `DDL_AUTO=validate` on an existing database, apply `src/main/resources/db/manual/2026-11-11-video-streaming-metadata.sql`. Never use the development JWT key or database password outside local development.