ALTER TABLE videos
    ADD COLUMN IF NOT EXISTS thumbnail_url VARCHAR(2048);

ALTER TABLE videos
    ADD COLUMN IF NOT EXISTS view_count BIGINT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_videos_views_created_at
    ON videos (view_count DESC, created_at DESC);
