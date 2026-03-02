--liquibase formatted sql

--changeset thesis-management:29-add-user-disabled-flag
ALTER TABLE users ADD COLUMN disabled BOOLEAN NOT NULL DEFAULT FALSE;
