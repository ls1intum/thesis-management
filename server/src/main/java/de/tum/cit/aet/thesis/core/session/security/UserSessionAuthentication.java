package de.tum.cit.aet.thesis.core.session.security;

import de.tum.cit.aet.thesis.core.session.entity.UserSession;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * The authentication of a request that carries a valid session. The principal name is the user's university
 * id; the authorities are the roles currently stored in {@code user_groups}.
 */
public class UserSessionAuthentication extends AbstractAuthenticationToken {
	private final String universityId;
	private final UserSession session;

	/**
	 * Creates an authenticated token.
	 *
	 * @param universityId the user's university id
	 * @param session the session that authenticated the request
	 * @param authorities the user's current roles
	 */
	public UserSessionAuthentication(String universityId, UserSession session, Collection<? extends GrantedAuthority> authorities) {
		super(authorities);
		this.universityId = universityId;
		this.session = session;
		setAuthenticated(true);
	}

	@Override
	public Object getCredentials() {
		return null;
	}

	@Override
	public Object getPrincipal() {
		return universityId;
	}

	/**
	 * Returns the session that authenticated the request.
	 *
	 * @return the session
	 */
	public UserSession getSession() {
		return session;
	}
}
