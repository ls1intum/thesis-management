package de.tum.cit.aet.thesis.service;

import de.tum.cit.aet.thesis.entity.ResearchGroup;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.exception.request.AccessDeniedException;
import de.tum.cit.aet.thesis.exception.request.ResourceNotFoundException;
import de.tum.cit.aet.thesis.repository.ResearchGroupRepository;
import de.tum.cit.aet.thesis.utility.HibernateHelper;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResearchGroupService {

  private final ResearchGroupRepository researchGroupRepository;
  private final UserService userService;

  @Autowired
  public ResearchGroupService(ResearchGroupRepository researchGroupRepository,
      UserService userService) {
    this.researchGroupRepository = researchGroupRepository;
    this.userService = userService;
  }

  public Page<ResearchGroup> getAll(
      User user,
      String[] heads,
      String[] campuses,
      boolean includeArchived,
      String searchQuery,
      int page,
      int limit,
      String sortBy,
      String sortOrder
  ) {
    if (user.hasAnyGroup("professor", "supervisor", "advisor")) {
      heads = new String[]{user.getId().toString()};
    }
    Sort.Order order = new Sort.Order(
        sortOrder.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC,
        HibernateHelper.getColumnName(ResearchGroup.class, sortBy)
    );

    String searchQueryFilter =
        searchQuery == null || searchQuery.isEmpty() ? null : searchQuery.toLowerCase();
    String[] headsFilter = heads == null || heads.length == 0 ? null : heads;
    String[] campusesFilter = campuses == null || campuses.length == 0 ? null : campuses;

    return researchGroupRepository.searchResearchGroup(
        headsFilter,
        campusesFilter,
        includeArchived,
        searchQueryFilter,
        PageRequest.of(page, limit, Sort.by(order))
    );
  }

  public ResearchGroup findById(User user, UUID researchGroupId) {
    ResearchGroup researchGroup = researchGroupRepository.findById(researchGroupId)
        .orElseThrow(() -> new ResourceNotFoundException(
            String.format("Research Group with id %s not found.", researchGroupId)));

    if (user.hasAnyGroup("student")) {
      return researchGroup;
    }

    if (user.getResearchGroup() == null
        || !researchGroup.getId().equals(user.getResearchGroup().getId()) && user.hasAnyGroup(
        "professor", "supervisor", "advisor")) {
      throw new AccessDeniedException("You are not authorized to access this Research Group.");
    }

    return researchGroup;
  }

  @Transactional
  public ResearchGroup createResearchGroup(
      User creator,
      UUID headId,
      String name,
      String abbreviation,
      String description,
      String websiteUrl,
      String campus
  ) {
    User head = userService.findById(headId);

    if (head == null) {
      throw new ResourceNotFoundException(String.format("Head with id %s not found.", headId));
    }

    ResearchGroup researchGroup = new ResearchGroup();

    researchGroup.setHead(head);
    researchGroup.setName(name);
    researchGroup.setAbbreviation(abbreviation);
    researchGroup.setDescription(description);
    researchGroup.setWebsiteUrl(websiteUrl);
    researchGroup.setCampus(campus);
    researchGroup.setCreatedAt(Instant.now());
    researchGroup.setUpdatedAt(Instant.now());
    researchGroup.setCreatedBy(creator);
    researchGroup.setUpdatedBy(creator);
    researchGroup.setArchived(false);

    return researchGroupRepository.save(researchGroup);
  }

  @Transactional
  public ResearchGroup updateResearchGroup(
      User updater,
      ResearchGroup researchGroup,
      UUID headId,
      String name,
      String abbreviation,
      String description,
      String websiteUrl,
      String campus
  ) {
    User head = userService.findById(headId);

    if (head == null) {
      throw new ResourceNotFoundException(String.format("Head with id %s not found.", headId));
    }

    researchGroup.setHead(head);
    researchGroup.setName(name);
    researchGroup.setAbbreviation(abbreviation);
    researchGroup.setDescription(description);
    researchGroup.setWebsiteUrl(websiteUrl);
    researchGroup.setCampus(campus);
    researchGroup.setUpdatedAt(Instant.now());
    researchGroup.setUpdatedBy(updater);

    return researchGroupRepository.save(researchGroup);
  }

  public void archiveResearchGroup(User updater, ResearchGroup researchGroup) {
    researchGroup.setUpdatedAt(Instant.now());
    researchGroup.setUpdatedBy(updater);
    researchGroup.setArchived(true);

    researchGroupRepository.save(researchGroup);
  }
}