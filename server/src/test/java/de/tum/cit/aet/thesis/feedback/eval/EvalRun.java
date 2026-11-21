package de.tum.cit.aet.thesis.feedback.eval;

/**
 * One cell of the sweep: a single document reviewed once, by one strategy, on one model, at one
 * seed. Identifies both the output path and the row in {@code runs.jsonl}.
 *
 * @param model        the chat model the review ran on
 * @param approach     the {@link EvalApproach} id of the prompting technique
 * @param documentId   the corpus's anonymous document id
 * @param documentType {@code PROPOSAL} or {@code THESIS}
 * @param seed         the sampling seed this repetition used
 */
public record EvalRun(String model, String approach, String documentId, String documentType, int seed) {
}
