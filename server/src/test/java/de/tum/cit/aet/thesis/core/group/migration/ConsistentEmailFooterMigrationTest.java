package de.tum.cit.aet.thesis.core.group.migration;

import static org.assertj.core.api.Assertions.assertThat;

import de.tum.cit.aet.thesis.mock.TestContainerImages;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

@Testcontainers
class ConsistentEmailFooterMigrationTest {

	private static final String FIRST_CHANGESET_ID = "49-add-email-signature-column";
	private static final String SIGNATURE_VARIABLE = "<div th:utext=\"${emailSignature}\"></div>";
	// The snippet the template editor used to offer as "Notification URL Footer".
	private static final String STANDALONE_FOOTER = "</hr><div style=\"text-align: center;font-size: 10px\"> "
			+ "Manage your notification settings <a th:href=\"${config.clientHost + '/settings/notifications'}\">here</a></div>";

	@Container
	final PostgreSQLContainer postgres = new PostgreSQLContainer(TestContainerImages.POSTGRES);

	@Test
	void migration_ReplacesSignOffsAndRemovesFooterSnippets() throws Exception {
		try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
			var database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
			try (var liquibase = new Liquibase("db/changelog/db.changelog-master.xml", new ClassLoaderResourceAccessor(), database)) {
				int before = 0;
				for (var changeSet : liquibase.getDatabaseChangeLog().getChangeSets()) {
					if (changeSet.getId().equals(FIRST_CHANGESET_ID)) {
						break;
					}
					before++;
				}
				liquibase.update(before, "prod");

				insertTemplate(connection, "TEST_SEEDED_SIGN_OFF",
						"<p>Hello</p>\n<p>Best regards,<br/>\nThe Thesis Coordination Team</p>");
				insertTemplate(connection, "TEST_FORMATTED_SIGN_OFF",
						"<p>Hello</p>\r\n<p>Best regards,<br />\r\nThe Thesis Coordination Team</p>");
				insertTemplate(connection, "TEST_CUSTOM_SIGN_OFF",
						"<p>Hello</p><p>Best regards,<br/>Jane Doe</p>");
				insertTemplate(connection, "TEST_NO_SIGN_OFF", "<p>Hello</p>");
				insertTemplate(connection, "TEST_EDITOR_FOOTER", "<p>Hello</p>" + STANDALONE_FOOTER);
				connection.commit();

				liquibase.update("prod");
				// A second startup must be a no-op.
				liquibase.update("prod");

				// The seeded sign-off and equivalent formatting (<br />, CRLF) both become the variable.
				assertThat(bodyOf(connection, "TEST_SEEDED_SIGN_OFF")).isEqualTo("<p>Hello</p>\n" + SIGNATURE_VARIABLE);
				assertThat(bodyOf(connection, "TEST_FORMATTED_SIGN_OFF")).isEqualTo("<p>Hello</p>\r\n" + SIGNATURE_VARIABLE);
				// A sign-off written by the group keeps its text and is not given a second signature.
				assertThat(bodyOf(connection, "TEST_CUSTOM_SIGN_OFF")).isEqualTo("<p>Hello</p><p>Best regards,<br/>Jane Doe</p>");
				// Templates without any sign-off get the variable once.
				assertThat(bodyOf(connection, "TEST_NO_SIGN_OFF")).isEqualTo("<p>Hello</p>\n" + SIGNATURE_VARIABLE);
				// The footer snippet is gone: MailBuilder.send() appends the notification link itself.
				assertThat(bodyOf(connection, "TEST_EDITOR_FOOTER")).isEqualTo("<p>Hello</p>\n" + SIGNATURE_VARIABLE);

				try (var statement = connection.createStatement();
						var result = statement.executeQuery("SELECT email_signature FROM research_group_settings")) {
					assertThat(result.next()).isFalse();
				}
			}
		}
	}

	private static void insertTemplate(Connection connection, String templateCase, String bodyHtml) throws SQLException {
		try (var statement = connection.prepareStatement(
				"INSERT INTO email_templates (email_template_id, template_case, subject, body_html) VALUES (?, ?, 'Subject', ?)")) {
			statement.setObject(1, UUID.randomUUID());
			statement.setString(2, templateCase);
			statement.setString(3, bodyHtml);
			statement.executeUpdate();
		}
	}

	private static String bodyOf(Connection connection, String templateCase) throws SQLException {
		try (var statement = connection.prepareStatement("SELECT body_html FROM email_templates WHERE template_case = ?")) {
			statement.setString(1, templateCase);
			try (var result = statement.executeQuery()) {
				assertThat(result.next()).as("template %s exists", templateCase).isTrue();
				return result.getString(1);
			}
		}
	}
}
