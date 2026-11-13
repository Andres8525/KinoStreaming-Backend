-- Kino Streaming - migration: live broadcasts from the browser camera (WebRTC).
-- Run once in Supabase (SQL Editor) BEFORE deploying the backend version that includes
-- LiveStream.sourceType, because the backend starts with DDL_AUTO=validate. Idempotent.

ALTER TABLE public.live_streams ADD COLUMN IF NOT EXISTS source_type VARCHAR(10) NOT NULL DEFAULT 'HLS';
-- Camera broadcasts have no playback URL: the video goes from the creator's browser to each viewer.
ALTER TABLE public.live_streams ALTER COLUMN playback_url DROP NOT NULL;

ALTER TABLE public.live_streams DROP CONSTRAINT IF EXISTS live_streams_source_type_check;
ALTER TABLE public.live_streams ADD CONSTRAINT live_streams_source_type_check
    CHECK (source_type IN ('CAMERA', 'HLS'));
ALTER TABLE public.live_streams DROP CONSTRAINT IF EXISTS live_streams_hls_url_check;
ALTER TABLE public.live_streams ADD CONSTRAINT live_streams_hls_url_check
    CHECK (source_type <> 'HLS' OR playback_url IS NOT NULL);
