package de.tum.cit.aet.thesis.core.user.entity;

import static org.assertj.core.api.Assertions.assertThat;

import de.tum.cit.aet.thesis.core.user.repository.UserRepository;
import de.tum.cit.aet.thesis.mock.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Every request that loads a user and saves it (for example the sync of the profile on each page load) must only write
 * the columns it changed. Otherwise it silently reverts columns another request changed in the meantime, such as the
 * profile picture.
 */
@Testcontainers
class UserConcurrentUpdateTest extends BaseIntegrationTest {

	@DynamicPropertySource
	static void configureDynamicProperties(DynamicPropertyRegistry registry) {
		configureProperties(registry);
	}

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PlatformTransactionManager transactionManager;

	private String avatarOf(UUID userId) {
		TransactionTemplate readTransaction = new TransactionTemplate(transactionManager);
		readTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		readTransaction.setReadOnly(true);

		return readTransaction.execute(status -> jdbcTemplate.queryForObject(
				"SELECT avatar FROM users WHERE user_id = ?::uuid", String.class, userId.toString()));
	}

	@Test
	void savingAStaleUserDoesNotRevertTheAvatarAnotherRequestSet() throws Exception {
		TestUser user = createRandomTestUser(List.of("student"));
		TransactionTemplate request = new TransactionTemplate(transactionManager);
		TransactionTemplate otherRequest = new TransactionTemplate(transactionManager);
		otherRequest.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		request.executeWithoutResult(status -> {
			// request A loads the user while it has no picture ...
			User loaded = userRepository.findById(user.userId()).orElseThrow();
			assertThat(loaded.getAvatar()).isNull();

			// ... request B sets the picture and commits ...
			otherRequest.executeWithoutResult(inner -> jdbcTemplate.update(
					"UPDATE users SET avatar = 'new-picture.png' WHERE user_id = ?::uuid", user.userId().toString()));

			// ... and request A saves an unrelated change of its stale copy
			loaded.setLastLoginAt(Instant.now());
			userRepository.save(loaded);
			entityManager.flush();
		});

		assertThat(avatarOf(user.userId())).isEqualTo("new-picture.png");
	}

	@Test
	void savingAUserStillWritesTheColumnsItChanged() throws Exception {
		TestUser user = createRandomTestUser(List.of("student"));
		TransactionTemplate request = new TransactionTemplate(transactionManager);

		request.executeWithoutResult(status -> {
			User loaded = userRepository.findById(user.userId()).orElseThrow();
			loaded.setAvatar("chosen.png");
			loaded.setFirstName("Changed");
			userRepository.save(loaded);
		});

		User reloaded = userRepository.findById(user.userId()).orElseThrow();
		assertThat(reloaded.getAvatar()).isEqualTo("chosen.png");
		assertThat(reloaded.getFirstName()).isEqualTo("Changed");
	}
}
