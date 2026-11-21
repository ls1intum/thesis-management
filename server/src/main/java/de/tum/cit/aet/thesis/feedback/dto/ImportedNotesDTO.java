package de.tum.cit.aet.thesis.feedback.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Response for the note import endpoint: the supervisor's notes as editable feedback entries, in
 * the order the notes raised them.
 *
 * <p>Reuses {@link AIFeedbackDraftDTO} because the client handles these exactly like preview
 * drafts — rows it appends to the unsaved batch — with one difference that matters to the UI: an
 * entry's category or severity is routinely absent here, because a terse note often does not say
 * enough to label it. The instructor picks those values or asks for a classification.
 *
 * @param entries the entries carved out of the notes; absent from the JSON when the notes held no
 *                feedback at all
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ImportedNotesDTO(
		List<AIFeedbackDraftDTO> entries
) {}
