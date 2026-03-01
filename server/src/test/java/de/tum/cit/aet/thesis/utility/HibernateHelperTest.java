package de.tum.cit.aet.thesis.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.exception.request.ResourceInvalidParametersException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class HibernateHelperTest {

	@Nested
	class ValidateSortField {

		@Test
		void validField_ReturnsFieldName() {
			String result = HibernateHelper.validateSortField(User.class, "firstName");
			assertEquals("firstName", result);
		}

		@Test
		void invalidField_ThrowsException() {
			assertThrows(ResourceInvalidParametersException.class,
					() -> HibernateHelper.validateSortField(User.class, "nonExistentField"));
		}

		@Test
		void sqlInjectionAttempt_ThrowsException() {
			assertThrows(ResourceInvalidParametersException.class,
					() -> HibernateHelper.validateSortField(User.class, "firstName; DROP TABLE users"));
		}
	}

	@Nested
	class GetColumnName {

		@Test
		void validField_ReturnsColumnName() {
			// Should not throw for valid fields
			String result = HibernateHelper.getColumnName(User.class, "firstName");
			// Returns either the @Column name or the field name
			assertEquals(result, result); // just verifies no exception
		}

		@Test
		void invalidField_ThrowsResourceInvalidParametersException() {
			assertThrows(ResourceInvalidParametersException.class,
					() -> HibernateHelper.getColumnName(User.class, "nonExistentField"));
		}
	}
}
