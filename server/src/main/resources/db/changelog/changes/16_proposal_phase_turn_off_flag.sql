--liquibase formatted sql
--changeset ramona:16_proposal_phase_turn_off_flag

ALTER TABLE research_group_settings
ADD COLUMN proposal_phase_active BOOLEAN NOT NULL DEFAULT true;