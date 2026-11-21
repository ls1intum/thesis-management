package de.tum.cit.aet.thesis.core.user.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.tum.cit.aet.thesis.core.notification.entity.NotificationSetting;
import de.tum.cit.aet.thesis.core.notification.entity.key.NotificationSettingId;
import de.tum.cit.aet.thesis.core.user.entity.key.UserGroupId;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

class UserTest {

	private User user(UUID id) {
		User u = new User();
		u.setId(id);
		return u;
	}

	private UserGroup group(User user, String name) {
		UserGroupId gid = new UserGroupId();
		gid.setUserId(user.getId());
		gid.setGroup(name);
		UserGroup g = new UserGroup();
		g.setId(gid);
		g.setUser(user);
		return g;
	}

	private NotificationSetting notification(User user, String name, String email) {
		NotificationSettingId nid = new NotificationSettingId();
		nid.setUserId(user.getId());
		nid.setName(name);
		NotificationSetting n = new NotificationSetting();
		n.setId(nid);
		n.setUser(user);
		n.setEmail(email);
		return n;
	}

	@Test
	void getEmail_validAddress_returnsInternetAddress() {
		User u = user(UUID.randomUUID());
		u.setEmail("user@example.com");
		assertNotNull(u.getEmail());
		assertEquals("user@example.com", u.getEmail().getAddress());
	}

	@Test
	void getEmail_invalidAddress_returnsNull() {
		User u = user(UUID.randomUUID());
		// Address with unencoded whitespace fails RFC822 parsing
		u.setEmail("user @ example.com");
		assertNull(u.getEmail());
	}

	@Test
	void getAdjustedAvatar_returnsAvatar_whenPresent() {
		User u = user(UUID.randomUUID());
		u.setAvatar("avatar.jpg");
		assertEquals("avatar.jpg", u.getAdjustedAvatar());
	}

	@Test
	void getAdjustedAvatar_returnsNull_whenBlankOrMissing() {
		User u = user(UUID.randomUUID());
		assertNull(u.getAdjustedAvatar());
		u.setAvatar("   ");
		assertNull(u.getAdjustedAvatar());
	}

	@Test
	void isAnonymized_reflectsTimestamp() {
		User u = user(UUID.randomUUID());
		assertFalse(u.isAnonymized());
		u.setAnonymizedAt(java.time.Instant.now());
		assertTrue(u.isAnonymized());
	}

	@Test
	void hasNoGroup_trueOnlyWhenEmpty() {
		User u = user(UUID.randomUUID());
		assertTrue(u.hasNoGroup());

		Set<UserGroup> groups = new HashSet<>();
		groups.add(group(u, "student"));
		u.setGroups(groups);
		assertFalse(u.hasNoGroup());
	}

	@Test
	void hasAnyGroup_matchesAnyOfTheGivenGroups() {
		User u = user(UUID.randomUUID());
		Set<UserGroup> groups = new HashSet<>();
		groups.add(group(u, "student"));
		groups.add(group(u, "supervisor"));
		u.setGroups(groups);

		assertTrue(u.hasAnyGroup("student"));
		assertTrue(u.hasAnyGroup("admin", "supervisor"));
		assertFalse(u.hasAnyGroup("admin"));
		assertFalse(u.hasAnyGroup());
	}

	@Test
	void hasFullAccess_adminSupervisorAdvisor_alwaysTrue() {
		User actor = user(UUID.randomUUID());
		Set<UserGroup> groups = new HashSet<>();
		groups.add(group(actor, "admin"));
		actor.setGroups(groups);

		User other = user(UUID.randomUUID());
		assertTrue(other.hasFullAccess(actor));
	}

	@Test
	void hasFullAccess_sameId_returnsTrue() {
		UUID id = UUID.randomUUID();
		User actor = user(id);
		actor.setGroups(new HashSet<>());

		User target = user(id);
		assertTrue(target.hasFullAccess(actor));
	}

	@Test
	void hasFullAccess_nonPrivilegedDifferentUser_isFalse() {
		User actor = user(UUID.randomUUID());
		Set<UserGroup> groups = new HashSet<>();
		groups.add(group(actor, "student"));
		actor.setGroups(groups);

		User target = user(UUID.randomUUID());
		assertFalse(target.hasFullAccess(actor));
	}

	@Test
	void isNotificationEnabled_falseWhenSetToNone() {
		User u = user(UUID.randomUUID());
		List<NotificationSetting> list = new ArrayList<>();
		list.add(notification(u, "thesis-comment", "none"));
		u.setNotificationSettings(list);
		assertFalse(u.isNotificationEnabled("thesis-comment"));
	}

	@Test
	void isNotificationEnabled_trueByDefault() {
		User u = user(UUID.randomUUID());
		u.setNotificationSettings(new ArrayList<>());
		assertTrue(u.isNotificationEnabled("anything"));
	}

	@Test
	void isNotificationEnabled_trueIfDifferentSettingNone() {
		User u = user(UUID.randomUUID());
		List<NotificationSetting> list = new ArrayList<>();
		list.add(notification(u, "other", "none"));
		u.setNotificationSettings(list);
		assertTrue(u.isNotificationEnabled("requested"));
	}

	@Test
	void getNotificationEmail_returnsConfiguredValue() {
		User u = user(UUID.randomUUID());
		List<NotificationSetting> list = new ArrayList<>();
		list.add(notification(u, "all-applications", "own"));
		u.setNotificationSettings(list);
		assertEquals("own", u.getNotificationEmail("all-applications"));
	}

	@Test
	void getNotificationEmail_defaultForNewApplications_isOwn() {
		User u = user(UUID.randomUUID());
		u.setNotificationSettings(new ArrayList<>());
		assertEquals("own", u.getNotificationEmail("new-applications"));
	}

	@Test
	void getNotificationEmail_defaultForOther_isAll() {
		User u = user(UUID.randomUUID());
		u.setNotificationSettings(new ArrayList<>());
		assertEquals("all", u.getNotificationEmail("thesis-presentation"));
	}
}
