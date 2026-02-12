package de.tum.cit.aet.thesis.service;

import de.tum.cit.aet.thesis.entity.ResearchGroup;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.exception.request.ResourceNotFoundException;
import de.tum.cit.aet.thesis.repository.UserRepository;
import de.tum.cit.aet.thesis.security.CurrentUserProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Provides user lookup, search, and document retrieval operations. */
@Service
public class UserService {
	private final UserRepository userRepository;
	private final UploadService uploadService;
	private final ObjectProvider<CurrentUserProvider> currentUserProviderProvider;

	/**
	 * Injects the user repository, upload service, and current user provider.
	 *
	 * @param userRepository the user repository
	 * @param uploadService the upload service
	 * @param currentUserProviderProvider the current user provider
	 */
	@Autowired
	public UserService(UserRepository userRepository, UploadService uploadService,
		ObjectProvider<CurrentUserProvider> currentUserProviderProvider) {
		this.userRepository = userRepository;
		this.uploadService = uploadService;
		this.currentUserProviderProvider = currentUserProviderProvider;
	}

	private CurrentUserProvider currentUserProvider() {
		return currentUserProviderProvider.getObject();
	}

	/**
	 * Returns a paginated and filtered list of users within the current user's research group.
	 *
	 * @param searchQuery the search query to filter users
	 * @param groups the user groups to filter by
	 * @param page the page number
	 * @param limit the number of items per page
	 * @param sortBy the field to sort by
	 * @param sortOrder the sort direction (asc or desc)
	 * @return a page of users matching the filters
	 */
	public Page<User> getAll(String searchQuery, String[] groups, Integer page, Integer limit, String sortBy, String sortOrder) {
		Sort.Order order = new Sort.Order(sortOrder.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);

		ResearchGroup researchGroup = currentUserProvider().getResearchGroupOrThrow();
		String searchQueryFilter = searchQuery == null || searchQuery.isEmpty() ? null : searchQuery.toLowerCase();
		Set<String> groupsFilter = groups == null || groups.length == 0 ? null : new HashSet<>(Arrays.asList(groups));

		return userRepository
				.searchUsers(researchGroup == null ? null : researchGroup.getId(),searchQueryFilter, groupsFilter, PageRequest.of(page, limit, Sort.by(order)));
	}

	/**
	 * Loads and returns the examination report file for the given user.
	 *
	 * @param user the user whose examination report to retrieve
	 * @return the examination report file resource
	 */
	public Resource getExaminationReport(User user) {
		return uploadService.load(user.getExaminationFilename());
	}

	/**
	 * Loads and returns the CV file for the given user.
	 *
	 * @param user the user whose CV to retrieve
	 * @return the CV file resource
	 */
	public Resource getCV(User user) {
		return uploadService.load(user.getCvFilename());
	}

	/**
	 * Loads and returns the degree report file for the given user.
	 *
	 * @param user the user whose degree report to retrieve
	 * @return the degree report file resource
	 */
	public Resource getDegreeReport(User user) {
		return uploadService.load(user.getDegreeFilename());
	}

	/**
	 * Finds a user by their ID or throws a ResourceNotFoundException if not found.
	 *
	 * @param userId the user ID
	 * @return the user
	 */
	public User findById(UUID userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException(String.format("User with id %s not found.", userId)));
	}

	/**
	 * Finds a user by their university ID or throws a ResourceNotFoundException if not found.
	 *
	 * @param universityId the university ID
	 * @return the user
	 */
	public User findByUniversityId(String universityId) {
		return userRepository.findByUniversityId(universityId)
				.orElseThrow(() -> new ResourceNotFoundException(String.format("User with universityId %s not found.", universityId)));
	}

	/**
	 * Returns all users matching the given list of university IDs.
	 *
	 * @param universityIds the list of university IDs to search for
	 * @return the list of matching users
	 */
	public List<User> findAllByUniversityIdIn(List<String> universityIds) {
		return userRepository.findAllByUniversityIdIn(universityIds);
	}
}
