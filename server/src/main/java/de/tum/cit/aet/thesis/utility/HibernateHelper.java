package de.tum.cit.aet.thesis.utility;

import de.tum.cit.aet.thesis.exception.request.ResourceInvalidParametersException;

import jakarta.persistence.Column;

import java.lang.reflect.Field;

/**
 * Provides helper methods for resolving JPA/Hibernate metadata such as database column names from entity fields.
 */
public class HibernateHelper {
	/**
	 * Resolves the database column name for a given entity field using the @Column annotation, falling back to the field name.
	 *
	 * @param entityClass the entity class to inspect
	 * @param fieldName the name of the field
	 * @return the database column name
	 * @throws ResourceInvalidParametersException if the field does not exist on the entity
	 */
	public static String getColumnName(Class<?> entityClass, String fieldName) {
		try {
			Field field = entityClass.getDeclaredField(fieldName);
			Column column = field.getAnnotation(Column.class);
			if (column != null) {
				return column.name();
			}
			// If no Column annotation, return field name as fallback
			return fieldName;
		} catch (NoSuchFieldException e) {
			throw new ResourceInvalidParametersException("Invalid sort field: " + fieldName);
		}
	}

	/**
	 * Validates that a field exists on the given entity class and returns the Java field name.
	 * Use this for JPQL-based queries where the entity property name (not the DB column name) is required.
	 *
	 * @param entityClass the entity class to inspect
	 * @param fieldName the name of the field to validate
	 * @return the validated field name (unchanged)
	 * @throws ResourceInvalidParametersException if the field does not exist on the entity
	 */
	public static String validateSortField(Class<?> entityClass, String fieldName) {
		try {
			entityClass.getDeclaredField(fieldName);
			return fieldName;
		} catch (NoSuchFieldException e) {
			throw new ResourceInvalidParametersException("Invalid sort field: " + fieldName);
		}
	}
}
