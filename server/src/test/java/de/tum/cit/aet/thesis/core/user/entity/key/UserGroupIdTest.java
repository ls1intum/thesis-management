package de.tum.cit.aet.thesis.core.user.entity.key;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.UUID;

class UserGroupIdTest {

	private UserGroupId build(UUID userId, String group) {
		UserGroupId id = new UserGroupId();
		id.setUserId(userId);
		id.setGroup(group);
		return id;
	}

	@Test
	void equalsAndHashCode_identicalValues_areEqual() {
		UUID user = UUID.randomUUID();
		UserGroupId a = build(user, "admin");
		UserGroupId b = build(user, "admin");

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	void equals_sameInstance_isTrue() {
		UserGroupId id = build(UUID.randomUUID(), "student");
		assertTrue(id.equals(id));
	}

	@Test
	void equals_nullOrOtherClass_isFalse() {
		UserGroupId id = build(UUID.randomUUID(), "student");
		assertNotEquals(id, null);
		assertNotEquals(id, new Object());
	}

	@Test
	void equals_differentUser_isFalse() {
		UserGroupId a = build(UUID.randomUUID(), "admin");
		UserGroupId b = build(UUID.randomUUID(), "admin");
		assertNotEquals(a, b);
	}

	@Test
	void equals_differentGroup_isFalse() {
		UUID user = UUID.randomUUID();
		UserGroupId a = build(user, "admin");
		UserGroupId b = build(user, "student");
		assertNotEquals(a, b);
	}

	@Test
	void gettersReturnAssignedValues() {
		UUID user = UUID.randomUUID();
		UserGroupId id = build(user, "supervisor");
		assertEquals(user, id.getUserId());
		assertEquals("supervisor", id.getGroup());
	}
}
