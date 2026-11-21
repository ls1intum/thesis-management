package de.tum.cit.aet.thesis.core.topic.entity.key;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.tum.cit.aet.thesis.thesis.constants.ThesisRoleName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class TopicRoleIdTest {

	private TopicRoleId build(UUID topicId, UUID userId, ThesisRoleName role) {
		TopicRoleId id = new TopicRoleId();
		id.setTopicId(topicId);
		id.setUserId(userId);
		id.setRole(role);
		return id;
	}

	@Test
	void equalsAndHashCode_identicalValues_areEqual() {
		UUID topic = UUID.randomUUID();
		UUID user = UUID.randomUUID();
		TopicRoleId a = build(topic, user, ThesisRoleName.SUPERVISOR);
		TopicRoleId b = build(topic, user, ThesisRoleName.SUPERVISOR);

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	void equals_sameInstance_isTrue() {
		TopicRoleId id = build(UUID.randomUUID(), UUID.randomUUID(), ThesisRoleName.SUPERVISOR);
		assertTrue(id.equals(id));
	}

	@Test
	void equals_nullOrOtherClass_isFalse() {
		TopicRoleId id = build(UUID.randomUUID(), UUID.randomUUID(), ThesisRoleName.SUPERVISOR);
		assertNotEquals(id, null);
		assertNotEquals(id, "string");
	}

	@Test
	void equals_differentTopic_isFalse() {
		UUID user = UUID.randomUUID();
		TopicRoleId a = build(UUID.randomUUID(), user, ThesisRoleName.SUPERVISOR);
		TopicRoleId b = build(UUID.randomUUID(), user, ThesisRoleName.SUPERVISOR);
		assertNotEquals(a, b);
	}

	@Test
	void equals_differentUser_isFalse() {
		UUID topic = UUID.randomUUID();
		TopicRoleId a = build(topic, UUID.randomUUID(), ThesisRoleName.SUPERVISOR);
		TopicRoleId b = build(topic, UUID.randomUUID(), ThesisRoleName.SUPERVISOR);
		assertNotEquals(a, b);
	}

	@Test
	void equals_differentRole_isFalse() {
		UUID topic = UUID.randomUUID();
		UUID user = UUID.randomUUID();
		TopicRoleId a = build(topic, user, ThesisRoleName.SUPERVISOR);
		TopicRoleId b = build(topic, user, ThesisRoleName.EXAMINER);
		assertNotEquals(a, b);
	}

	@Test
	void gettersReturnAssignedValues() {
		UUID topic = UUID.randomUUID();
		UUID user = UUID.randomUUID();
		TopicRoleId id = build(topic, user, ThesisRoleName.STUDENT);

		assertEquals(topic, id.getTopicId());
		assertEquals(user, id.getUserId());
		assertEquals(ThesisRoleName.STUDENT, id.getRole());
	}
}
