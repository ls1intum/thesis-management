package de.tum.cit.aet.thesis.thesis.entity.key;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.tum.cit.aet.thesis.thesis.constants.ThesisRoleName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class ThesisRoleIdTest {

	private ThesisRoleId build(UUID thesisId, UUID userId, ThesisRoleName role) {
		ThesisRoleId id = new ThesisRoleId();
		id.setThesisId(thesisId);
		id.setUserId(userId);
		id.setRole(role);
		return id;
	}

	@Test
	void equalsAndHashCode_identicalValues_areEqual() {
		UUID thesis = UUID.randomUUID();
		UUID user = UUID.randomUUID();
		ThesisRoleId a = build(thesis, user, ThesisRoleName.SUPERVISOR);
		ThesisRoleId b = build(thesis, user, ThesisRoleName.SUPERVISOR);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	void equals_sameInstance_isTrue() {
		ThesisRoleId id = build(UUID.randomUUID(), UUID.randomUUID(), ThesisRoleName.STUDENT);
		assertTrue(id.equals(id));
	}

	@Test
	void equals_nullOrOther_isFalse() {
		ThesisRoleId id = build(UUID.randomUUID(), UUID.randomUUID(), ThesisRoleName.STUDENT);
		assertNotEquals(id, null);
		assertNotEquals(id, "other");
	}

	@Test
	void equals_differentRole_isFalse() {
		UUID thesis = UUID.randomUUID();
		UUID user = UUID.randomUUID();
		assertNotEquals(build(thesis, user, ThesisRoleName.SUPERVISOR), build(thesis, user, ThesisRoleName.STUDENT));
	}

	@Test
	void equals_differentThesis_isFalse() {
		UUID user = UUID.randomUUID();
		assertNotEquals(
				build(UUID.randomUUID(), user, ThesisRoleName.SUPERVISOR),
				build(UUID.randomUUID(), user, ThesisRoleName.SUPERVISOR)
		);
	}

	@Test
	void equals_differentUser_isFalse() {
		UUID thesis = UUID.randomUUID();
		assertNotEquals(
				build(thesis, UUID.randomUUID(), ThesisRoleName.SUPERVISOR),
				build(thesis, UUID.randomUUID(), ThesisRoleName.SUPERVISOR)
		);
	}
}
