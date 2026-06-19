package de.tum.cit.aet.thesis.mock;

/**
 * Central source of truth for container images used by integration tests.
 *
 * <p>The PostgreSQL image tag is injected via the {@code postgres.image.tag} system property, which
 * the Gradle {@code test} task resolves from the {@code POSTGRES_IMAGE_TAG} entry in the repo-root
 * {@code .env} file (shared with the docker-compose files) or the same-named environment variable.
 * The fallback keeps IDE-launched tests (which do not go through Gradle) working. When bumping the
 * PostgreSQL version, update {@code .env} and the other references listed in {@code docs/DATABASE.md}.
 */
public final class TestContainerImages {

	public static final String POSTGRES = "postgres:" + System.getProperty("postgres.image.tag", "18.4-alpine");

	private TestContainerImages() {
	}
}
