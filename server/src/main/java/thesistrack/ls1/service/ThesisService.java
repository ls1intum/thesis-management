package thesistrack.ls1.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import thesistrack.ls1.constants.ThesisRoleName;
import thesistrack.ls1.constants.ThesisState;
import thesistrack.ls1.constants.ThesisVisibility;
import thesistrack.ls1.controller.payload.ThesisStatePayload;
import thesistrack.ls1.controller.payload.UpdateThesisPayload;
import thesistrack.ls1.entity.*;
import thesistrack.ls1.entity.key.ThesisRoleId;
import thesistrack.ls1.entity.key.ThesisStateChangeId;
import thesistrack.ls1.exception.request.ResourceInvalidParametersException;
import thesistrack.ls1.exception.request.ResourceNotFoundException;
import thesistrack.ls1.repository.ThesisRepository;
import thesistrack.ls1.repository.ThesisRoleRepository;
import thesistrack.ls1.repository.ThesisStateChangeRepository;
import thesistrack.ls1.repository.UserRepository;
import thesistrack.ls1.utility.RequestValidator;

import java.time.Instant;
import java.util.*;

@Service
public class ThesisService {
    private final ThesisRoleRepository thesisRoleRepository;
    private final ThesisRepository thesisRepository;
    private final ThesisStateChangeRepository thesisStateChangeRepository;
    private final UserRepository userRepository;

    @Autowired
    public ThesisService(ThesisRoleRepository thesisRoleRepository, ThesisRepository thesisRepository, ThesisStateChangeRepository thesisStateChangeRepository, UserRepository userRepository) {
        this.thesisRoleRepository = thesisRoleRepository;
        this.thesisRepository = thesisRepository;
        this.thesisStateChangeRepository = thesisStateChangeRepository;
        this.userRepository = userRepository;
    }

    public Page<Thesis> getAll(
            UUID userId,
            String searchQuery,
            ThesisState[] states,
            int page,
            int limit,
            String sortBy,
            String sortOrder
    ) {
        Sort.Order order = new Sort.Order(sortOrder.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);

        String searchQueryFilter = searchQuery == null || searchQuery.isEmpty() ? null : searchQuery.toLowerCase();
        Set<ThesisState> statesFilter = states == null || states.length == 0 ? null : new HashSet<>(Arrays.asList(states));

        return thesisRepository.searchTheses(
                userId,
                searchQueryFilter,
                statesFilter,
                PageRequest.of(page, limit, Sort.by(order))
        );
    }

    @Transactional
    public Thesis createThesis(
            User creator,
            String title,
            Set<UUID> supervisorIds,
            Set<UUID> advisorIds,
            Set<UUID> studentIds,
            Application application
    ) {
        Thesis thesis = new Thesis();

        thesis.setTitle(RequestValidator.validateStringMaxLength(title, 500));
        thesis.setInfo("");
        thesis.setAbstractField("");
        thesis.setState(ThesisState.PROPOSAL);
        thesis.setApplication(application);
        thesis.setCreatedAt(Instant.now());
        thesis.setVisibility(ThesisVisibility.INTERNAL);

        thesis = thesisRepository.save(thesis);

        assignThesisRoles(thesis, creator, supervisorIds, advisorIds, studentIds);

        ThesisStateChangeId stateChangeId = new ThesisStateChangeId();
        stateChangeId.setState(ThesisState.PROPOSAL);
        stateChangeId.setThesisId(thesis.getId());

        ThesisStateChange stateChange = new ThesisStateChange();
        stateChange.setChangedAt(Instant.now());
        stateChange.setThesis(thesis);
        stateChange.setId(stateChangeId);

        thesisStateChangeRepository.save(stateChange);

        return findById(thesis.getId());
    }

    @Transactional
    public Thesis closeThesis(UUID thesisId) {
        Thesis thesis = findById(thesisId);

        if (thesis.getState() == ThesisState.DROPPED_OUT || thesis.getState() == ThesisState.FINISHED) {
            throw new ResourceInvalidParametersException("Thesis is already completed");
        }

        thesis.setState(ThesisState.DROPPED_OUT);

        ThesisStateChangeId stateChangeId = new ThesisStateChangeId();
        stateChangeId.setThesisId(thesis.getId());
        stateChangeId.setState(ThesisState.DROPPED_OUT);

        ThesisStateChange stateChange = new ThesisStateChange();
        stateChange.setId(stateChangeId);
        stateChange.setThesis(thesis);
        stateChange.setChangedAt(Instant.now());

        thesisStateChangeRepository.save(stateChange);

        return thesisRepository.save(thesis);
    }

