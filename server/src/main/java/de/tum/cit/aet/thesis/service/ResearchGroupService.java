package de.tum.cit.aet.thesis.service;

import de.tum.cit.aet.thesis.entity.ResearchGroup;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.exception.request.AccessDeniedException;
import de.tum.cit.aet.thesis.exception.request.ResourceNotFoundException;
import de.tum.cit.aet.thesis.repository.ResearchGroupRepository;
import de.tum.cit.aet.thesis.repository.ThesisRepository;
import de.tum.cit.aet.thesis.repository.UserRepository;
import de.tum.cit.aet.thesis.security.CurrentUserProvider;
import de.tum.cit.aet.thesis.utility.HibernateHelper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Manages research group lifecycle, membership, and role assignments.
 */
@Service
public class ResearchGroupService {

	private final ResearchGroupRepository researchGroupRepository;
	private final UserService userService;
	private final ObjectProvider<CurrentUserProvider> currentUserProviderProvider;
	private final UserRepository userRepository;
	private final AccessManagementService accessManagementService;

	private final ThesisRepository thesisRepository;

	/**
	 * Injects the research group repository, user service, access management, and current user provider.
	 *
	 * @param researchGroupRepository     the research group repository
	 * @param userService                 the user service
	 * @param currentUserProviderProvider the current user provider
	 * @param userRepository              the user repository
	 * @param accessManagementService     the access management service
	 * @param thesisRepository            the thesis repository
	 */
	@Autowired
	public ResearchGroupService(ResearchGroupRepository researchGroupRepository,
								UserService userService, ObjectProvider<CurrentUserProvider> currentUserProviderProvider,
								UserRepository userRepository, AccessManagementService accessManagementService, ThesisRepository thesisRepository) {
		this.researchGroupRepository = researchGroupRepository;
		this.userService = userService;
		this.currentUserProviderProvider = currentUserProviderProvider;
		this.userRepository = userRepository;
		this.accessManagementService = accessManagementService;
		this.thesisRepository = thesisRepository;
	}

	private CurrentUserProvider currentUserProvider() {
		return currentUserProviderProvider.getObject();
	}

	/**
	 * Returns a paginated and filtered list of research groups visible to the current user.
	 *
	 * @param heads           the head usernames to filter by
	 * @param campuses        the campuses to filter by
	 * @param includeArchived whether to include archived research groups
	 * @param searchQuery     the search query to filter results
	 * @param page            the page number for pagination
	 * @param limit           the number of items per page
	 * @param sortBy          the field to sort by
	 * @param sortOrder       the sort direction (asc or desc)
	 * @return the paginated list of research groups
	 */
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

	/**
	 * Returns all non-archived research groups matching the search query without access restrictions.
	 *
	 * @param searchQuery the search query to filter results
	 * @return the page of matching research groups
	 */
	public Page<ResearchGroup> getAllLight(String searchQuery) {
		String searchQueryFilter =
				searchQuery == null || searchQuery.isEmpty() ? null : searchQuery.toLowerCase();

		Sort.Order order = new Sort.Order(Sort.Direction.ASC, HibernateHelper.getColumnName(ResearchGroup.class, "name"));

		return researchGroupRepository.searchResearchGroup(
				null,
				null,
				false,
				searchQueryFilter,
				PageRequest.of(0, Integer.MAX_VALUE, Sort.by(order))
		);
	}

	/**
	 * Returns the active research groups accessible to the current user, including groups via active theses.
	 *
	 * @return the list of active research groups for the current user
	 */
	public List<ResearchGroup> getActiveResearchGroupsForUser() {

		User currentUser = currentUserProviderProvider.getObject().getUser();

		if (currentUser == null) {
			throw new AccessDeniedException("User is not authenticated.");
		}

		// Return all groups if the person is admin
		if (currentUser.hasAnyGroup("admin")) {
			Sort.Order order = new Sort.Order(Sort.Direction.ASC, HibernateHelper.getColumnName(ResearchGroup.class, "name"));

			Page<ResearchGroup> allResearchGroups = researchGroupRepository.searchResearchGroup(
					null,
					null,
					false,
					"",
					PageRequest.of(0, Integer.MAX_VALUE, Sort.by(order))
			);

			return allResearchGroups.stream().toList();
		}

		Set<ResearchGroup> result = new HashSet<>();
		if (currentUser.getResearchGroup() != null) {
			result.add(currentUser.getResearchGroup());
		}

		List<ResearchGroup> viaTheses = thesisRepository.findActiveStudentThesisResearchGroups(currentUser.getId());
		result.addAll(viaTheses);

		return new ArrayList<>(result);
	}

