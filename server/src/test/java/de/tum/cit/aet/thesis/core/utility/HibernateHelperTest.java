package de.tum.cit.aet.thesis.core.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.tum.cit.aet.thesis.core.exception.request.ResourceInvalidParametersException;
import de.tum.cit.aet.thesis.core.user.entity.User;
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
		void validFieldWithColumnAnnotation_returnsColumnName() {
			// User.firstName has @Column(name = "first_name")
			assertEquals("first_name", HibernateHelper.getColumnName(User.class, "firstName"));
		}

		@Test
		void invalidField_ThrowsResourceInvalidParametersException() {
			assertThrows(ResourceInvalidParametersException.class,
					() -> HibernateHelper.getColumnName(User.class, "nonExistentField"));
		}

		@Test
		void fieldWithoutColumnAnnotation_returnsFieldName() {
			// Use a local POJO field without @Column to exercise the fallback path.
			String result = HibernateHelper.getColumnName(WithoutColumn.class, "plainField");
			assertEquals("plainField", result);
		}
	}

	private static final class WithoutColumn {
		@SuppressWarnings("unused")
		private String plainField;
	}
}
