package de.tum.cit.aet.thesis.mailvariables;

import de.tum.cit.aet.thesis.dto.MailVariableDto;
import de.tum.cit.aet.thesis.entity.ThesisProposal;
import de.tum.cit.aet.thesis.entity.User;

import java.util.List;

/** Mail placeholder model for thesis proposal variables. */
public record MailThesisProposal(
		String creatorFirstName,
		String creatorLastName,
		String approverFirstName,
		String approverLastName
) {
	/**
	 * Builds a mail-safe thesis proposal model.
	 *
	 * @param proposal the thesis proposal entity
	 * @return mapped thesis proposal mail model
	 */
	public static MailThesisProposal fromProposal(ThesisProposal proposal) {
		if (proposal == null) {
			return new MailThesisProposal("", "", "", "");
		}

		User creator = proposal.getCreatedBy();
		User approver = proposal.getApprovedBy();

		return new MailThesisProposal(
				valueOrEmpty(creator != null ? creator.getFirstName() : null),
				valueOrEmpty(creator != null ? creator.getLastName() : null),
				valueOrEmpty(approver != null ? approver.getFirstName() : null),
				valueOrEmpty(approver != null ? approver.getLastName() : null)
		);
	}

	/**
	 * Returns all selectable template variables for thesis proposals.
	 *
	 * @return thesis proposal variable descriptors
	 */
	public static List<MailVariableDto> templateVariables() {
		return List.of(
				new MailVariableDto("Proposal Creator First Name", "[[${proposal.creatorFirstName}]]", "Max", "Proposal"),
				new MailVariableDto("Proposal Creator Last Name", "[[${proposal.creatorLastName}]]", "Mustermann", "Proposal"),
				new MailVariableDto("Proposal Approver First Name", "[[${proposal.approverFirstName}]]", "Maria", "Proposal"),
				new MailVariableDto("Proposal Approver Last Name", "[[${proposal.approverLastName}]]", "Musterfrau", "Proposal")
		);
	}

	private static String valueOrEmpty(String value) {
		return value == null ? "" : value;
	}
}
