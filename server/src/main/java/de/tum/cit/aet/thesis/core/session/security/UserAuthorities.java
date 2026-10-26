package de.tum.cit.aet.thesis.core.session.security;

import de.tum.cit.aet.thesis.core.user.repository.UserGroupRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Loads a user's authorities from the {@code user_groups} table. The database is the single source of truth
 * for authorization; neither Keycloak nor the session carries roles.
 */
@Component
public class UserAuthorities {
	private final UserGroupRepository userGroupRepository;

	/**
	 * Creates the loader.
	 *
	 * @param userGroupRepository the repository holding the user's groups
	 */
	public UserAuthorities(UserGroupRepository userGroupRepository) {
		this.userGroupRepository = userGroupRepository;
	}

	/**
	 * Returns the roles of a user.
	 *
	 * @param universityId the user's university id
	 * @return one {@code ROLE_<group>} authority per group
	 */
	public Collection<GrantedAuthority> forUser(String universityId) {
		if (universityId == null) {
			return Set.of();
		}

		return userGroupRepository.findGroupNamesByUniversityId(universityId).stream()
				.<GrantedAuthority>map(group -> new SimpleGrantedAuthority("ROLE_" + group))
				.collect(Collectors.toSet());
	}
}
