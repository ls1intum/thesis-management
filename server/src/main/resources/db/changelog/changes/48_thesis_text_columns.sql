--liquibase formatted sql

--changeset thesis:48-thesis-text-columns
-- Older installations still have VARCHAR(2000) columns. Match the TEXT columns
-- in the initial schema so accepting a longer extracted abstract cannot fail.
-- This also safely applies when either column has already been widened.
ALTER TABLE theses
    ALTER COLUMN abstract TYPE TEXT,
    ALTER COLUMN info TYPE TEXT;
