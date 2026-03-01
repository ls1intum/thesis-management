package de.tum.cit.aet.thesis.service;

import de.tum.cit.aet.thesis.constants.ThesisRoleName;
import de.tum.cit.aet.thesis.constants.TopicState;
import de.tum.cit.aet.thesis.dto.TopicInterviewProcessDto;
import de.tum.cit.aet.thesis.entity.ResearchGroup;
import de.tum.cit.aet.thesis.entity.Topic;
import de.tum.cit.aet.thesis.entity.TopicRole;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.entity.key.TopicRoleId;
import de.tum.cit.aet.thesis.exception.request.ResourceInvalidParametersException;
import de.tum.cit.aet.thesis.exception.request.ResourceNotFoundException;
import de.tum.cit.aet.thesis.repository.InterviewProcessRepository;
import de.tum.cit.aet.thesis.repository.ResearchGroupRepository;
import de.tum.cit.aet.thesis.repository.TopicRepository;
import de.tum.cit.aet.thesis.repository.TopicRoleRepository;
import de.tum.cit.aet.thesis.repository.UserRepository;
import de.tum.cit.aet.thesis.security.CurrentUserProvider;
import de.tum.cit.aet.thesis.utility.HibernateHelper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Manages thesis topics, including creation, updates, role assignments, and search. */
@Service
public class TopicService {
	private final TopicRepository topicRepository;
	private final TopicRoleRepository topicRoleRepository;
	private final UserRepository userRepository;
	private final ObjectProvider<CurrentUserProvider> currentUserProviderProvider;
	private final ResearchGroupRepository researchGroupRepository;
	private final InterviewProcessRepository interviewProcessRepository;

	/**
	 * Injects the topic, user, and research group repositories along with the current user provider.
	 *
	 * @param topicRepository the topic repository
	 * @param topicRoleRepository the topic role repository
	 * @param userRepository the user repository
	 * @param currentUserProviderProvider the current user provider
	 * @param researchGroupRepository the research group repository
	 * @param interviewProcessRepository the interview process repository
	 */
	@Autowired
	public TopicService(
			TopicRepository topicRepository,
			TopicRoleRepository topicRoleRepository,
			UserRepository userRepository,
			ObjectProvider<CurrentUserProvider> currentUserProviderProvider,
			ResearchGroupRepository researchGroupRepository,
			InterviewProcessRepository interviewProcessRepository) {
		this.topicRepository = topicRepository;
		this.topicRoleRepository = topicRoleRepository;
		this.userRepository = userRepository;
	this.currentUserProviderProvider = currentUserProviderProvider;
		this.researchGroupRepository = researchGroupRepository;
		this.interviewProcessRepository = interviewProcessRepository;
	}

	private CurrentUserProvider currentUserProvider() {
		return currentUserProviderProvider.getObject();
	}

	/**
	 * Returns a paginated and filtered list of topics based on type, state, and research group.
	 *
	 * @param onlyOwnResearchGroup whether to filter by the current user's research group
	 * @param types the topic types to filter by
	 * @param states the topic states to filter by
	 * @param searchQuery the search query string
	 * @param page the page number
	 * @param limit the number of items per page
	 * @param sortBy the field to sort by
	 * @param sortOrder the sort direction (asc or desc)
	 * @param researchGroupIds the research group IDs to filter by
	 * @return a page of topics matching the filters
	 */
	public Page<Topic> getAll(
			boolean onlyOwnResearchGroup,
			String[] types,
			String[] states,
			String searchQuery,
			int page,
			int limit,
			String sortBy,
			String sortOrder,
			UUID[] researchGroupIds
	) {
		Sort.Order order = new Sort.Order(
				sortOrder.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC,
				HibernateHelper.getColumnName(Topic.class, sortBy)
		);

		ResearchGroup researchGroup = onlyOwnResearchGroup ?
			currentUserProvider().getResearchGroupOrThrow() : null;
		String searchQueryFilter = searchQuery == null || searchQuery.isEmpty() ? null : searchQuery.toLowerCase();
		String[] typesFilter = types == null || types.length == 0 ? null : types;


		if (states != null && Arrays.stream(states).anyMatch(s -> s.equals(TopicState.CLOSED.name()) || s.equals(TopicState.DRAFT.name()))) {
			currentUserProvider().assertCanAccessResearchGroup(researchGroup);
		}
		String[] statesFilter = (states != null && states.length > 0) ? states : new String[] { TopicState.OPEN.name() };

		return topicRepository.searchTopics(
				researchGroup == null
						? (researchGroupIds == null ? null
								: new HashSet<>(Arrays.asList(researchGroupIds)))
						: new HashSet<UUID>(Collections.singleton(researchGroup.getId())),
				typesFilter,
				statesFilter,
				searchQueryFilter,
				PageRequest.of(page, limit, Sort.by(order))
		);
	}

