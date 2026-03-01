--liquibase formatted sql

--changeset krusche:25-drop-calendar-event-column
ALTER TABLE thesis_presentations DROP COLUMN IF EXISTS calendar_event;
