package de.tum.cit.aet.thesis.feedback.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A feedback entry raised on an earlier review of the same document type, as a reviewer sees it
 * when comparing the new version against what was already said.
 *
 * <p>The model never sees the database id. It addresses an entry by its short {@code ref}
 * ({@code E1}, {@code E2}, ...), which is far less likely to be garbled in structured output than a
 * UUID, and the application maps the ref back to the id. A ref the model invents therefore maps to
 * nothing and is dropped instead of attaching a follow-up to the wrong entry.
 *
 * @param ref           the short handle the model uses for this entry within one review
 * @param id            the {@code thesis_feedback} id
 * @param text          the entry's feedback text as the student sees it
 * @param category      the entry's category token (FORMATTING, CITATION, ...), or {@code null}
 * @param severity      the entry's severity token (CRITICAL, MAJOR, ...), or {@code null}
 * @param completedAt   when the student marked the entry as resolved; {@code null} while open
 * @param followUpDepth how many follow-ups precede this entry in its chain; 0 for an original
 * @param deferred      whether this entry already defers the defect to the supervisor
 */
public record EarlierFeedback(
		String ref,
		UUID id,
		String text,
		String category,
		String severity,
		Instant completedAt,
		int followUpDepth,
		boolean deferred
) {
	/** Rejects an entry the comparison could not address or describe. */
	public EarlierFeedback {
		Objects.requireNonNull(ref, "ref");
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(text, "text");
	}

	/**
	 * Whether the student marked this entry as resolved.
	 *
	 * @return {@code true} once the entry is completed
	 */
	public boolean completed() {
		return completedAt != null;
	}
}