	/**
	 * Returns open topics eligible for interview processes for the current user's research group.
	 *
	 * @param searchQuery the search query to filter topics
	 * @param page the page number
	 * @param limit the number of items per page
	 * @param excludeSupervised whether to exclude topics supervised by the current user
	 * @return a page of topics with interview process information
	 */
	public Page<TopicInterviewProcessDto> getPossibleInterviewTopics(
			String searchQuery,
			int page,
			int limit,
			boolean excludeSupervised
	) {
		if (currentUserProvider().getResearchGroupOrThrow().getId() == null && !currentUserProvider().isAdmin()) {
			throw new IllegalStateException("Current user is not assigned to any research group.");
		}

		UUID userId = currentUserProvider().isAdmin() ?
				null : currentUserProvider().getUser().getId();

		Sort.Order order = new Sort.Order(
				Sort.Direction.DESC,
				HibernateHelper.getColumnName(Topic.class, "title")
		);

		Page<Topic> topics = topicRepository.findOpenTopicsForUserByRoles(searchQuery, userId, excludeSupervised, PageRequest.of(page, limit <= 0 ? Integer.MAX_VALUE : limit, Sort.by(order)));

		return topics.map(topic ->
			TopicInterviewProcessDto.from(topic, interviewProcessRepository.existsByTopicId(topic.getId()))
		);
	}

	/**
	 * Returns all open topics belonging to the specified research group.
	 *
	 * @param researchGroupId the research group ID
	 * @return the list of open topics for the research group
	 */
	public List<Topic> getOpenFromResearchGroup(UUID researchGroupId) {
		return topicRepository.searchTopics(
				Collections.singleton(researchGroupId),
				null,
				new String[] { TopicState.OPEN.name() },
				null,
				PageRequest.of(0, Integer.MAX_VALUE, Sort.unsorted())
		).toList();
	}

	// TODO: we should avoid using @Transactional because it can lead to performance issue and concurrency problems
	@Transactional
	public Topic createTopic(
			String title,
			Set<String> thesisTypes,
			String problemStatement,
			String requirements,
			String goals,
			String references,
			List<UUID> supervisorIds,
			List<UUID> advisorIds,
			UUID researchGroupId,
			Instant intendedStart,
			Instant applicationDeadline,
			Boolean isDraft
	) {
		User creator = currentUserProvider().getUser();
		ResearchGroup researchGroup = researchGroupRepository.findById(researchGroupId).orElseThrow(
				() -> new ResourceNotFoundException("Research group not found")
		);
		Topic topic = new Topic();

		topic.setTitle(title);
		topic.setThesisTypes(thesisTypes);
		topic.setProblemStatement(problemStatement);
		topic.setRequirements(requirements);
		topic.setGoals(goals);
		topic.setReferences(references);
		topic.setUpdatedAt(Instant.now());
		topic.setCreatedAt(Instant.now());
		if (!isDraft) {
			topic.setPublishedAt(Instant.now());
		}
		topic.setCreatedBy(creator);
		topic.setResearchGroup(researchGroup);
		topic.setIntendedStart(intendedStart);
		topic.setApplicationDeadline(applicationDeadline);

		topic = topicRepository.save(topic);

		assignTopicRoles(topic, advisorIds, supervisorIds);

		return topicRepository.save(topic);
	}

