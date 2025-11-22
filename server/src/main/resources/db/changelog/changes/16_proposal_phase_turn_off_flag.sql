--liquibase formatted sql
--changeset ramona:16_add_presentation_time_to_settings

ALTER TABLE research_group_settings
ADD COLUMN proposal_phase_active BOOLEAN DEFAULT TRUE;