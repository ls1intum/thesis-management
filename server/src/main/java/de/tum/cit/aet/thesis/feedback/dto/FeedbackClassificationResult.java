package de.tum.cit.aet.thesis.feedback.dto;

/**
 * Raw structured output of the classification LLM call. The fields stay {@link String} rather than
 * the domain enums for the same reason {@link FindingDTO} does: a model that answers with an
 * unexpected token must not fail response parsing outright — it is mapped leniently one layer up.
 */
public record FeedbackClassificationResult(String category, String severity) {
}
