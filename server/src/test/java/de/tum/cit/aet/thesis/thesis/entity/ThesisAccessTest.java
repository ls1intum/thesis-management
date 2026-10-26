package de.tum.cit.aet.thesis.thesis.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.tum.cit.aet.thesis.core.user.entity.User;
import de.tum.cit.aet.thesis.core.user.entity.UserGroup;
import de.tum.cit.aet.thesis.core.user.entity.key.UserGroupId;
import de.tum.cit.aet.thesis.thesis.constants.ThesisRoleName;
import de.tum.cit.aet.thesis.thesis.constants.ThesisState;
import de.tum.cit.aet.thesis.thesis.constants.ThesisVisibility;
import de.tum.cit.aet.thesis.thesis.entity.key.ThesisRoleId;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

class ThesisAccessTest {

	private User user(String... groups) {
		User u = new User();
		u.setId(UUID.randomUUID());
		Set<UserGroup> userGroups = new HashSet<>();
		for (String g : groups) {
			UserGroupId id = new UserGroupId();
			id.setUserId(u.getId());
			id.setGroup(g);
			UserGroup ug = new UserGroup();
			ug.setId(id);
			ug.setUser(u);
			userGroups.add(ug);
		}
		u.setGroups(userGroups);
		return u;
	}

	private Thesis baseThesis(ThesisVisibility visibility, ThesisState state) {
		Thesis t = new Thesis();
		t.setId(UUID.randomUUID());
		t.setVisibility(visibility);
		t.setState(state);
		t.setRoles(new ArrayList<>());
		return t;
	}

	private ThesisRole roleOf(Thesis thesis, User user, ThesisRoleName role) {
		ThesisRoleId id = new ThesisRoleId();
		id.setThesisId(thesis.getId());
		id.setUserId(user.getId());
		id.setRole(role);
		ThesisRole r = new ThesisRole();
		r.setId(id);
		r.setUser(user);
		r.setThesis(thesis);
		r.setPosition(0);
		return r;
	}

	@Test
	void hasExaminerAccess_nullUser_isFalse() {
		Thesis t = baseThesis(ThesisVisibility.INTERNAL, ThesisState.WRITING);
		assertFalse(t.hasExaminerAccess(null));
	}

	@Test
	void hasExaminerAccess_adminAlways_true() {
		Thesis t = baseThesis(ThesisVisibility.INTERNAL, ThesisState.WRITING);
		User admin = user("admin");
		assertTrue(t.hasExaminerAccess(admin));
	}

	@Test
	void hasExaminerAccess_supervisorGroupAndExaminerRole_isTrue() {
		Thesis t = baseThesis(ThesisVisibility.INTERNAL, ThesisState.WRITING);
		User u = user("supervisor");
		t.getRoles().add(roleOf(t, u, ThesisRoleName.EXAMINER));
		assertTrue(t.hasExaminerAccess(u));
	}

	@Test
	void hasExaminerAccess_supervisorGroupButNoExaminerRole_isFalse() {
		Thesis t = baseThesis(ThesisVisibility.INTERNAL, ThesisState.WRITING);
		User u = user("supervisor");
		assertFalse(t.hasExaminerAccess(u));
	}

	@Test
	void hasSupervisorAccess_nullUser_isFalse() {
		Thesis t = baseThesis(ThesisVisibility.INTERNAL, ThesisState.WRITING);
		assertFalse(t.hasSupervisorAccess(null));
	}

	@Test
	void hasSupervisorAccess_advisorWithSupervisorRole_isTrue() {
		Thesis t = baseThesis(ThesisVisibility.INTERNAL, ThesisState.WRITING);
		User u = user("advisor");
		t.getRoles().add(roleOf(t, u, ThesisRoleName.SUPERVISOR));
		assertTrue(t.hasSupervisorAccess(u));
	}

