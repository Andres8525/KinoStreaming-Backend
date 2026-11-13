-- Kino Streaming - migration: live broadcasts ("En vivo").
-- Run once in Supabase (SQL Editor) BEFORE deploying the backend version that includes LiveStream,
-- because the backend starts with DDL_AUTO=validate. Idempotent: safe to run again.

CREATE TABLE IF NOT EXISTS public.live_streams (
    id            UUID                        NOT NULL DEFAULT gen_random_uuid(),
    creator_id    UUID                        NOT NULL,
    title         VARCHAR(180)                NOT NULL,
    description   VARCHAR(1000),
    category      VARCHAR(60)                 NOT NULL,
    playback_url  VARCHAR(2048)               NOT NULL,
    thumbnail_url VARCHAR(2048),
    status        VARCHAR(10)                 NOT NULL DEFAULT 'LIVE',
    started_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    ended_at      TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT live_streams_pkey PRIMARY KEY (id),
    CONSTRAINT live_streams_creator_fk FOREIGN KEY (creator_id) REFERENCES public.users (id) ON DELETE CASCADE,
    CONSTRAINT live_streams_status_check CHECK (status IN ('LIVE', 'ENDED')),
    CONSTRAINT live_streams_ended_check CHECK ((status = 'LIVE' AND ended_at IS NULL) OR (status = 'ENDED' AND ended_at IS NOT NULL))
);

CREATE INDEX IF NOT EXISTS idx_live_streams_status_started_at ON public.live_streams (status, started_at DESC);
CREATE INDEX IF NOT EXISTS idx_live_streams_creator_id ON public.live_streams (creator_id);
-- A creator can only have one live broadcast at a time.
CREATE UNIQUE INDEX IF NOT EXISTS uq_live_streams_one_live_per_creator
    ON public.live_streams (creator_id) WHERE status = 'LIVE';

-- Block the public Supabase Data API, like the other Kino tables.
ALTER TABLE public.live_streams ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.live_streams FROM anon, authenticated;
