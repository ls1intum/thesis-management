--liquibase formatted sql

--changeset thesis:30-user-deletion-1
ALTER TABLE users ADD COLUMN anonymized_at TIMESTAMP;
ALTER TABLE users ADD COLUMN deletion_requested_at TIMESTAMP;
ALTER TABLE users ADD COLUMN deletion_scheduled_for TIMESTAMP;

--changeset thesis:30-user-deletion-2
-- ON DELETE CASCADE for user-owned metadata (no retention needed)
ALTER TABLE notification_settings DROP CONSTRAINT IF EXISTS notification_settings_user_id_fkey;
ALTER TABLE notification_settings ADD CONSTRAINT notification_settings_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE;

ALTER TABLE user_groups DROP CONSTRAINT IF EXISTS user_groups_user_id_fkey;
ALTER TABLE user_groups ADD CONSTRAINT user_groups_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE;

ALTER TABLE data_exports DROP CONSTRAINT IF EXISTS data_exports_user_id_fkey;
ALTER TABLE data_exports ADD CONSTRAINT data_exports_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE;

--changeset thesis:30-user-deletion-3
-- ON DELETE SET NULL for audit references on retained records
-- thesis_assessments.created_by
ALTER TABLE thesis_assessments ALTER COLUMN created_by DROP NOT NULL;
ALTER TABLE thesis_assessments DROP CONSTRAINT IF EXISTS thesis_assessments_created_by_fkey;
ALTER TABLE thesis_assessments ADD CONSTRAINT thesis_assessments_created_by_fkey
    FOREIGN KEY (created_by) REFERENCES users (user_id) ON DELETE SET NULL;

-- thesis_comments.created_by
ALTER TABLE thesis_comments ALTER COLUMN created_by DROP NOT NULL;
ALTER TABLE thesis_comments DROP CONSTRAINT IF EXISTS thesis_comments_created_by_fkey;
ALTER TABLE thesis_comments ADD CONSTRAINT thesis_comments_created_by_fkey
    FOREIGN KEY (created_by) REFERENCES users (user_id) ON DELETE SET NULL;

-- thesis_feedback.requested_by
ALTER TABLE thesis_feedback ALTER COLUMN requested_by DROP NOT NULL;
ALTER TABLE thesis_feedback DROP CONSTRAINT IF EXISTS thesis_feedback_requested_by_fkey;
ALTER TABLE thesis_feedback ADD CONSTRAINT thesis_feedback_requested_by_fkey
    FOREIGN KEY (requested_by) REFERENCES users (user_id) ON DELETE SET NULL;

-- thesis_files.uploaded_by
ALTER TABLE thesis_files ALTER COLUMN uploaded_by DROP NOT NULL;
ALTER TABLE thesis_files DROP CONSTRAINT IF EXISTS thesis_files_uploaded_by_fkey;
ALTER TABLE thesis_files ADD CONSTRAINT thesis_files_uploaded_by_fkey
    FOREIGN KEY (uploaded_by) REFERENCES users (user_id) ON DELETE SET NULL;

-- thesis_proposals.created_by
ALTER TABLE thesis_proposals ALTER COLUMN created_by DROP NOT NULL;
ALTER TABLE thesis_proposals DROP CONSTRAINT IF EXISTS thesis_proposals_created_by_fkey;
ALTER TABLE thesis_proposals ADD CONSTRAINT thesis_proposals_created_by_fkey
    FOREIGN KEY (created_by) REFERENCES users (user_id) ON DELETE SET NULL;

-- topics.created_by
ALTER TABLE topics ALTER COLUMN created_by DROP NOT NULL;
ALTER TABLE topics DROP CONSTRAINT IF EXISTS topics_created_by_fkey;
ALTER TABLE topics ADD CONSTRAINT topics_created_by_fkey
    FOREIGN KEY (created_by) REFERENCES users (user_id) ON DELETE SET NULL;

-- email_templates.updated_by (already nullable from 09_email_templates.sql)
ALTER TABLE email_templates DROP CONSTRAINT IF EXISTS email_templates_updated_by_fkey;
ALTER TABLE email_templates ADD CONSTRAINT email_templates_updated_by_fkey
    FOREIGN KEY (updated_by) REFERENCES users (user_id) ON DELETE SET NULL;

-- research_groups.created_by (already nullable from 08_research_groups.sql)
ALTER TABLE research_groups DROP CONSTRAINT IF EXISTS research_groups_created_by_fkey;
ALTER TABLE research_groups ADD CONSTRAINT research_groups_created_by_fkey
    FOREIGN KEY (created_by) REFERENCES users (user_id) ON DELETE SET NULL;

-- research_groups.updated_by (already nullable from 08_research_groups.sql)
ALTER TABLE research_groups DROP CONSTRAINT IF EXISTS research_groups_updated_by_fkey;
ALTER TABLE research_groups ADD CONSTRAINT research_groups_updated_by_fkey
    FOREIGN KEY (updated_by) REFERENCES users (user_id) ON DELETE SET NULL;

-- topic_roles.assigned_by
ALTER TABLE topic_roles ALTER COLUMN assigned_by DROP NOT NULL;
ALTER TABLE topic_roles DROP CONSTRAINT IF EXISTS topic_roles_assigned_by_fkey;
ALTER TABLE topic_roles ADD CONSTRAINT topic_roles_assigned_by_fkey
    FOREIGN KEY (assigned_by) REFERENCES users (user_id) ON DELETE SET NULL;

-- thesis_roles.assigned_by
ALTER TABLE thesis_roles ALTER COLUMN assigned_by DROP NOT NULL;
ALTER TABLE thesis_roles DROP CONSTRAINT IF EXISTS thesis_roles_assigned_by_fkey;
ALTER TABLE thesis_roles ADD CONSTRAINT thesis_roles_assigned_by_fkey
    FOREIGN KEY (assigned_by) REFERENCES users (user_id) ON DELETE SET NULL;
