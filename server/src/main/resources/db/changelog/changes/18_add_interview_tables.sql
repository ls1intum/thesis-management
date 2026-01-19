--liquibase formatted sql
--changeset ramona:18_add_interview_tables

CREATE TABLE IF NOT EXISTS interview_processes (
    interview_process_id UUID PRIMARY KEY,
    topic_id UUID NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT fk_interview_process_topic FOREIGN KEY (topic_id) REFERENCES topics(topic_id) on DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS interviewees (
    interviewee_id UUID PRIMARY KEY,
    interview_process_id UUID NOT NULL,
    last_invited TIMESTAMP,
    score INTEGER,
    application_id UUID NOT NULL,
    CONSTRAINT fk_interviewee_process FOREIGN KEY (interview_process_id) REFERENCES interview_processes(interview_process_id) ON DELETE CASCADE,
    CONSTRAINT fk_interviewee_application FOREIGN KEY (application_id) REFERENCES applications(application_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS interview_slots (
    slot_id UUID PRIMARY KEY,
    interview_process_id UUID NOT NULL,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    interviewee_id UUID,
    location TEXT,
    stream_link TEXT,
    CONSTRAINT fk_slot_process FOREIGN KEY (interview_process_id) REFERENCES interview_processes(interview_process_id) ON DELETE CASCADE,
    CONSTRAINT fk_slot_interviewee FOREIGN KEY (interviewee_id) REFERENCES interviewees(interviewee_id) ON DELETE CASCADE 
);

CREATE TABLE IF NOT EXISTS interview_assessments (
    interview_assessment_id UUID PRIMARY KEY,
    interviewee_id UUID NOT NULL,
    interview_note TEXT,
    CONSTRAINT fk_assessment_interviewee FOREIGN KEY (interviewee_id) REFERENCES interviewees(interviewee_id) ON DELETE CASCADE
);