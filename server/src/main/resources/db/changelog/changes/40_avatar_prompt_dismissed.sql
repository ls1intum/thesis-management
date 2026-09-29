-- liquibase formatted sql

-- changeset thesis-management:40-add-avatar-prompt-dismissed-at
-- comment: Records when a user dismissed the "add a profile picture" dialog so they are only asked once.
--          Intentionally not backfilled: existing users without a picture are asked once after the release.
ALTER TABLE users ADD COLUMN avatar_prompt_dismissed_at TIMESTAMP WITH TIME ZONE;
