package de.tum.cit.aet.thesis.thesis.entity.jsonb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

class ThesisMetadataTest {

	@Test
	void getEmptyMetadata_returnsEmptyMutableMaps() {
		ThesisMetadata m = ThesisMetadata.getEmptyMetadata();
		assertNotNull(m.titles());
		assertNotNull(m.credits());
		assertTrue(m.titles().isEmpty());
		assertTrue(m.credits().isEmpty());

		m.titles().put("en", "Some Title");
		m.credits().put(UUID.randomUUID(), 30);

		assertEquals(1, m.titles().size());
		assertEquals(1, m.credits().size());
	}

	@Test
	void constructor_acceptsNullMaps_andReplacesThemWithEmpty() {
		ThesisMetadata m = new ThesisMetadata(null, null);
		assertNotNull(m.titles());
		assertNotNull(m.credits());
		assertTrue(m.titles().isEmpty());
		assertTrue(m.credits().isEmpty());
	}

	@Test
	void constructor_preservesProvidedMaps() {
		Map<String, String> titles = new HashMap<>();
		titles.put("de", "Titel");
		Map<UUID, Number> credits = new HashMap<>();
		UUID id = UUID.randomUUID();
		credits.put(id, 15);

		ThesisMetadata m = new ThesisMetadata(titles, credits);
		assertEquals("Titel", m.titles().get("de"));
		assertEquals(15, m.credits().get(id));
	}
}