	@Test
	void hasSupervisorAccess_advisorButNotInRoles_isFalse() {
		Thesis t = baseThesis(ThesisVisibility.INTERNAL, ThesisState.WRITING);
		User u = user("advisor");
		assertFalse(t.hasSupervisorAccess(u));
	}

	@Test
	void hasStudentAccess_studentInThesis_isTrue() {
		Thesis t = baseThesis(ThesisVisibility.INTERNAL, ThesisState.WRITING);
		User student = user("student");
		t.getRoles().add(roleOf(t, student, ThesisRoleName.STUDENT));
		assertTrue(t.hasStudentAccess(student));
	}

	@Test
	void hasStudentAccess_unrelatedUser_isFalse() {
		Thesis t = baseThesis(ThesisVisibility.INTERNAL, ThesisState.WRITING);
		assertFalse(t.hasStudentAccess(user("student")));
	}

	@Test
	void hasReadAccess_publicFinishedThesis_alwaysTrue() {
		Thesis t = baseThesis(ThesisVisibility.PUBLIC, ThesisState.FINISHED);
		assertTrue(t.hasReadAccess(null));
		assertTrue(t.hasReadAccess(user()));
	}

	@Test
	void hasReadAccess_internalNotFinished_nullUserFalse() {
		Thesis t = baseThesis(ThesisVisibility.INTERNAL, ThesisState.WRITING);
		assertFalse(t.hasReadAccess(null));
	}

	@Test
	void hasReadAccess_publicNotFinished_anyUserTrue() {
		Thesis t = baseThesis(ThesisVisibility.PUBLIC, ThesisState.WRITING);
		assertTrue(t.hasReadAccess(user("student")));
	}

	@Test
	void hasReadAccess_internalAdvisorAccess_isTrue() {
		Thesis t = baseThesis(ThesisVisibility.INTERNAL, ThesisState.WRITING);
		assertTrue(t.hasReadAccess(user("advisor")));
		assertTrue(t.hasReadAccess(user("supervisor")));
	}

	@Test
	void hasReadAccess_studentVisibility_anyMemberRole_isTrue() {
		Thesis t = baseThesis(ThesisVisibility.STUDENT, ThesisState.WRITING);
		assertTrue(t.hasReadAccess(user("student")));
	}

	@Test
	void hasReadAccess_studentVisibility_nonMember_isFalse() {
		Thesis t = baseThesis(ThesisVisibility.STUDENT, ThesisState.WRITING);
		assertFalse(t.hasReadAccess(user()));
	}

	@Test
	void getPresentation_existing_returnsOptionalWithValue() {
		Thesis t = baseThesis(ThesisVisibility.PUBLIC, ThesisState.WRITING);
		ThesisFile file = new ThesisFile();
		file.setId(UUID.randomUUID());
		file.setType("DRAFT");
		t.setFiles(List.of(file));

		// Test getFileById success path
		assertTrue(t.getFileById(file.getId()).isPresent());
		assertFalse(t.getFileById(UUID.randomUUID()).isPresent());

		// Test getLatestFile success path
		assertTrue(t.getLatestFile("DRAFT").isPresent());
		assertFalse(t.getLatestFile("UNKNOWN").isPresent());

		// Empty presentation/feedback/proposal lists
		t.setPresentations(new ArrayList<>());
		t.setFeedback(new ArrayList<>());
		t.setProposals(new ArrayList<>());
		assertFalse(t.getPresentation(UUID.randomUUID()).isPresent());
		assertFalse(t.getFeedbackItem(UUID.randomUUID()).isPresent());
		assertFalse(t.getProposalById(UUID.randomUUID()).isPresent());
	}

	@Test
	void isAnonymized_reflectsTimestamp() {
		Thesis t = baseThesis(ThesisVisibility.PUBLIC, ThesisState.FINISHED);
		assertFalse(t.isAnonymized());
		t.setAnonymizedAt(java.time.Instant.now());
		assertTrue(t.isAnonymized());
	}
}
