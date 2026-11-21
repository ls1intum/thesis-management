package de.tum.cit.aet.thesis.core.organization.migration;

import static org.assertj.core.api.Assertions.assertThat;

import de.tum.cit.aet.thesis.mock.TestContainerImages;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Migrates a database to the state right before the schools/study programs changeset, adds legacy data
 * (students with free-text study programs, a thesis) and applies the changeset.
 */
@Testcontainers
class StudyProgramMigrationTest {
	private static final String CHANGELOG = "db/changelog/db.changelog-master.xml";
	private static final String CHANGESET_ID = "46-schools-departments-study-programs";

	@Container
	static PostgreSQLContainer postgres = new PostgreSQLContainer(TestContainerImages.POSTGRES);

	private static String uuid() {
		return UUID.randomUUID().toString();
	}

	private static List<String> column(Statement statement, String query) throws Exception {
		List<String> values = new ArrayList<>();

		try (ResultSet rs = statement.executeQuery(query)) {
			while (rs.next()) {
				values.add(rs.getString(1));
			}
		}

		return values;
	}

	private static void insertUser(Statement statement, String id, String universityId, String studyProgram) throws Exception {
		statement.execute(String.format(
				"INSERT INTO users (user_id, university_id, study_program, updated_at, joined_at) VALUES ('%s', '%s', %s, now(), now())",
				id, universityId, studyProgram == null ? "NULL" : "'" + studyProgram + "'"));
	}

	@Test
	void migration_TurnsLegacyStudyProgramsIntoRowsAndLinksUsersAndTheses() throws Exception {
		try (Connection connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
				Statement statement = connection.createStatement()) {
			Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
			Liquibase liquibase = new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), database);

			int before = 0;
			for (var changeSet : liquibase.getDatabaseChangeLog().getChangeSets()) {
				if (changeSet.getId().equals(CHANGESET_ID)) {
					break;
				}
				before++;
			}
			assertThat(before).isPositive();

			liquibase.update(before, "prod");

			String csStudent = uuid();
			String customStudent = uuid();
			String caseVariantStudent = uuid();
			String noProgramStudent = uuid();
			String blankStudent = uuid();
			insertUser(statement, csStudent, "cs-student", "COMPUTER_SCIENCE");
			insertUser(statement, customStudent, "custom-student", "DATA_SCIENCE_AND_AI");
			insertUser(statement, caseVariantStudent, "case-student", "computer_science");
			insertUser(statement, noProgramStudent, "none-student", null);
			insertUser(statement, blankStudent, "blank-student", "  ");

			String head = uuid();
			insertUser(statement, head, "head", null);
			String researchGroup = uuid();
			statement.execute(String.format(
					"INSERT INTO research_groups (research_group_id, head_user_id, name, abbreviation, created_at, updated_at, archived) "
							+ "VALUES ('%s', '%s', 'Group', 'GRP', now(), now(), false)", researchGroup, head));
			String thesis = uuid();
			statement.execute(String.format(
					"INSERT INTO theses (thesis_id, title, type, language, metadata, info, abstract, state, created_at, research_group_id) "
							+ "VALUES ('%s', 'Legacy thesis', 'MASTER', 'ENGLISH', '{}'::jsonb, '', '', 'WRITING', now(), '%s')",
					thesis, researchGroup));
			statement.execute(String.format(
					"INSERT INTO thesis_roles (thesis_id, user_id, role, position, assigned_at, assigned_by) VALUES ('%s', '%s', 'STUDENT', 0, now(), '%s')",
					thesis, customStudent, head));

			liquibase.update("prod");

			assertThat(column(statement, "SELECT key || '=' || name FROM study_programs ORDER BY key"))
					.containsExactly("COMPUTER_SCIENCE=Computer Science", "DATA_SCIENCE_AND_AI=Data Science And Ai");

			assertThat(column(statement, "SELECT sp.key FROM users u JOIN study_programs sp ON sp.study_program_id = u.study_program_id WHERE u.user_id = '" + csStudent + "'"))
					.containsExactly("COMPUTER_SCIENCE");
			assertThat(column(statement, "SELECT sp.key FROM users u JOIN study_programs sp ON sp.study_program_id = u.study_program_id WHERE u.user_id = '" + customStudent + "'"))
					.containsExactly("DATA_SCIENCE_AND_AI");
			assertThat(column(statement, "SELECT sp.key FROM users u JOIN study_programs sp ON sp.study_program_id = u.study_program_id WHERE u.user_id = '" + caseVariantStudent + "'"))
					.containsExactly("COMPUTER_SCIENCE");
			assertThat(column(statement, "SELECT count(*) FROM users WHERE user_id IN ('" + noProgramStudent + "', '" + blankStudent + "') AND study_program_id IS NOT NULL"))
					.containsExactly("0");

			assertThat(column(statement, "SELECT sp.key FROM theses t JOIN study_programs sp ON sp.study_program_id = t.study_program_id WHERE t.thesis_id = '" + thesis + "'"))
					.containsExactly("DATA_SCIENCE_AND_AI");

			assertThat(column(statement, "SELECT count(*) FROM schools")).containsExactly("0");
		}
	}
}
