package de.tum.cit.aet.thesis.thesis.mailvariables;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.tum.cit.aet.thesis.core.dto.MailVariableDto;
import de.tum.cit.aet.thesis.core.user.entity.User;
import de.tum.cit.aet.thesis.thesis.entity.ThesisAssessment;
import org.junit.jupiter.api.Test;

import java.util.List;

class MailThesisAssessmentTest {

	@Test
	void fromAssessment_null_returnsEmpty() {
		MailThesisAssessment result = MailThesisAssessment.fromAssessment(null);
		assertEquals(new MailThesisAssessment("", "", "", "", "", ""), result);
	}

	@Test
	void fromAssessment_full_mapsAllFields() {
		User creator = new User();
		creator.setFirstName("Max");
		creator.setLastName("Mustermann");
		ThesisAssessment assessment = new ThesisAssessment();
		assessment.setCreatedBy(creator);
		assessment.setSummary("summary");
		assessment.setPositives("positives");
		assessment.setNegatives("negatives");
		assessment.setGradeSuggestion("1.3");

		MailThesisAssessment result = MailThesisAssessment.fromAssessment(assessment);
		assertEquals("Max", result.creatorFirstName());
		assertEquals("Mustermann", result.creatorLastName());
		assertEquals("summary", result.summary());
		assertEquals("positives", result.positives());
		assertEquals("negatives", result.negatives());
		assertEquals("1.3", result.gradeSuggestion());
	}

	@Test
	void fromAssessment_noCreator_namesEmpty() {
		ThesisAssessment assessment = new ThesisAssessment();
		MailThesisAssessment result = MailThesisAssessment.fromAssessment(assessment);
		assertEquals("", result.creatorFirstName());
		assertEquals("", result.creatorLastName());
	}

	@Test
	void templateVariables_sixAssessmentEntries() {
		List<MailVariableDto> vars = MailThesisAssessment.templateVariables();
		assertEquals(6, vars.size());
		for (MailVariableDto v : vars) {
			assertEquals("Assessment", v.group());
		}
	}
}
