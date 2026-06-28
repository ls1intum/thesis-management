package de.tum.cit.aet.thesis.feedback.dto;

import java.util.List;

public record FindingDTO(String severity, String category, String title, String description, List<Location> locations) {
}
