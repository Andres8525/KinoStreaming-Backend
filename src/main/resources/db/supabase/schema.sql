-- Kino Streaming - database schema for Supabase (PostgreSQL 15+)
--
-- Run this whole script once in Supabase: Dashboard > SQL Editor > New query > Run.
-- It is idempotent: running it again does not drop or duplicate anything.
--
-- The tables match the JPA entities in com.kino.backend.model, so the backend can
-- start with DDL_AUTO=validate.
--
-- Security: Supabase publishes every table in the "public" schema through its Data API
-- (PostgREST) using the anon key. Row Level Security is enabled below WITHOUT policies,
-- so the Data API cannot read or write these tables (for example password hashes).
-- The Spring Boot backend connects as the "postgres" role, which owns the tables and
-- bypasses RLS, so the API keeps working normally.

-- ---------------------------------------------------------------------------
-- users
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.users (
    id            UUID                        NOT NULL DEFAULT gen_random_uuid(),
    display_name  VARCHAR(120)                NOT NULL,
    email         VARCHAR(254)                NOT NULL,
    password_hash VARCHAR(255)                NOT NULL,
    role          VARCHAR(20)                 NOT NULL DEFAULT 'VIEWER',
    created_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT users_pkey PRIMARY KEY (id),
    CONSTRAINT users_email_key UNIQUE (email),
    CONSTRAINT users_role_check CHECK (role IN ('CREATOR', 'VIEWER'))
);

-- ---------------------------------------------------------------------------
-- videos
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.videos (
    id                 UUID                        NOT NULL DEFAULT gen_random_uuid(),
    title              VARCHAR(180)                NOT NULL,
    description        VARCHAR(5000)               NOT NULL,
    video_url          VARCHAR(2048)               NOT NULL,
    thumbnail_url      VARCHAR(2048),
    view_count         BIGINT                      NOT NULL DEFAULT 0,
    access_price       NUMERIC(10, 2)              NOT NULL DEFAULT 0,
    suggested_donation NUMERIC(10, 2),
    creator_id         UUID                        NOT NULL,
    created_at         TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT videos_pkey PRIMARY KEY (id),
    CONSTRAINT videos_creator_fk FOREIGN KEY (creator_id) REFERENCES public.users (id) ON DELETE CASCADE,
    CONSTRAINT videos_access_price_check CHECK (access_price >= 0),
    CONSTRAINT videos_suggested_donation_check CHECK (suggested_donation IS NULL OR suggested_donation >= 0),
    CONSTRAINT videos_view_count_check CHECK (view_count >= 0)
);

CREATE INDEX IF NOT EXISTS idx_videos_views_created_at ON public.videos (view_count DESC, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_videos_creator_id ON public.videos (creator_id);
CREATE INDEX IF NOT EXISTS idx_videos_created_at ON public.videos (created_at DESC);

-- ---------------------------------------------------------------------------
-- video_tags (JPA @ElementCollection of Video.tags)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.video_tags (
    video_id UUID        NOT NULL,
    tag      VARCHAR(40) NOT NULL,
    CONSTRAINT video_tags_video_fk FOREIGN KEY (video_id) REFERENCES public.videos (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_video_tags_video_id ON public.video_tags (video_id);

-- ---------------------------------------------------------------------------
-- interactions (viewer/video history used by the recommendation feed)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.interactions (
    id              UUID                        NOT NULL DEFAULT gen_random_uuid(),
    user_id         UUID                        NOT NULL,
    video_id        UUID                        NOT NULL,
    rating          INTEGER,
    watched_seconds INTEGER                     NOT NULL DEFAULT 0,
    watched_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT interactions_pkey PRIMARY KEY (id),
    CONSTRAINT interactions_user_fk FOREIGN KEY (user_id) REFERENCES public.users (id) ON DELETE CASCADE,
    CONSTRAINT interactions_video_fk FOREIGN KEY (video_id) REFERENCES public.videos (id) ON DELETE CASCADE,
    CONSTRAINT interactions_rating_check CHECK (rating IS NULL OR rating BETWEEN 1 AND 5),
    CONSTRAINT interactions_watched_seconds_check CHECK (watched_seconds >= 0)
);

CREATE INDEX IF NOT EXISTS idx_interactions_user_id ON public.interactions (user_id);
CREATE INDEX IF NOT EXISTS idx_interactions_video_id ON public.interactions (video_id);

-- ---------------------------------------------------------------------------
-- live_streams (live broadcasts)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.live_streams (
    id            UUID                        NOT NULL DEFAULT gen_random_uuid(),
    creator_id    UUID                        NOT NULL,
    title         VARCHAR(180)                NOT NULL,
    description   VARCHAR(1000),
    category      VARCHAR(60)                 NOT NULL,
    source_type   VARCHAR(10)                 NOT NULL DEFAULT 'HLS',
    playback_url  VARCHAR(2048),
    thumbnail_url VARCHAR(2048),
    status        VARCHAR(10)                 NOT NULL DEFAULT 'LIVE',
    started_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    ended_at      TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT live_streams_pkey PRIMARY KEY (id),
    CONSTRAINT live_streams_creator_fk FOREIGN KEY (creator_id) REFERENCES public.users (id) ON DELETE CASCADE,
    CONSTRAINT live_streams_status_check CHECK (status IN ('LIVE', 'ENDED')),
    CONSTRAINT live_streams_source_type_check CHECK (source_type IN ('CAMERA', 'HLS')),
    CONSTRAINT live_streams_hls_url_check CHECK (source_type <> 'HLS' OR playback_url IS NOT NULL),
    CONSTRAINT live_streams_ended_check CHECK ((status = 'LIVE' AND ended_at IS NULL) OR (status = 'ENDED' AND ended_at IS NOT NULL))
);

CREATE INDEX IF NOT EXISTS idx_live_streams_status_started_at ON public.live_streams (status, started_at DESC);
CREATE INDEX IF NOT EXISTS idx_live_streams_creator_id ON public.live_streams (creator_id);
-- A creator can only have one live broadcast at a time.
CREATE UNIQUE INDEX IF NOT EXISTS uq_live_streams_one_live_per_creator
    ON public.live_streams (creator_id) WHERE status = 'LIVE';

-- ---------------------------------------------------------------------------
-- Block the public Supabase Data API (anon / authenticated keys).
-- ---------------------------------------------------------------------------
ALTER TABLE public.users        ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.videos       ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.video_tags   ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.interactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.live_streams ENABLE ROW LEVEL SECURITY;

REVOKE ALL ON public.users, public.videos, public.video_tags, public.interactions, public.live_streams FROM anon, authenticated;
