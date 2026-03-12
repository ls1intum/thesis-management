package de.tum.cit.aet.thesis.security;

import de.tum.cit.aet.thesis.repository.UserGroupRepository;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {
	private final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter;
	private final UserGroupRepository userGroupRepository;

	public JwtAuthConverter(UserGroupRepository userGroupRepository) {
		this.userGroupRepository = userGroupRepository;
		this.jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
	}

	@Override
	@Nullable
	public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
		String username = jwt.getClaim("preferred_username");

		Collection<GrantedAuthority> authorities = Stream.concat(
				jwtGrantedAuthoritiesConverter.convert(jwt).stream(),
				extractDatabaseRoles(username).stream()).collect(Collectors.toSet());

		return new JwtAuthenticationToken(jwt, authorities, username);
	}

	private Collection<? extends GrantedAuthority> extractDatabaseRoles(String username) {
		if (username == null) {
			return Set.of();
		}

		List<String> groupNames = userGroupRepository.findGroupNamesByUniversityId(username);

		return groupNames.stream()
				.map(group -> new SimpleGrantedAuthority("ROLE_" + group))
				.collect(Collectors.toSet());
	}
}
