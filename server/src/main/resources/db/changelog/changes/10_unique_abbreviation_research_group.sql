--liquibase formatted sql
--changeset ramona:10-unique-abbreviation-research-group-1

-- Step 1: Fill in missing abbreviations (example logic using name initial + ID suffix)
UPDATE research_groups
SET abbreviation = CONCAT(LEFT(name, 5), '-', SUBSTRING(research_group_id::text, 1, 4))
WHERE abbreviation IS NULL OR abbreviation = '';

--changeset ramona:10-unique-abbreviation-research-group-2
-- Step 2: Deduplicate existing abbreviations (example: add suffix to duplicates)
WITH duplicates AS (
    SELECT abbreviation
    FROM research_groups
    GROUP BY abbreviation
    HAVING COUNT(*) > 1
),
     to_fix AS (
         SELECT research_group_id, abbreviation,
                ROW_NUMBER() OVER (PARTITION BY abbreviation ORDER BY research_group_id) AS rn
         FROM research_groups
         WHERE abbreviation IN (SELECT abbreviation FROM duplicates)
     )
UPDATE research_groups rg
SET abbreviation = CONCAT(rg.abbreviation, '-', tf.rn)
FROM to_fix tf
WHERE rg.research_group_id = tf.research_group_id AND tf.rn > 1;

--changeset ramona:10-unique-abbreviation-research-group-3
-- Step 3: Set NOT NULL constraint
ALTER TABLE research_groups
    ALTER COLUMN abbreviation SET NOT NULL;

--changeset ramona:10-unique-abbreviation-research-group-4
-- Step 4: Enforce NOT EMPTY via CHECK constraint
ALTER TABLE research_groups
    ADD CONSTRAINT chk_abbreviation_not_empty CHECK (abbreviation <> '');

--changeset ramona:10-unique-abbreviation-research-group-5
-- Step 5: Enforce uniqueness
ALTER TABLE research_groups
    ADD CONSTRAINT uq_research_groups_abbreviation UNIQUE (abbreviation);