package de.tum.cit.aet.thesis.proposal.mailvariables;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.tum.cit.aet.thesis.core.dto.MailVariableDto;
import de.tum.cit.aet.thesis.core.user.entity.User;
import de.tum.cit.aet.thesis.proposal.entity.ThesisProposal;
import org.junit.jupiter.api.Test;

import java.util.List;

class MailThesisProposalTest {

	@Test
	void fromProposal_null_returnsEmpty() {
		MailThesisProposal result = MailThesisProposal.fromProposal(null);
		assertEquals(new MailThesisProposal("", "", "", ""), result);
	}

	@Test
	void fromProposal_creatorAndApprover_mapAllFields() {
		User creator = new User();
		creator.setFirstName("Max");
		creator.setLastName("Mustermann");
		User approver = new User();
		approver.setFirstName("Maria");
		approver.setLastName("Musterfrau");

		ThesisProposal proposal = new ThesisProposal();
		proposal.setCreatedBy(creator);
		proposal.setApprovedBy(approver);

		MailThesisProposal result = MailThesisProposal.fromProposal(proposal);
		assertEquals("Max", result.creatorFirstName());
		assertEquals("Mustermann", result.creatorLastName());
		assertEquals("Maria", result.approverFirstName());
		assertEquals("Musterfrau", result.approverLastName());
	}

	@Test
	void fromProposal_noApprover_emptyApproverNames() {
		User creator = new User();
		creator.setFirstName("Max");
		creator.setLastName("Mustermann");
		ThesisProposal proposal = new ThesisProposal();
		proposal.setCreatedBy(creator);

		MailThesisProposal result = MailThesisProposal.fromProposal(proposal);
		assertEquals("Max", result.creatorFirstName());
		assertEquals("", result.approverFirstName());
		assertEquals("", result.approverLastName());
	}

	@Test
	void fromProposal_noCreator_emptyCreatorNames() {
		ThesisProposal proposal = new ThesisProposal();
		MailThesisProposal result = MailThesisProposal.fromProposal(proposal);
		assertEquals("", result.creatorFirstName());
		assertEquals("", result.creatorLastName());
	}

	@Test
	void templateVariables_fourEntriesInProposalGroup() {
		List<MailVariableDto> vars = MailThesisProposal.templateVariables();
		assertEquals(4, vars.size());
		for (MailVariableDto v : vars) {
			assertEquals("Proposal", v.group());
		}
	}
}
