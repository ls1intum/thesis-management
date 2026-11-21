package de.tum.cit.aet.thesis.core.notification.entity.key;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.UUID;

class NotificationSettingIdTest {

	private NotificationSettingId build(UUID userId, String name) {
		NotificationSettingId id = new NotificationSettingId();
		id.setUserId(userId);
		id.setName(name);
		return id;
	}

	@Test
	void equalsAndHashCode_identical_areEqual() {
		UUID user = UUID.randomUUID();
		NotificationSettingId a = build(user, "thesis-comment");
		NotificationSettingId b = build(user, "thesis-comment");
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	void equals_sameInstance_isTrue() {
		NotificationSettingId id = build(UUID.randomUUID(), "x");
		assertTrue(id.equals(id));
	}

	@Test
	void equals_nullOrOther_isFalse() {
		NotificationSettingId id = build(UUID.randomUUID(), "x");
		assertNotEquals(id, null);
		assertNotEquals(id, "string");
	}

	@Test
	void equals_differentName_isFalse() {
		UUID user = UUID.randomUUID();
		assertNotEquals(build(user, "a"), build(user, "b"));
	}

	@Test
	void equals_differentUser_isFalse() {
		assertNotEquals(build(UUID.randomUUID(), "a"), build(UUID.randomUUID(), "a"));
	}
}
