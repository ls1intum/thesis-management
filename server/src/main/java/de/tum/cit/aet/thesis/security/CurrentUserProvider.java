package de.tum.cit.aet.thesis.security;

import de.tum.cit.aet.thesis.entity.ResearchGroup;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.exception.request.AccessDeniedException;
import de.tum.cit.aet.thesis.service.AuthenticationService;
import jakarta.annotation.PostConstruct;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
@RequiredArgsConstructor
public class CurrentUserProvider {

   private final AuthenticationService authenticationService;
   private User cachedUser;

   @PostConstruct
   public void loadUser() {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication instanceof JwtAuthenticationToken) {
         JwtAuthenticationToken jwt = (JwtAuthenticationToken) authentication;
         cachedUser = authenticationService.getAuthenticatedUserWithResearchGroup(jwt);
      } else {
         throw new AccessDeniedException("Please login first.");
      }
   }

   public User getUser() {
      return cachedUser;
   }

   public ResearchGroup getResearchGroupOrThrow() {
      ResearchGroup researchGroup = cachedUser.getResearchGroup();
      if (!canSeeAllResearchGroups() && researchGroup == null) {
         throw new AccessDeniedException("Your account must be assigned to a research group.");
      }
      return researchGroup;
   }

   public boolean isStudent() {
      return cachedUser.hasAnyGroup("student");
   }
   
   public boolean isAdvisor() {
      return cachedUser.hasAnyGroup("advisor");
   }
   
   public boolean isSupervisor() {
      return cachedUser.hasAnyGroup("supervisor");
   }
   
   public boolean isAdmin() {
      return cachedUser.hasAnyGroup("admin");
   }

   public boolean canSeeAllResearchGroups() {
      return isStudent();
   }

   public void assertSameResearchGroupIfNotPrivileged(ResearchGroup target) {
      if (!canSeeAllResearchGroups()) {
         assertCanAccessResearchGroup(target);
      }
   }
   
   public void assertCanAccessResearchGroup(ResearchGroup target) {
      if (canSeeAllResearchGroups()) {
         return;
      }

      ResearchGroup own = getResearchGroupOrThrow();
      if (target == null || !own.getId().equals(target.getId())) {
         throw new AccessDeniedException("This resource is not part of your research group.");
      }
   }
   
   public void assertCanAccessResearchGroup(UUID target) {
      if (canSeeAllResearchGroups()) {
         return;
      }

      ResearchGroup own = getResearchGroupOrThrow();
      if (!own.getId().equals(target)) {
         throw new AccessDeniedException("This resource is not part of your research group.");
      }
   }
}
