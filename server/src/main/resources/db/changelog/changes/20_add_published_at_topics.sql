--liquibase formatted sql
--changeset ramona:20_add_published_at_topics

ALTER TABLE topics
ADD COLUMN published_at TIMESTAMP WITH TIME ZONE NULL;

UPDATE topics
SET published_at = created_at
WHERE published_at IS NULL;
