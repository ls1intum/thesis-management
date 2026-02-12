package de.tum.cit.aet.thesis.utility;

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
			throw new RuntimeException("Field not found: " + fieldName, e);
		}
	}
}