	/**
	 * Finds a research group by its ID with access control enforcement.
	 *
	 * @param researchGroupId the unique identifier of the research group
	 * @return the found research group
	 */
	public ResearchGroup findById(UUID researchGroupId) {
		return findById(researchGroupId, false);
	}

	/**
	 * Finds a research group by its abbreviation.
	 *
	 * @param abbreviation the abbreviation of the research group
	 * @return the found research group
	 */
	public ResearchGroup findByAbbreviation(String abbreviation) {
		return researchGroupRepository.findByAbbreviation(abbreviation);
	}

	/**
	 * Finds a research group by its ID, optionally bypassing access control checks.
	 *
	 * @param researchGroupId  the unique identifier of the research group
	 * @param noAuthentication whether to skip access control checks
	 * @return the found research group
	 */
	public ResearchGroup findById(UUID researchGroupId, boolean noAuthentication) {
		ResearchGroup researchGroup = researchGroupRepository.findById(researchGroupId)
				.orElseThrow(() -> new ResourceNotFoundException(
						String.format("Research Group with id %s not found.", researchGroupId)));
		if (!noAuthentication) {
			currentUserProvider().assertCanAccessResearchGroup(researchGroup);
		}
		return researchGroup;
	}

	// TODO: we should avoid using @Transactional because it can lead to performance issue and concurrency problems
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

		accessManagementService.assignSupervisorRole(head);
		accessManagementService.assignGroupAdminRole(head);

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

		ResearchGroup savedResearchGroup = researchGroupRepository.save(researchGroup);

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
			newUser.setMatriculationNumber(userElement.getMatriculationNumber());

			return userRepository.save(newUser);
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
		if (researchGroup.isArchived()) {
			throw new AccessDeniedException("Cannot update an archived research group.");
		}
		//If user has group-admin rights he still needs to be part of the specific research group
		currentUserProvider().assertCanAccessResearchGroup(researchGroup);

		User oldHead = researchGroup.getHead();
		//Get the User by universityId else create the user
		User head = getUserByUsernameOrCreate(headUsername);

		//Update head only on change
		if (!oldHead.getId().equals(head.getId())) {
			if (head.getResearchGroup() != null) {
				throw new AccessDeniedException("User is already assigned to a research group.");
			}

			//Remove ResearchGroup from old head and set it to the new head
			oldHead.setResearchGroup(null);
			researchGroup.setHead(head);

			//Give new head supervisor as role and remove the role from the old head
			accessManagementService.assignSupervisorRole(head);
			accessManagementService.assignGroupAdminRole(head);
			accessManagementService.removeResearchGroupRoles(oldHead);
		}

		researchGroup.setName(name);
		researchGroup.setAbbreviation(abbreviation);
		researchGroup.setDescription(description);
		researchGroup.setWebsiteUrl(websiteUrl);
		researchGroup.setCampus(campus);
		researchGroup.setUpdatedAt(Instant.now());
		researchGroup.setUpdatedBy(currentUserProvider().getUser());

		ResearchGroup savedResearchGroup = researchGroupRepository.save(researchGroup);

		head.setResearchGroup(savedResearchGroup);
		userRepository.save(oldHead);
		userRepository.save(head);

