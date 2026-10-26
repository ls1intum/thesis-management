package de.tum.cit.aet.thesis.core.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.time.Instant;

class ErrorDtoTest {

	@Test
	void fromException_withMessage_propagatesMessage() {
		Instant before = Instant.now();
		ErrorDto dto = ErrorDto.fromException(new RuntimeException("boom"));
		Instant after = Instant.now();

		assertEquals("boom", dto.message());
		assertNotNull(dto.timestamp());
		assertTrue(!dto.timestamp().isBefore(before));
		assertTrue(!dto.timestamp().isAfter(after));
	}

	@Test
	void fromException_nullMessage_fallsBackToDefault() {
		ErrorDto dto = ErrorDto.fromException(new RuntimeException((String) null));
		assertEquals("An unexpected error occurred", dto.message());
	}

	@Test
	void recordAccessors_returnConstructorValues() {
		Instant ts = Instant.parse("2024-01-01T00:00:00Z");
		ErrorDto dto = new ErrorDto(ts, "msg");
		assertEquals(ts, dto.timestamp());
		assertEquals("msg", dto.message());
	}
}
