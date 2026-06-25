package de.tum.cit.aet.thesis.thesis.mailvariables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import de.tum.cit.aet.thesis.core.dto.MailVariableDto;
import de.tum.cit.aet.thesis.core.user.entity.User;
import de.tum.cit.aet.thesis.thesis.constants.ThesisRoleName;
import de.tum.cit.aet.thesis.thesis.entity.Thesis;
import de.tum.cit.aet.thesis.thesis.entity.ThesisRole;
import de.tum.cit.aet.thesis.thesis.entity.key.ThesisRoleId;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class MailThesisTest {

	private User user(String first, String last) {
		User u = new User();
		u.setId(UUID.randomUUID());
		u.setFirstName(first);
		u.setLastName(last);
		return u;
	}

	private ThesisRole role(Thesis t, User u, ThesisRoleName role) {
		ThesisRoleId id = new ThesisRoleId();
		id.setThesisId(t.getId());
		id.setUserId(u.getId());
		id.setRole(role);
		ThesisRole r = new ThesisRole();
		r.setId(id);
		r.setUser(u);
		r.setThesis(t);
		r.setPosition(0);
		return r;
	}

	@Test
	void fromThesis_null_returnsEmpty() {
		MailThesis t = MailThesis.fromThesis(null);
		assertEquals(new MailThesis("", "", "", "", "", "", "", ""), t);
	}

	@Test
	void fromThesis_basic_mapsExpectedFields() {
		Thesis thesis = new Thesis();
		thesis.setId(UUID.randomUUID());
		thesis.setTitle("My Thesis");
		thesis.setType("BACHELOR_THESIS");
		thesis.setAbstractField("Some abstract");
		thesis.setFinalGrade("1.3");
		thesis.setFinalFeedback("Nice work");

		User student = user("Max", "Mustermann");
		User supervisor = user("Alex", "Example");
		User examiner = user("Maria", "Musterfrau");

		List<ThesisRole> roles = new ArrayList<>();
		roles.add(role(thesis, student, ThesisRoleName.STUDENT));
		roles.add(role(thesis, supervisor, ThesisRoleName.SUPERVISOR));
		roles.add(role(thesis, examiner, ThesisRoleName.EXAMINER));
		thesis.setRoles(roles);

		MailThesis result = MailThesis.fromThesis(thesis);
		assertEquals("My Thesis", result.title());
		assertEquals("Bachelor Thesis", result.type());
		assertEquals("Some abstract", result.abstractText());
		assertEquals("Max Mustermann", result.students());
		assertEquals("Maria Musterfrau", result.examiners());
		assertEquals("Alex Example", result.supervisors());
		assertEquals("", result.finalGrade());
		assertEquals("", result.finalFeedback());
	}

	@Test
	void fromThesisWithGrade_includesFinalGradeFields() {
		Thesis thesis = new Thesis();
		thesis.setId(UUID.randomUUID());
		thesis.setTitle("title");
		thesis.setType("MASTER_THESIS");
		thesis.setAbstractField("abstract");
		thesis.setFinalGrade("2.0");
		thesis.setFinalFeedback("ok");
		thesis.setRoles(new ArrayList<>());

		MailThesis result = MailThesis.fromThesisWithGrade(thesis);
		assertEquals("2.0", result.finalGrade());
		assertEquals("ok", result.finalFeedback());
	}

	@Test
	void fromThesis_emptyRoles_returnsEmptyParticipantStrings() {
		Thesis thesis = new Thesis();
		thesis.setId(UUID.randomUUID());
		thesis.setTitle("t");
		thesis.setType("BACHELOR_THESIS");
		thesis.setAbstractField("a");
		thesis.setRoles(new ArrayList<>());
		MailThesis result = MailThesis.fromThesis(thesis);
		assertEquals("", result.students());
		assertEquals("", result.examiners());
		assertEquals("", result.supervisors());
	}

	@Test
	void templateVariables_sevenEntriesInThesisGroup() {
		List<MailVariableDto> vars = MailThesis.templateVariables();
		assertEquals(7, vars.size());
		assertNotNull(vars.get(0));
	}

	@Test
	void gradeTemplateVariables_twoEntries() {
		List<MailVariableDto> vars = MailThesis.gradeTemplateVariables();
		assertEquals(2, vars.size());
		assertEquals("Thesis Final Grade", vars.get(0).label());
		assertEquals("Thesis Final Feedback", vars.get(1).label());
	}
}
