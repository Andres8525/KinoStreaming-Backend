-- Kino Streaming - migration: every account can publish videos and go live.
-- New registrations are created as CREATOR; this promotes the accounts created before the change.
-- Idempotent: safe to run again.
UPDATE public.users SET role = 'CREATOR' WHERE role = 'VIEWER';
