package de.tum.cit.aet.thesis.thesis.entity.key;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.tum.cit.aet.thesis.thesis.constants.ThesisState;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class ThesisStateChangeIdTest {

	private ThesisStateChangeId build(UUID thesisId, ThesisState state) {
		ThesisStateChangeId id = new ThesisStateChangeId();
		id.setThesisId(thesisId);
		id.setState(state);
		return id;
	}

	@Test
	void equalsAndHashCode_identical_areEqual() {
		UUID thesis = UUID.randomUUID();
		ThesisStateChangeId a = build(thesis, ThesisState.WRITING);
		ThesisStateChangeId b = build(thesis, ThesisState.WRITING);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	void equals_sameInstance_isTrue() {
		ThesisStateChangeId id = build(UUID.randomUUID(), ThesisState.WRITING);
		assertTrue(id.equals(id));
	}

	@Test
	void equals_nullOrOther_isFalse() {
		ThesisStateChangeId id = build(UUID.randomUUID(), ThesisState.WRITING);
		assertNotEquals(id, null);
		assertNotEquals(id, "x");
	}

	@Test
	void equals_differentState_isFalse() {
		UUID thesis = UUID.randomUUID();
		assertNotEquals(build(thesis, ThesisState.WRITING), build(thesis, ThesisState.FINISHED));
	}

	@Test
	void equals_differentThesis_isFalse() {
		assertNotEquals(
				build(UUID.randomUUID(), ThesisState.WRITING),
				build(UUID.randomUUID(), ThesisState.WRITING)
		);
	}
}
