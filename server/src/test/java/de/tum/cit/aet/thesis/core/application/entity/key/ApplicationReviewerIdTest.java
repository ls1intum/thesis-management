package de.tum.cit.aet.thesis.core.application.entity.key;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.UUID;

class ApplicationReviewerIdTest {

	private ApplicationReviewerId build(UUID applicationId, UUID userId) {
		ApplicationReviewerId id = new ApplicationReviewerId();
		id.setApplicationId(applicationId);
		id.setUserId(userId);
		return id;
	}

	@Test
	void equalsAndHashCode_identical_areEqual() {
		UUID app = UUID.randomUUID();
		UUID user = UUID.randomUUID();
		ApplicationReviewerId a = build(app, user);
		ApplicationReviewerId b = build(app, user);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	void equals_sameInstance_isTrue() {
		ApplicationReviewerId id = build(UUID.randomUUID(), UUID.randomUUID());
		assertTrue(id.equals(id));
	}

	@Test
	void equals_nullOrOther_isFalse() {
		ApplicationReviewerId id = build(UUID.randomUUID(), UUID.randomUUID());
		assertNotEquals(id, null);
		assertNotEquals(id, "other");
	}

	@Test
	void equals_differentApplication_isFalse() {
		UUID user = UUID.randomUUID();
		assertNotEquals(build(UUID.randomUUID(), user), build(UUID.randomUUID(), user));
	}

	@Test
	void equals_differentUser_isFalse() {
		UUID app = UUID.randomUUID();
		assertNotEquals(build(app, UUID.randomUUID()), build(app, UUID.randomUUID()));
	}

	@Test
	void gettersReturnAssignedValues() {
		UUID app = UUID.randomUUID();
		UUID user = UUID.randomUUID();
		ApplicationReviewerId id = build(app, user);
		assertEquals(app, id.getApplicationId());
		assertEquals(user, id.getUserId());
	}
}
