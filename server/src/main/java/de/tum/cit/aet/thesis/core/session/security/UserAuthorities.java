package de.tum.cit.aet.thesis.core.session.security;

import de.tum.cit.aet.thesis.core.user.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Derives a user's authorities from their groups in the {@code user_groups} table. The database is the single
 * source of truth for authorization; neither Keycloak nor the session carries roles. The user is loaded from
 * the database on every request, so a role change takes effect immediately.
 */
@Component
public class UserAuthorities {
	/**
	 * Returns the roles of a user.
	 *
	 * @param user the user, loaded from the database for the current request
	 * @return one {@code ROLE_<group>} authority per group
	 */
	public Collection<GrantedAuthority> forUser(User user) {
		return user.getGroups().stream()
				.map(group -> group.getId().getGroup())
				.<GrantedAuthority>map(group -> new SimpleGrantedAuthority("ROLE_" + group))
				.collect(Collectors.toSet());
	}
}
