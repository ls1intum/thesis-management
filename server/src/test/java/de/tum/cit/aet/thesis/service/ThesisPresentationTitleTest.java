package de.tum.cit.aet.thesis.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.tum.cit.aet.thesis.constants.ThesisRoleName;
import de.tum.cit.aet.thesis.entity.Thesis;
import de.tum.cit.aet.thesis.entity.ThesisRole;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.entity.key.ThesisRoleId;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class ThesisPresentationTitleTest {

	@Test
	void shortThesisType_mapsKnownKeys() {
		assertEquals("BA", ThesisPresentationService.shortThesisType("BACHELOR"));
		assertEquals("MA", ThesisPresentationService.shortThesisType("MASTER"));
		assertEquals("IDP", ThesisPresentationService.shortThesisType("INTERDISCIPLINARY_PROJECT"));
		assertEquals("GR", ThesisPresentationService.shortThesisType("GUIDED_RESEARCH"));
	}

	@Test
	void shortThesisType_unknownKeyFallsBackToRawValue() {
		assertEquals("DOCTORATE", ThesisPresentationService.shortThesisType("DOCTORATE"));
	}

	@Test
	void shortThesisType_nullFallsBackToThesis() {
		assertEquals("Thesis", ThesisPresentationService.shortThesisType(null));
	}

	@Test
	void buildPresentationTitle_singleStudent() {
		Thesis thesis = thesis("BACHELOR", "Enhancing Terminal Usability");
		addStudent(thesis, "John", "Doe");

		assertEquals(
				"BA Presentation John Doe: Enhancing Terminal Usability",
				ThesisPresentationService.buildPresentationTitle(thesis)
		);
	}

	@Test
	void buildPresentationTitle_multipleStudentsJoinedWithAmpersand() {
		Thesis thesis = thesis("MASTER", "Joint Thesis");
		addStudent(thesis, "Alice", "Smith");
		addStudent(thesis, "Bob", "Jones");

		assertEquals(
				"MA Presentation Alice Smith & Bob Jones: Joint Thesis",
				ThesisPresentationService.buildPresentationTitle(thesis)
		);
	}

	@Test
	void buildPresentationTitle_noStudentsUsesDashSeparator() {
		Thesis thesis = thesis("INTERDISCIPLINARY_PROJECT", "Unassigned Topic");

		assertEquals(
				"IDP Presentation – Unassigned Topic",
				ThesisPresentationService.buildPresentationTitle(thesis)
		);
	}

	@Test
	void buildPresentationTitle_nullFirstNameDoesNotProduceNullLiteral() {
		Thesis thesis = thesis("GUIDED_RESEARCH", "Research Topic");
		addStudent(thesis, null, "Doe");

		assertEquals(
				"GR Presentation Doe: Research Topic",
				ThesisPresentationService.buildPresentationTitle(thesis)
		);
	}

	@Test
	void buildPresentationTitle_nameContainingCommaIsPreservedInSummary() {
		Thesis thesis = thesis("BACHELOR", "Some Title");
		addStudent(thesis, "Maria", "García, Sr.");

		assertEquals(
				"BA Presentation Maria García, Sr.: Some Title",
				ThesisPresentationService.buildPresentationTitle(thesis)
		);
	}

	@Test
	void buildPresentationTitle_unknownTypeFallsBackToRawKey() {
		Thesis thesis = thesis("DOCTORATE", "Doctoral Topic");
		addStudent(thesis, "Eve", "Williams");

		assertEquals(
				"DOCTORATE Presentation Eve Williams: Doctoral Topic",
				ThesisPresentationService.buildPresentationTitle(thesis)
		);
	}

	@Test
	void buildPresentationTitle_nullTypeFallsBackToThesisPrefix() {
		Thesis thesis = thesis(null, "Some Title");
		addStudent(thesis, "Pat", "Lee");

		assertEquals(
				"Thesis Presentation Pat Lee: Some Title",
				ThesisPresentationService.buildPresentationTitle(thesis)
		);
	}

	private static Thesis thesis(String type, String title) {
		Thesis thesis = new Thesis();
		thesis.setType(type);
		thesis.setTitle(title);
		thesis.setRoles(new ArrayList<>());
		return thesis;
	}

	private static void addStudent(Thesis thesis, String firstName, String lastName) {
		User user = new User();
		user.setFirstName(firstName);
		user.setLastName(lastName);

		ThesisRole role = new ThesisRole();
		ThesisRoleId roleId = new ThesisRoleId();
		roleId.setRole(ThesisRoleName.STUDENT);
		role.setId(roleId);
		role.setThesis(thesis);
		role.setUser(user);

		List<ThesisRole> roles = new ArrayList<>(thesis.getRoles());
		roles.add(role);
		thesis.setRoles(roles);
	}
}
