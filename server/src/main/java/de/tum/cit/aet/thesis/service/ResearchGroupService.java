package de.tum.cit.aet.thesis.service;

import de.tum.cit.aet.thesis.entity.ResearchGroup;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.exception.request.AccessDeniedException;
import de.tum.cit.aet.thesis.exception.request.ResourceNotFoundException;
import de.tum.cit.aet.thesis.repository.ResearchGroupRepository;
import de.tum.cit.aet.thesis.repository.UserRepository;
import de.tum.cit.aet.thesis.security.CurrentUserProvider;
import de.tum.cit.aet.thesis.utility.HibernateHelper;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResearchGroupService {

  private final ResearchGroupRepository researchGroupRepository;
  private final UserService userService;
  private final ObjectProvider<CurrentUserProvider> currentUserProviderProvider;
  private final UserRepository userRepository;

  @Autowired
  public ResearchGroupService(ResearchGroupRepository researchGroupRepository,
      UserService userService, ObjectProvider<CurrentUserProvider> currentUserProviderProvider,
      UserRepository userRepository) {
      this.researchGroupRepository = researchGroupRepository;
      this.userService = userService;
      this.currentUserProviderProvider = currentUserProviderProvider;
      this.userRepository = userRepository;
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
      heads = new String[]{currentUserProvider().getUser().getId().toString()};
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
      UUID headId,
      String name,
      String abbreviation,
      String description,
      String websiteUrl,
      String campus
  ) {
    User head = userService.findById(headId);

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

  @Transactional
  public ResearchGroup updateResearchGroup(
      ResearchGroup researchGroup,
      UUID headId,
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
    User head = userService.findById(headId);

    researchGroup.setHead(head);
    researchGroup.setName(name);
    researchGroup.setAbbreviation(abbreviation);
    researchGroup.setDescription(description);
    researchGroup.setWebsiteUrl(websiteUrl);
    researchGroup.setCampus(campus);
    researchGroup.setUpdatedAt(Instant.now());
    researchGroup.setUpdatedBy(currentUserProvider().getUser());

    ResearchGroup savedResearchGroup =  researchGroupRepository.save(researchGroup);

    head.setResearchGroup(savedResearchGroup);
    userRepository.save(head);

    return savedResearchGroup;
  }

  public void archiveResearchGroup(ResearchGroup researchGroup) {
    currentUserProvider().assertCanAccessResearchGroup(researchGroup);
    researchGroup.setUpdatedAt(Instant.now());
    researchGroup.setUpdatedBy(currentUserProvider().getUser());
    researchGroup.setArchived(true);

    researchGroupRepository.save(researchGroup);
  }

  public void assignUserToResearchGroup(UUID userId, UUID researchGroupId) {
    User user = userService.findById(userId);
    ResearchGroup researchGroup = findById(researchGroupId);

    if (user.getResearchGroup() != null) {
      throw new AccessDeniedException("User is already assigned to a research group.");
    }

    if(researchGroup != null && researchGroup.isArchived()){
      throw new AccessDeniedException("Cannot assign user to an archived research group.");
    }

    user.setResearchGroup(researchGroup);
    userRepository.save(user);
  }
}