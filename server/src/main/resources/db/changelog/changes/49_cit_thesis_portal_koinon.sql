--liquibase formatted sql

--changeset thesis:49-cit-thesis-portal-koinon
-- The CIT portal moved from portal.cit.tum.de to koinon.tum.de (the old host now only shows a
-- migration notice). Only rewrite rows that still hold the old default so admin edits are kept.
UPDATE schools
SET thesis_portal_url = 'https://koinon.tum.de/',
    updated_at        = now()
WHERE thesis_portal_url IN ('https://portal.cit.tum.de/', 'https://portal.cit.tum.de');
