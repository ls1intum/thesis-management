--liquibase formatted sql
--changeset ramona:15_add_presentation_time_to_settings

ALTER TABLE research_group_settings
ADD COLUMN presentation_slot_duration INTEGER NULL;