    @Transactional
    public Thesis updateThesis(
            User updater,
            UUID thesisId,
            String thesisTitle,
            ThesisVisibility visibility,
            Instant startDate,
            Instant endDate,
            Set<UUID> studentIds,
            Set<UUID> advisorIds,
            Set<UUID> supervisorIds,
            List<ThesisStatePayload> states
    ) {
        Thesis thesis = findById(thesisId);

        thesis.setTitle(RequestValidator.validateStringMaxLength(thesisTitle, 500));
        thesis.setVisibility(visibility);

        if ((startDate == null && endDate != null) || (startDate != null && endDate == null)) {
            throw new ResourceInvalidParametersException("Both start and end date must be provided.");
        }

        thesis.setStartDate(startDate);
        thesis.setEndDate(endDate);

        assignThesisRoles(thesis, updater, supervisorIds, advisorIds, studentIds);

        for (ThesisStatePayload state : states) {
            ThesisStateChangeId stateChangeId = new ThesisStateChangeId();
            stateChangeId.setThesisId(thesis.getId());
            stateChangeId.setState(state.state());

            ThesisStateChange stateChange = new ThesisStateChange();
            stateChange.setId(stateChangeId);
            stateChange.setThesis(thesis);
            stateChange.setChangedAt(state.changedAt());

            thesisStateChangeRepository.save(stateChange);
        }

        return thesisRepository.save(thesis);
    }

    public Thesis findById(UUID thesisId) {
        return thesisRepository.findById(thesisId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Thesis with id %s not found.", thesisId)));
    }

    private void assignThesisRoles(
            Thesis thesis,
            User assigner,
            Set<UUID> supervisorIds,
            Set<UUID> advisorIds,
            Set<UUID> studentIds
    ) {
        List<User> supervisors = userRepository.findAllById(supervisorIds);
        List<User> advisors = userRepository.findAllById(advisorIds);
        List<User> students = userRepository.findAllById(studentIds);

        if (supervisors.isEmpty() || supervisors.size() != supervisorIds.size()) {
            throw new ResourceInvalidParametersException("No supervisors selected or supervisors not found");
        }

        if (advisors.isEmpty() || advisors.size() != advisorIds.size()) {
            throw new ResourceInvalidParametersException("No advisors selected or advisors not found");
        }

        if (students.isEmpty() || students.size() != studentIds.size()) {
            throw new ResourceInvalidParametersException("No students selected or students not found");
        }

        for (User supervisor : supervisors) {
            if (!supervisor.hasAnyGroup("supervisor")) {
                throw new ResourceInvalidParametersException("User is not a supervisor");
            }

            saveThesisRole(thesis, assigner, supervisor, ThesisRoleName.SUPERVISOR);
        }

        for (User advisor : advisors) {
            if (!advisor.hasAnyGroup("advisor", "supervisor")) {
                throw new ResourceInvalidParametersException("User is not an advisor");
            }

            saveThesisRole(thesis, assigner, advisor, ThesisRoleName.ADVISOR);
        }

        for (User student : students) {
            saveThesisRole(thesis, assigner, student, ThesisRoleName.STUDENT);
        }
    }

    private void saveThesisRole(Thesis thesis, User assigner, User user, ThesisRoleName role) {
        if (assigner == null || user == null) {
            throw new ResourceInvalidParametersException("Assigner and user must be provided.");
        }

        ThesisRole thesisRole = new ThesisRole();
        ThesisRoleId thesisRoleId = new ThesisRoleId();

        thesisRoleId.setThesisId(thesis.getId());
        thesisRoleId.setUserId(user.getId());
        thesisRoleId.setRole(role);

        thesisRole.setId(thesisRoleId);
        thesisRole.setUser(user);
        thesisRole.setAssignedBy(assigner);
        thesisRole.setAssignedAt(Instant.now());
        thesisRole.setThesis(thesis);

        thesisRoleRepository.save(thesisRole);
    }
}
