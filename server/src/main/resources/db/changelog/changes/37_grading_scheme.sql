--liquibase formatted sql

--changeset thesis:37_grading_scheme

-- Default grading scheme per research group
CREATE TABLE grading_scheme_components (
    component_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    research_group_id UUID NOT NULL REFERENCES research_groups(research_group_id) ON DELETE CASCADE,
    name              VARCHAR(255) NOT NULL,
    weight            NUMERIC(5,2) NOT NULL,
    is_bonus          BOOLEAN NOT NULL DEFAULT FALSE,
    position          INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_grading_scheme_components_research_group ON grading_scheme_components(research_group_id);

-- Per-assessment grade breakdown (snapshot at assessment time)
CREATE TABLE thesis_grade_components (
    grade_component_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessment_id      UUID NOT NULL REFERENCES thesis_assessments(assessment_id) ON DELETE CASCADE,
    name               VARCHAR(255) NOT NULL,
    weight             NUMERIC(5,2) NOT NULL,
    is_bonus           BOOLEAN NOT NULL DEFAULT FALSE,
    grade              NUMERIC(3,1) NOT NULL,
    position           INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_thesis_grade_components_assessment ON thesis_grade_components(assessment_id);
