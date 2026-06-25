package de.tum.cit.aet.thesis.presentation.entity.key;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.UUID;

class ThesisPresentationInviteIdTest {

	private ThesisPresentationInviteId build(UUID presentationId, String email) {
		ThesisPresentationInviteId id = new ThesisPresentationInviteId();
		id.setPresentationId(presentationId);
		id.setEmail(email);
		return id;
	}

	@Test
	void equalsAndHashCode_identical_areEqual() {
		UUID pres = UUID.randomUUID();
		ThesisPresentationInviteId a = build(pres, "a@example.com");
		ThesisPresentationInviteId b = build(pres, "a@example.com");
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	void equals_sameInstance_isTrue() {
		ThesisPresentationInviteId id = build(UUID.randomUUID(), "x@x.com");
		assertTrue(id.equals(id));
	}

	@Test
	void equals_nullOrOther_isFalse() {
		ThesisPresentationInviteId id = build(UUID.randomUUID(), "x@x.com");
		assertNotEquals(id, null);
		assertNotEquals(id, "string");
	}

	@Test
	void equals_differentEmail_isFalse() {
		UUID p = UUID.randomUUID();
		assertNotEquals(build(p, "a@x.com"), build(p, "b@x.com"));
	}

	@Test
	void equals_differentPresentation_isFalse() {
		assertNotEquals(build(UUID.randomUUID(), "a@x.com"), build(UUID.randomUUID(), "a@x.com"));
	}
}
