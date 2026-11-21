package de.tum.cit.aet.thesis.core.application.cron.model;

import java.time.Instant;
import java.util.UUID;

public record ApplicationRejectObject(String name, String topicTitle, Instant rejectionDate, UUID applicationId) {
}
