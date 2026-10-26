package de.tum.cit.aet.thesis.core.admin.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DataRetentionResultDtoTest {

	@Test
	void record_preservesValue() {
		DataRetentionResultDto dto = new DataRetentionResultDto(42);
		assertEquals(42, dto.deletedApplications());
	}

	@Test
	void zeroValue_isSupported() {
		assertEquals(0, new DataRetentionResultDto(0).deletedApplications());
	}
}
