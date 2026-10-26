package de.tum.cit.aet.thesis.core.user.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.Optional;

class GravatarServiceTest {

	private final GravatarService service = new GravatarService();

	@Test
	void fetchProfilePicture_nullEmail_returnsEmpty() {
		Optional<byte[]> result = service.fetchProfilePicture(null);
		assertTrue(result.isEmpty());
	}

	@Test
	void fetchProfilePicture_blankEmail_returnsEmpty() {
		assertTrue(service.fetchProfilePicture("").isEmpty());
		assertTrue(service.fetchProfilePicture("   ").isEmpty());
	}
}
