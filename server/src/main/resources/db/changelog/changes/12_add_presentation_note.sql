--liquibase formatted sql
--changeset ramona:12-add-presentation-note

ALTER TABLE thesis_presentations
ADD COLUMN presentation_note_html TEXT NULL;