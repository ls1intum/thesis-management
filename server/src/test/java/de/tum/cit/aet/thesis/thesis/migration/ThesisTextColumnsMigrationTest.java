package de.tum.cit.aet.thesis.thesis.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.tum.cit.aet.thesis.mock.TestContainerImages;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.DriverManager;
import java.sql.SQLException;

@Testcontainers
class ThesisTextColumnsMigrationTest {

	private static final String CHANGESET_ID = "48-thesis-text-columns";

	@Container
	final PostgreSQLContainer postgres = new PostgreSQLContainer(TestContainerImages.POSTGRES);

	@ParameterizedTest
	@CsvSource({"varchar(2000),varchar(2000)", "text,text", "varchar(2000),text", "text,varchar(2000)"})
	void migration_PreservesContentAndAllowsLongAbstractsAndInfo(String abstractType, String infoType) throws Exception {
		try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
				var statement = connection.createStatement()) {
			var database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
			try (var liquibase = new Liquibase("db/changelog/db.changelog-master.xml", new ClassLoaderResourceAccessor(), database)) {
				int before = 0;
				for (var changeSet : liquibase.getDatabaseChangeLog().getChangeSets()) {
					if (changeSet.getId().equals(CHANGESET_ID)) {
						break;
					}
					before++;
				}
				liquibase.update(before, "prod");

				// Older installations kept these varchar columns even though the initial changelog now uses TEXT.
				statement.execute("ALTER TABLE theses ALTER COLUMN abstract TYPE " + abstractType + ", ALTER COLUMN info TYPE " + infoType);
				statement.execute("""
						INSERT INTO theses (thesis_id, title, type, language, metadata, abstract, info, state, created_at, abstract_suggestion)
						VALUES ('00000000-0000-4000-a000-000000000001', 'Migration test', 'BACHELOR', 'ENGLISH', '{}'::jsonb,
								'Existing abstract ä', 'Existing info ü', 'WRITING', now(), '<p>' || repeat('a', 2157) || '</p>')
						""");
				if (abstractType.startsWith("varchar")) {
					var savepoint = connection.setSavepoint();
					assertThatThrownBy(() -> statement.executeUpdate("UPDATE theses SET abstract = abstract_suggestion"))
							.isInstanceOf(SQLException.class).hasMessageContaining("value too long");
					connection.rollback(savepoint);
				}
				connection.commit();

				liquibase.update("prod");
				// A second startup must also succeed, including after an operational repair already widened the columns.
				liquibase.update("prod");

				try (var result = statement.executeQuery("SELECT abstract, info, length(abstract_suggestion) FROM theses")) {
					assertThat(result.next()).isTrue();
					assertThat(result.getString(1)).isEqualTo("Existing abstract ä");
					assertThat(result.getString(2)).isEqualTo("Existing info ü");
					assertThat(result.getInt(3)).isEqualTo(2164);
				}
				try (var result = statement.executeQuery("""
						SELECT data_type, character_maximum_length, is_nullable
						FROM information_schema.columns
						WHERE table_schema = 'public' AND table_name = 'theses' AND column_name IN ('abstract', 'info')
						""")) {
					int columns = 0;
					while (result.next()) {
						assertThat(result.getString(1)).isEqualTo("text");
						assertThat(result.getObject(2)).isNull();
						assertThat(result.getString(3)).isEqualTo("NO");
						columns++;
					}
					assertThat(columns).isEqualTo(2);
				}

				statement.executeUpdate("UPDATE theses SET abstract = abstract_suggestion, info = repeat('ü', 10000), abstract_suggestion = NULL");
				try (var result = statement.executeQuery("SELECT abstract, info, abstract_suggestion FROM theses")) {
					assertThat(result.next()).isTrue();
					assertThat(result.getString(1)).isEqualTo("<p>" + "a".repeat(2157) + "</p>");
					assertThat(result.getString(2)).isEqualTo("ü".repeat(10000));
					assertThat(result.getString(3)).isNull();
				}
			}
		}
	}
}
