package de.tum.cit.aet.thesis.core.application.mailvariables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import de.tum.cit.aet.thesis.core.application.entity.Application;
import de.tum.cit.aet.thesis.core.dto.MailVariableDto;
import de.tum.cit.aet.thesis.core.topic.entity.Topic;
import de.tum.cit.aet.thesis.core.user.entity.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

class MailApplicationTest {

	@Test
	void fromApplication_null_returnsAllEmptyStrings() {
		MailApplication result = MailApplication.fromApplication(null);
		assertEquals("", result.thesisTitle());
		assertEquals("", result.applicantFirstName());
		assertEquals("", result.applicantLastName());
		assertEquals("", result.applicantEmail());
	}

	@Test
	void fromApplication_withApplicantAndTopic_mapsAllFields() {
		User user = new User();
		user.setFirstName("Max");
		user.setLastName("Mustermann");
		user.setUniversityId("ge47zig");
		user.setMatriculationNumber("12345678");
		user.setStudyDegree("BACHELOR");
		user.setStudyProgram("INFORMATICS");
		user.setEmail("max@example.com");
		user.setEnrolledAt(Instant.now());
		user.setSpecialSkills("skills");
		user.setInterests("interests");
		user.setProjects("projects");

		Application application = new Application();
		application.setUser(user);
		application.setDesiredStartDate(Instant.parse("2024-10-01T00:00:00Z"));
		application.setMotivation("strong motivation");
		application.setThesisTitle("My Thesis Title");

		MailApplication result = MailApplication.fromApplication(application);

		assertEquals("My Thesis Title", result.thesisTitle());
		assertEquals("Max", result.applicantFirstName());
		assertEquals("Mustermann", result.applicantLastName());
		assertEquals("max@example.com", result.applicantEmail());
		assertEquals("ge47zig", result.applicantUniversityId());
		assertEquals("12345678", result.applicantMatriculationNumber());
		assertEquals("Informatics", result.studyProgram());
		assertEquals("Bachelor", result.studyDegree());
		assertEquals("strong motivation", result.motivation());
		assertEquals("skills", result.specialSkills());
		assertEquals("interests", result.interests());
		assertEquals("projects", result.projects());
	}

	@Test
	void fromApplication_withTopicButNoTitle_fallsBackToTopicTitle() {
		Application application = new Application();
		Topic topic = new Topic();
		topic.setTitle("Topic Title");
		application.setTopic(topic);
		application.setDesiredStartDate(Instant.now());
		application.setMotivation("m");

		MailApplication result = MailApplication.fromApplication(application);
		assertEquals("Topic Title", result.thesisTitle());
	}

	@Test
	void fromApplication_blankTitleAndBlankTopicTitle_returnsEmpty() {
		Application application = new Application();
		application.setThesisTitle("   ");
		Topic topic = new Topic();
		topic.setTitle("");
		application.setTopic(topic);
		application.setDesiredStartDate(Instant.now());
		application.setMotivation("m");

		MailApplication result = MailApplication.fromApplication(application);
		assertEquals("", result.thesisTitle());
	}

	@Test
	void fromApplication_withoutApplicant_returnsEmptyApplicantFields() {
		Application application = new Application();
		application.setDesiredStartDate(Instant.now());
		application.setMotivation("m");
		application.setThesisTitle("title");

		MailApplication result = MailApplication.fromApplication(application);
		assertEquals("", result.applicantFirstName());
		assertEquals("", result.applicantUniversityId());
	}

	@Test
	void templateVariables_returnsExpectedCount() {
		List<MailVariableDto> vars = MailApplication.templateVariables();
		assertEquals(15, vars.size());
		assertNotNull(vars.get(0).label());
		for (MailVariableDto v : vars) {
			assertFalse(v.label().isBlank());
			assertFalse(v.templateVariable().isBlank());
		}
	}
}
