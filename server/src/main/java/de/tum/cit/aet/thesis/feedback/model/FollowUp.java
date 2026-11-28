package de.tum.cit.aet.thesis.feedback.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

/**
 * A defect from an earlier, completed feedback entry that recurs in the new version. The finding
 * restates the defect more explicitly than the original entry did; whether it is persisted as a
 * follow-up or as a deferral to the supervisor is decided by the application, which knows how
 * often the defect has recurred already.
 *
 * @param ref     the {@link EarlierFeedback#ref()} of the entry whose defect recurs
 * @param finding the restated defect, located in the new version
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record FollowUp(String ref, Finding finding) {
	/** Rejects a follow-up that names no entry or carries no finding. */
	public FollowUp {
		Objects.requireNonNull(ref, "ref");
		Objects.requireNonNull(finding, "finding");
	}
}