		return savedResearchGroup;
	}

	/**
	 * Returns a paginated list of members belonging to the specified research group.
	 *
	 * @param researchGroupId the unique identifier of the research group
	 * @param page            the page number for pagination
	 * @param limit           the number of items per page
	 * @param sortBy          the field to sort by
	 * @param sortOrder       the sort direction (asc or desc)
	 * @return the paginated list of research group members
	 */
	public Page<User> getAllResearchGroupMembers(UUID researchGroupId, Integer page, Integer limit, String sortBy, String sortOrder) {
		ResearchGroup researchGroup = findById(researchGroupId);
		currentUserProvider().assertCanAccessResearchGroup(researchGroup);

		Sort.Order order = new Sort.Order(sortOrder.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC,
				HibernateHelper.validateSortField(User.class, sortBy));

		return userRepository
				.searchUsers(researchGroupId, null, null, PageRequest.of(page, limit, Sort.by(order)));
	}

	/**
	 * Archives the given research group, preventing further modifications to it.
	 *
	 * @param researchGroup the research group to archive
	 */
	public void archiveResearchGroup(ResearchGroup researchGroup) {
		currentUserProvider().assertCanAccessResearchGroup(researchGroup);
		researchGroup.setUpdatedAt(Instant.now());
		researchGroup.setUpdatedBy(currentUserProvider().getUser());
		researchGroup.setArchived(true);

		researchGroupRepository.save(researchGroup);
	}

	/**
	 * Assigns a user to a research group and grants them the advisor role.
	 *
	 * @param username        the username of the user to assign
	 * @param researchGroupId the unique identifier of the research group
	 * @return the assigned user
	 */
	public User assignUserToResearchGroup(String username, UUID researchGroupId) {

		ResearchGroup researchGroup = findById(researchGroupId);
		//If user has group-admin rights he still needs to be part of the specific research group
		currentUserProvider().assertCanAccessResearchGroup(researchGroup);

		User user = getUserByUsernameOrCreate(username);

		if (user.getResearchGroup() != null) {
			throw new AccessDeniedException("User is already assigned to a research group.");
		}

		if (researchGroup != null && researchGroup.isArchived()) {
			throw new AccessDeniedException("Cannot assign user to an archived research group.");
		}

		user.setResearchGroup(researchGroup);

		accessManagementService.assignAdvisorRole(user);

		userRepository.save(user);
		return user;
	}

	/**
	 * Removes a user from a research group and revokes their research group roles.
	 *
	 * @param userId          the unique identifier of the user to remove
	 * @param researchGroupId the unique identifier of the research group
	 * @return the removed user
	 */
	public User removeUserFromResearchGroup(UUID userId, UUID researchGroupId) {
		User user = userService.findById(userId);

		ResearchGroup researchGroup = findById(researchGroupId);
		currentUserProvider().assertCanAccessResearchGroup(researchGroup);

		if (user.getResearchGroup() == null || !user.getResearchGroup().getId().equals(researchGroupId)) {
			throw new AccessDeniedException("User is not assigned to this research group.");
		}
		if (user.getResearchGroup().isArchived()) {
			throw new AccessDeniedException("Cannot remove user from an archived research group.");
		}
		if (user.getResearchGroup().getHead() == user) {
			throw new AccessDeniedException("Cannot remove the head of the research group.");
		}

		accessManagementService.removeResearchGroupRoles(user);
		user.setResearchGroup(null);

		userRepository.save(user);

		return user;
	}

	/**
	 * Updates the role of a research group member to the specified advisor or supervisor role.
	 *
	 * @param researchGroupId the unique identifier of the research group
	 * @param userId          the unique identifier of the member
	 * @param role            the new role to assign (advisor or supervisor)
	 * @return the updated user
	 */
	public User updateResearchGroupMemberRole(
			UUID researchGroupId,
			UUID userId,
			String role
	) {
		User user = userService.findById(userId);
		ResearchGroup researchGroup = findById(researchGroupId);
		currentUserProvider().assertCanAccessResearchGroup(researchGroup);

		if (!user.getResearchGroup().getId().equals(researchGroup.getId())) {
			throw new AccessDeniedException("User is not assigned to this research group.");
		}

		if (user.getResearchGroup().isArchived()) {
			throw new AccessDeniedException("Cannot update role of a user in an archived research group.");
		}

		if ("advisor".equalsIgnoreCase(role)) {
			accessManagementService.assignAdvisorRole(user);
		} else if ("supervisor".equalsIgnoreCase(role)) {
			accessManagementService.assignSupervisorRole(user);
		} else {
			throw new IllegalArgumentException("Invalid role: " + role);
		}

		userRepository.save(user);

		return user;
	}

	/**
	 * Toggles the group-admin role for the specified user in the research group.
	 *
	 * @param researchGroupId the unique identifier of the research group
	 * @param userId          the unique identifier of the user
	 * @return the updated user
	 */
	public User changeResearchGroupAdminRole(UUID researchGroupId, UUID userId) {
		User user = userService.findById(userId);

		ResearchGroup researchGroup = findById(researchGroupId);
		//If the user has group-admin rights he still needs to be part of the specific research group
		currentUserProvider().assertCanAccessResearchGroup(researchGroup);

		if (user.getResearchGroup() == null || !user.getResearchGroup().getId().equals(researchGroupId)) {
			throw new AccessDeniedException("User is not a member of this research group.");
		}

		if (user.hasAnyGroup("group-admin")) {
			accessManagementService.removeGroupAdminRole(user);
		} else {
			accessManagementService.assignGroupAdminRole(user);
		}

		return user;
	}
}
