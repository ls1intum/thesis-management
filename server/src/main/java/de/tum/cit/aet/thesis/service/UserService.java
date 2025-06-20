package de.tum.cit.aet.thesis.service;

import de.tum.cit.aet.thesis.entity.ResearchGroup;
import de.tum.cit.aet.thesis.security.CurrentUserProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.exception.request.ResourceNotFoundException;
import de.tum.cit.aet.thesis.repository.UserRepository;

import java.util.*;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final UploadService uploadService;
    private final ObjectProvider<CurrentUserProvider> currentUserProviderProvider;

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

    public Page<User> getAll(String searchQuery, String[] groups, Integer page, Integer limit, String sortBy, String sortOrder) {
        Sort.Order order = new Sort.Order(sortOrder.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);

        ResearchGroup researchGroup = currentUserProvider().getResearchGroupOrThrow();
        String searchQueryFilter = searchQuery == null || searchQuery.isEmpty() ? null : searchQuery.toLowerCase();
        Set<String> groupsFilter = groups == null || groups.length == 0 ? null : new HashSet<>(Arrays.asList(groups));

        return userRepository
                .searchUsers(researchGroup == null ? null : researchGroup.getId(),searchQueryFilter, groupsFilter, PageRequest.of(page, limit, Sort.by(order)));
    }

    public Resource getExaminationReport(User user) {
        return uploadService.load(user.getExaminationFilename());
    }

    public Resource getCV(User user) {
        return uploadService.load(user.getCvFilename());
    }

    public Resource getDegreeReport(User user) {
        return uploadService.load(user.getDegreeFilename());
    }

    public User findById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("User with id %s not found.", userId)));
    }

    public User findByUniversityId(String universityId) {
        return userRepository.findByUniversityId(universityId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("User with universityId %s not found.", universityId)));
    }

    public List<User> findAllByUniversityIdIn(List<String> universityIds) {
        return userRepository.findAllByUniversityIdIn(universityIds);
    }
}