	// TODO: we should avoid using @Transactional because it can lead to performance issue and concurrency problems
	@Transactional
	public Topic updateTopic(
			Topic topic,
			String title,
			Set<String> thesisTypes,
			String problemStatement,
			String requirements,
			String goals,
			String references,
			List<UUID> supervisorIds,
			List<UUID> advisorIds,
			UUID researchGroupId,
			Instant intendedStart,
			Instant applicationDeadline,
			Boolean isDraft
	) {
		ResearchGroup researchGroup = researchGroupRepository.findById(researchGroupId).orElseThrow(
				() -> new ResourceNotFoundException("Research group not found")
		);
		currentUserProvider().assertCanAccessResearchGroup(researchGroup);
		topic.setTitle(title);
		topic.setThesisTypes(thesisTypes);
		topic.setProblemStatement(problemStatement);
		topic.setRequirements(requirements);
		topic.setGoals(goals);
		topic.setReferences(references);
		topic.setUpdatedAt(Instant.now());
		if (!isDraft && topic.getPublishedAt() == null) {
			topic.setPublishedAt(Instant.now());
		}
		topic.setResearchGroup(researchGroup);
		topic.setIntendedStart(intendedStart);
		topic.setApplicationDeadline(applicationDeadline);

		assignTopicRoles(topic, advisorIds, supervisorIds);

		return topicRepository.save(topic);
	}

	/**
	 * Finds a topic by its ID or throws a ResourceNotFoundException if not found.
	 *
	 * @param topicId the topic ID
	 * @return the topic
	 */
	public Topic findById(UUID topicId) {
		return topicRepository.findById(topicId)
				.orElseThrow(() -> new ResourceNotFoundException(String.format("Topic with id %s not found.", topicId)));
	}

	private void assignTopicRoles(Topic topic, List<UUID> advisorIds, List<UUID> supervisorIds) {
		currentUserProvider().assertCanAccessResearchGroup(topic.getResearchGroup());
		List<User> supervisors = userRepository.findAllById(supervisorIds);
		List<User> advisors = userRepository.findAllById(advisorIds);

		supervisors.sort(Comparator.comparing(user -> supervisorIds.indexOf(user.getId())));
		advisors.sort(Comparator.comparing(user -> advisorIds.indexOf(user.getId())));

		if (supervisors.isEmpty() || supervisors.size() != supervisorIds.size()) {
			throw new ResourceInvalidParametersException("No supervisors selected or supervisors not found");
		}

		if (advisors.isEmpty() || advisors.size() != advisorIds.size()) {
			throw new ResourceInvalidParametersException("No advisors selected or advisors not found");
		}

		topicRoleRepository.deleteByTopicId(topic.getId());
		topic.setRoles(new ArrayList<>());

		for (int i = 0; i < supervisors.size(); i++) {
			User supervisor = supervisors.get(i);

			if (!supervisor.hasAnyGroup("supervisor")) {
				throw new ResourceInvalidParametersException("User is not a supervisor");
			}

			saveTopicRole(topic, supervisor, ThesisRoleName.SUPERVISOR, i);
		}

		for (int i = 0; i < advisors.size(); i++) {
			User advisor = advisors.get(i);

			if (!advisor.hasAnyGroup("advisor", "supervisor")) {
				throw new ResourceInvalidParametersException("User is not an advisor");
			}

			saveTopicRole(topic, advisor, ThesisRoleName.ADVISOR, i);
		}
	}

	private void saveTopicRole(Topic topic, User user, ThesisRoleName role, int position) {
		currentUserProvider().assertCanAccessResearchGroup(topic.getResearchGroup());
		User assigner = currentUserProvider().getUser();
		if (assigner == null || user == null) {
			throw new ResourceInvalidParametersException("Assigner and user must be provided.");
		}

		TopicRole topicRole = new TopicRole();
		TopicRoleId topicRoleId = new TopicRoleId();

		topicRoleId.setTopicId(topic.getId());
		topicRoleId.setUserId(user.getId());
		topicRoleId.setRole(role);

		topicRole.setId(topicRoleId);
		topicRole.setUser(user);
		topicRole.setAssignedBy(assigner);
		topicRole.setAssignedAt(Instant.now());
		topicRole.setTopic(topic);
		topicRole.setPosition(position);

		topicRoleRepository.save(topicRole);

		List<TopicRole> roles = topic.getRoles();

		roles.add(topicRole);
		roles.sort(Comparator.comparingInt(TopicRole::getPosition));

		topic.setRoles(roles);
	}
}
