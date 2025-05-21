package de.tum.cit.aet.thesis.service;

import de.tum.cit.aet.thesis.entity.ResearchGroup;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.entity.UserGroup;
import de.tum.cit.aet.thesis.exception.request.AccessDeniedException;
import de.tum.cit.aet.thesis.exception.request.ResourceNotFoundException;
import de.tum.cit.aet.thesis.repository.ResearchGroupRepository;
import de.tum.cit.aet.thesis.repository.UserRepository;
import de.tum.cit.aet.thesis.security.CurrentUserProvider;
import de.tum.cit.aet.thesis.utility.HibernateHelper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class ResearchGroupService {

  private final ResearchGroupRepository researchGroupRepository;
  private final UserService userService;
  private final ObjectProvider<CurrentUserProvider> currentUserProviderProvider;
  private final UserRepository userRepository;
  private final AccessManagementService accessManagementService;

  @Autowired
  public ResearchGroupService(ResearchGroupRepository researchGroupRepository,
      UserService userService, ObjectProvider<CurrentUserProvider> currentUserProviderProvider,
      UserRepository userRepository, AccessManagementService accessManagementService) {
      this.researchGroupRepository = researchGroupRepository;
      this.userService = userService;
      this.currentUserProviderProvider = currentUserProviderProvider;
      this.userRepository = userRepository;
      this.accessManagementService = accessManagementService;
  }

  private CurrentUserProvider currentUserProvider() {
    return currentUserProviderProvider.getObject();
  }

  public Page<ResearchGroup> getAll(
      String[] heads,
      String[] campuses,
      boolean includeArchived,
      String searchQuery,
      int page,
      int limit,
      String sortBy,
      String sortOrder
  ) {
    if (!currentUserProvider().canSeeAllResearchGroups()) {
      return new PageImpl<>(List.of(currentUserProvider().getResearchGroupOrThrow()),
              PageRequest.of(0, 1),
              1);
    }
    Sort.Order order = new Sort.Order(
        sortOrder.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC,
        HibernateHelper.getColumnName(ResearchGroup.class, sortBy)
    );

    String searchQueryFilter =
        searchQuery == null || searchQuery.isEmpty() ? null : searchQuery.toLowerCase();
    String[] headsFilter = heads == null || heads.length == 0 ? null : heads;
    String[] campusesFilter = campuses == null || campuses.length == 0 ? null : campuses;

    Pageable pageable = limit == -1
        ? PageRequest.of(0, Integer.MAX_VALUE, Sort.by(order))
        : PageRequest.of(page, limit, Sort.by(order));

    return researchGroupRepository.searchResearchGroup(
        headsFilter,
        campusesFilter,
        includeArchived,
        searchQueryFilter,
        pageable
    );
  }

  public ResearchGroup findById(UUID researchGroupId) {
    ResearchGroup researchGroup = researchGroupRepository.findById(researchGroupId)
        .orElseThrow(() -> new ResourceNotFoundException(
            String.format("Research Group with id %s not found.", researchGroupId)));
    currentUserProvider().assertCanAccessResearchGroup(researchGroup);
    return researchGroup;
  }

  @Transactional
  public ResearchGroup createResearchGroup(
      String headUsername,
      String name,
      String abbreviation,
      String description,
      String websiteUrl,
      String campus
  ) {
    //Get the User by universityId else create the user
    User head = getUserByUsernameOrCreate(headUsername);
    if (head.getResearchGroup() != null) {
        throw new AccessDeniedException("User is already assigned to a research group.");
    }

    //Add supervisor role in keycloak
    accessManagementService.assignSupervisorRole(head);
    Set<UserGroup> updatedGroupsHead = accessManagementService.syncRolesFromKeycloakToDatabase(head);
    head.setGroups(updatedGroupsHead);

    ResearchGroup researchGroup = new ResearchGroup();
    researchGroup.setHead(head);
    researchGroup.setName(name);
    researchGroup.setAbbreviation(abbreviation);
    researchGroup.setDescription(description);
    researchGroup.setWebsiteUrl(websiteUrl);
    researchGroup.setCampus(campus);
    researchGroup.setCreatedAt(Instant.now());
    researchGroup.setUpdatedAt(Instant.now());
    researchGroup.setCreatedBy(currentUserProvider().getUser());
    researchGroup.setUpdatedBy(currentUserProvider().getUser());
    researchGroup.setArchived(false);

    ResearchGroup savedResearchGroup =  researchGroupRepository.save(researchGroup);

    head.setResearchGroup(savedResearchGroup);
    userRepository.save(head);

    return savedResearchGroup;
  }

  private User getUserByUsernameOrCreate(String username) {
    User user = userRepository.findByUniversityId(username).orElseGet(() -> {
      User newUser = new User();
      Instant currentTime = Instant.now();

      newUser.setJoinedAt(currentTime);
      newUser.setUpdatedAt(currentTime);

      // Load user data from Keycloak
      AccessManagementService.KeycloakUserInformation userElement = accessManagementService.getUserByUsername(username);

      newUser.setUniversityId(userElement.username());
      newUser.setFirstName(userElement.firstName());
      newUser.setLastName(userElement.lastName());
      newUser.setEmail(userElement.email());

      userRepository.save(newUser);
      return newUser;
    });

    return user;
  }

  @Transactional
  public ResearchGroup updateResearchGroup(
      ResearchGroup researchGroup,
      String headUsername,
      String name,
      String abbreviation,
      String description,
      String websiteUrl,
      String campus
  ) {
    if(researchGroup.isArchived()) {
      throw new AccessDeniedException("Cannot update an archived research group.");
    }
    currentUserProvider().assertCanAccessResearchGroup(researchGroup);

    //Get the User by universityId else create the user
    User head = getUserByUsernameOrCreate(headUsername);
    if (head.getResearchGroup() != null) {
      throw new AccessDeniedException("User is already assigned to a research group.");
    }

    //Remove ResearchGroup from old head and set it to the new head
    User oldHead = researchGroup.getHead();
    oldHead.setResearchGroup(null);
    researchGroup.setHead(head);

    //Give new head supervisor as role and remove the role from the old head
    accessManagementService.assignSupervisorRole(head);
    accessManagementService.removeResearchGroupRoles(oldHead);
    Set<UserGroup> updatedGroupsHead = accessManagementService.syncRolesFromKeycloakToDatabase(head);
    head.setGroups(updatedGroupsHead);
    Set<UserGroup> updatedGroupsOldHead = accessManagementService.syncRolesFromKeycloakToDatabase(oldHead);
    oldHead.setGroups(updatedGroupsOldHead);

    researchGroup.setName(name);
    researchGroup.setAbbreviation(abbreviation);
    researchGroup.setDescription(description);
    researchGroup.setWebsiteUrl(websiteUrl);
    researchGroup.setCampus(campus);
    researchGroup.setUpdatedAt(Instant.now());
    researchGroup.setUpdatedBy(currentUserProvider().getUser());

    ResearchGroup savedResearchGroup =  researchGroupRepository.save(researchGroup);

    head.setResearchGroup(savedResearchGroup);
    userRepository.save(oldHead);
    userRepository.save(head);

    return savedResearchGroup;
  }

  public Page<User> getAllResearchGroupMembers(UUID researchGroupId, Integer page, Integer limit, String sortBy, String sortOrder) {
    Sort.Order order = new Sort.Order(sortOrder.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);

    return userRepository
            .searchUsers(researchGroupId, null, null, PageRequest.of(page, limit, Sort.by(order)));
  }

  public void archiveResearchGroup(ResearchGroup researchGroup) {
    currentUserProvider().assertCanAccessResearchGroup(researchGroup);
    researchGroup.setUpdatedAt(Instant.now());
    researchGroup.setUpdatedBy(currentUserProvider().getUser());
    researchGroup.setArchived(true);

    researchGroupRepository.save(researchGroup);
  }

  public User assignUserToResearchGroup(String username, UUID researchGroupId) {

    User user = getUserByUsernameOrCreate(username);
    ResearchGroup researchGroup = findById(researchGroupId);

    if (user.getResearchGroup() != null) {
      throw new AccessDeniedException("User is already assigned to a research group.");
    }

    if(researchGroup != null && researchGroup.isArchived()){
      throw new AccessDeniedException("Cannot assign user to an archived research group.");
    }

    user.setResearchGroup(researchGroup);

    //Assign member the advisor role in keycloak and update database
    accessManagementService.assignAdvisorRole(user);
    Set<UserGroup> updatedGroups = accessManagementService.syncRolesFromKeycloakToDatabase(user);
    user.setGroups(updatedGroups);

    userRepository.save(user);
    return user;
  }

    public User removeUserFromResearchGroup(UUID userId, UUID researchGroupId) {
        User user = userService.findById(userId);

        if (!user.getResearchGroup().getId().equals(researchGroupId)) {
          throw new AccessDeniedException("User is not assigned to this research group.");
        }
        if (user.getResearchGroup().isArchived()) {
            throw new AccessDeniedException("Cannot remove user from an archived research group.");
        }
        if (user.getResearchGroup().getHead() == user) {
            throw new AccessDeniedException("Cannot remove the head of the research group.");
        }


        //Remove advisor role in keycloak and update database
        accessManagementService.removeResearchGroupRoles(user);
        Set<UserGroup> updatedGroups = accessManagementService.syncRolesFromKeycloakToDatabase(user);
        user.setGroups(updatedGroups);
        user.setResearchGroup(null);

        userRepository.save(user);

        return user;
    }
}