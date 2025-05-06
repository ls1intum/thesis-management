-- ============================================================================
-- Manual insert of the fictional "Institute for Adaptive Algorithmic Systems (IAAS)"
-- research group for demonstration and testing purposes only.
--
-- This group does not exist at TUM and is used exclusively in non-production contexts.
--
-- Author: Marc Fett
-- Date: 2025/05/06
-- ============================================================================

WITH head_user_id_value
         AS (SELECT 'fa734cf3-d1d9-4b0e-8c3a-cf105195bdfa'::UUID AS head_user_id -- Replace with existing user UUID
    )
INSERT
INTO research_groups (
    research_group_id,
    head_user_id,
    name,
    abbreviation,
    description,
    website_url,
    campus,
    created_at,
    created_by,
    updated_at,
    updated_by,
    archived)
SELECT gen_random_uuid(),
       head_user_id,
       'Institute for Adaptive Algorithmic Systems',
       'IAAS',
       'The IAAS group conducts interdisciplinary research on adaptive algorithms, self-optimizing systems, and dynamic software infrastructures. Our work bridges theoretical computer science and applied engineering to enable resilient digital platforms.',
       'https://iaas-labs.org/',
       'Munich - Main Campus',
       NOW(),
       head_user_id,
       NOW(),
       head_user_id,
       FALSE
FROM head_user_id_value;