package de.tum.cit.aet.thesis.service;

import de.tum.cit.aet.thesis.constants.*;
import de.tum.cit.aet.thesis.controller.payload.RequestChangesPayload;
import de.tum.cit.aet.thesis.controller.payload.ThesisStatePayload;
import de.tum.cit.aet.thesis.entity.*;
import de.tum.cit.aet.thesis.entity.jsonb.ThesisMetadata;
import de.tum.cit.aet.thesis.entity.key.ThesisRoleId;
import de.tum.cit.aet.thesis.entity.key.ThesisStateChangeId;
import de.tum.cit.aet.thesis.exception.request.ResourceInvalidParametersException;
import de.tum.cit.aet.thesis.exception.request.ResourceNotFoundException;
import de.tum.cit.aet.thesis.repository.*;
import de.tum.cit.aet.thesis.security.CurrentUserProvider;
import de.tum.cit.aet.thesis.utility.DataFormatter;
import de.tum.cit.aet.thesis.utility.PDFBuilder;
import de.tum.cit.aet.thesis.utility.RequestValidator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ThesisService {
    private final ThesisRoleRepository thesisRoleRepository;
    private final ThesisRepository thesisRepository;
    private final ThesisStateChangeRepository thesisStateChangeRepository;
    private final UserRepository userRepository;
    private final UploadService uploadService;
    private final ThesisProposalRepository thesisProposalRepository;
    private final ThesisAssessmentRepository thesisAssessmentRepository;
    private final MailingService mailingService;
    private final AccessManagementService accessManagementService;
    private final ThesisPresentationService thesisPresentationService;
    private final ThesisFeedbackRepository thesisFeedbackRepository;
    private final ThesisFileRepository thesisFileRepository;
    private final ObjectProvider<CurrentUserProvider> currentUserProviderProvider;
    private final ResearchGroupRepository researchGroupRepository;

    @Autowired
    public ThesisService(
            ThesisRoleRepository thesisRoleRepository,
            ThesisRepository thesisRepository,
            ThesisStateChangeRepository thesisStateChangeRepository,
            UserRepository userRepository,
            ThesisProposalRepository thesisProposalRepository,
            ThesisAssessmentRepository thesisAssessmentRepository,
            UploadService uploadService,
            MailingService mailingService,
            AccessManagementService accessManagementService,
            ThesisPresentationService thesisPresentationService,
            ThesisFeedbackRepository thesisFeedbackRepository, ThesisFileRepository thesisFileRepository,
            ObjectProvider<CurrentUserProvider> currentUserProviderProvider, ResearchGroupRepository researchGroupRepository
    ) {
        this.thesisRoleRepository = thesisRoleRepository;
        this.thesisRepository = thesisRepository;
        this.thesisStateChangeRepository = thesisStateChangeRepository;
        this.userRepository = userRepository;
        this.uploadService = uploadService;
        this.thesisProposalRepository = thesisProposalRepository;
        this.thesisAssessmentRepository = thesisAssessmentRepository;
        this.mailingService = mailingService;
        this.accessManagementService = accessManagementService;
        this.thesisPresentationService = thesisPresentationService;
        this.thesisFeedbackRepository = thesisFeedbackRepository;
        this.thesisFileRepository = thesisFileRepository;
        this.currentUserProviderProvider = currentUserProviderProvider;
        this.researchGroupRepository = researchGroupRepository;
    }

    private CurrentUserProvider currentUserProvider() {
        return currentUserProviderProvider.getObject();
    }

    public Page<Thesis> getAll(
        UUID userId,
        Set<ThesisVisibility> visibilities,
        String searchQuery,
        ThesisState[] states,
        String[] types,
        int page,
        int limit,
        String sortBy,
        String sortOrder
    ) {
        Sort.Order order = new Sort.Order(sortOrder.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);

        ResearchGroup researchGroup = null;
        if (visibilities == null || visibilities.isEmpty() || !visibilities.equals(Set.of(ThesisVisibility.PUBLIC))) {
            researchGroup = currentUserProvider().getResearchGroupOrThrow();
        }
        String searchQueryFilter = searchQuery == null || searchQuery.isEmpty() ? null : searchQuery.toLowerCase();
        Set<ThesisState> statesFilter = states == null || states.length == 0 ? null : new HashSet<>(Arrays.asList(states));
        Set<String> typesFilter = types == null || types.length == 0 ? null : new HashSet<>(Arrays.asList(types));
        Set<String> visibilityFilter = visibilities == null ? null :
                visibilities.stream().map(ThesisVisibility::getValue).collect(Collectors.toSet());

        return thesisRepository.searchTheses(
                researchGroup == null ? null : researchGroup.getId(),
                userId,
                visibilityFilter,
                searchQueryFilter,
                statesFilter,
                typesFilter,
                PageRequest.of(page, limit, Sort.by(order))
        );
    }

    @Transactional
    public Thesis createThesis(
            String thesisTitle,
            String thesisType,
            String language,
            List<UUID> supervisorIds,
            List<UUID> advisorIds,
            List<UUID> studentIds,
            Application application,
            boolean notifyUser,
            UUID researchGroupId
    ) {
        ResearchGroup researchGroup = researchGroupRepository.findById(researchGroupId)
                .orElseThrow(() -> new ResourceNotFoundException("Research group not found"));
        Thesis thesis = new Thesis();

        thesis.setTitle(thesisTitle);
        thesis.setType(thesisType);
        thesis.setLanguage(language);
        thesis.setMetadata(ThesisMetadata.getEmptyMetadata());
        thesis.setVisibility(ThesisVisibility.INTERNAL);
        thesis.setKeywords(new HashSet<>());
        thesis.setInfo("");
        thesis.setAbstractField("");
        thesis.setState(ThesisState.PROPOSAL);
        thesis.setApplication(application);
        thesis.setCreatedAt(Instant.now());
        thesis.setResearchGroup(researchGroup);

        thesis = thesisRepository.save(thesis);

        assignThesisRoles(thesis, supervisorIds, advisorIds, studentIds);
        saveStateChange(thesis, ThesisState.PROPOSAL);

        if (notifyUser) {
            mailingService.sendThesisCreatedEmail(currentUserProvider().getUser(), thesis);
        }

        for (User student : thesis.getStudents()) {
            accessManagementService.addStudentGroup(student);
        }

        return thesis;
    }

    @Transactional
    public Thesis closeThesis(Thesis thesis) {
        currentUserProvider().assertCanAccessResearchGroup(thesis.getResearchGroup());
        if (thesis.getState() == ThesisState.DROPPED_OUT || thesis.getState() == ThesisState.FINISHED) {
            throw new ResourceInvalidParametersException("Thesis is already completed");
        }

        thesis.setState(ThesisState.DROPPED_OUT);
        saveStateChange(thesis, ThesisState.DROPPED_OUT);

        thesis = thesisRepository.save(thesis);

        mailingService.sendThesisClosedEmail(currentUserProvider().getUser(), thesis);

        for (User student : thesis.getStudents()) {
            if (!existsPendingThesis(student)) {
                accessManagementService.removeStudentGroup(student);
            }
        }

        return thesis;
    }

    @Transactional
    public Thesis updateThesis(
            Thesis thesis,
            String thesisTitle,
            String thesisType,
            String language,
            ThesisVisibility visibility,
            Set<String> keywords,
            Instant startDate,
            Instant endDate,
            List<UUID> studentIds,
            List<UUID> advisorIds,
            List<UUID> supervisorIds,
            List<ThesisStatePayload> states,
            UUID researchGroupId
    ) {
        ResearchGroup researchGroup = researchGroupRepository.findById(researchGroupId)
                .orElseThrow(() -> new ResourceNotFoundException("Research group not found"));
        currentUserProvider().assertCanAccessResearchGroup(researchGroup);
        thesis.setTitle(thesisTitle);
        thesis.setType(thesisType);
        thesis.setLanguage(language);
        thesis.setVisibility(visibility);
        thesis.setKeywords(keywords);
        thesis.setResearchGroup(researchGroup);

        if ((startDate == null && endDate != null) || (startDate != null && endDate == null)) {
            throw new ResourceInvalidParametersException("Both start and end date must be provided.");
        }

        thesis.setStartDate(startDate);
        thesis.setEndDate(endDate);

        assignThesisRoles(thesis, supervisorIds, advisorIds, studentIds);

        for (ThesisStatePayload state : states) {
            saveStateChange(thesis, state.state());
        }

        thesis = thesisRepository.save(thesis);

        thesisPresentationService.updateThesisCalendarEvents(thesis);

        return thesis;
    }

    @Transactional
    public Thesis updateThesisInfo(
            Thesis thesis,
            String abstractText,
            String infoText
    ) {
        currentUserProvider().assertCanAccessResearchGroup(thesis.getResearchGroup());
        thesis.setAbstractField(abstractText);
        thesis.setInfo(infoText);

        thesis = thesisRepository.save(thesis);

        thesisPresentationService.updateThesisCalendarEvents(thesis);

        return thesis;
    }

    @Transactional
    public Thesis updateThesisTitles(
            Thesis thesis,
            String primaryTitle,
            Map<String, String> titles
    ) {
        currentUserProvider().assertCanAccessResearchGroup(thesis.getResearchGroup());
        thesis.setMetadata(new ThesisMetadata(
                titles,
                thesis.getMetadata().credits()
        ));
        thesis.setTitle(primaryTitle);

        thesis = thesisRepository.save(thesis);

        thesisPresentationService.updateThesisCalendarEvents(thesis);

        return thesis;
    }

    @Transactional
    public Thesis updateThesisCredits(
            Thesis thesis,
            Map<UUID, Number> credits
    ) {
        currentUserProvider().assertCanAccessResearchGroup(thesis.getResearchGroup());
        thesis.setMetadata(new ThesisMetadata(
                thesis.getMetadata().titles(),
                credits
        ));

        return thesisRepository.save(thesis);
    }

    /* FEEDBACK */
    public Thesis completeFeedback(Thesis thesis, UUID feedbackId, boolean completed) {
        currentUserProvider().assertCanAccessResearchGroup(thesis.getResearchGroup());
        ThesisFeedback feedback = thesis.getFeedbackItem(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback id not found"));

        feedback.setCompletedAt(completed ? Instant.now() : null);

        thesisFeedbackRepository.save(feedback);

        return thesis;
    }

    @Transactional
    public Thesis deleteFeedback(Thesis thesis, UUID feedbackId) {
        currentUserProvider().assertCanAccessResearchGroup(thesis.getResearchGroup());
        thesis.getFeedbackItem(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback id not found"));

        thesisFeedbackRepository.deleteById(feedbackId);

        thesis.setFeedback(new ArrayList<>(
                thesis.getFeedback().stream().filter(feedback -> !feedback.getId().equals(feedbackId)).toList()
        ));

        return thesis;
    }

    @Transactional
    public Thesis requestChanges(Thesis thesis, ThesisFeedbackType type, List<RequestChangesPayload.RequestedChange> requestedChanges) {
        currentUserProvider().assertCanAccessResearchGroup(thesis.getResearchGroup());
        for (var requestedChange : requestedChanges) {
            ThesisFeedback feedback = new ThesisFeedback();

            feedback.setRequestedAt(Instant.now());
            feedback.setRequestedBy(currentUserProvider().getUser());
            feedback.setThesis(thesis);
            feedback.setType(type);
            feedback.setFeedback(RequestValidator.validateStringMaxLength(requestedChange.feedback(), StringLimits.LONGTEXT.getLimit()));
            feedback.setCompletedAt(requestedChange.completed() ? Instant.now() : null);

            feedback = thesisFeedbackRepository.save(feedback);

            thesis.getFeedback().add(feedback);
            thesis.setFeedback(thesis.getFeedback());
        }

        if (type == ThesisFeedbackType.PROPOSAL) {
            mailingService.sendProposalChangeRequestEmail(currentUserProvider().getUser(), thesis);
        }

        return thesis;
    }

    /* PROPOSAL */

    public Resource getProposalFile(ThesisProposal proposal) {
        currentUserProvider().assertCanAccessResearchGroup(proposal.getResearchGroup());
        return uploadService.load(proposal.getProposalFilename());
    }

    @Transactional
    public Thesis uploadProposal(Thesis thesis, MultipartFile proposalFile) {
        currentUserProvider().assertCanAccessResearchGroup(thesis.getResearchGroup());
        ThesisProposal proposal = new ThesisProposal();

        proposal.setThesis(thesis);
        proposal.setProposalFilename(uploadService.store(proposalFile, 20 * 1024 * 1024, UploadFileType.PDF));
        proposal.setCreatedAt(Instant.now());
        proposal.setCreatedBy(currentUserProvider().getUser());

        List<ThesisProposal> proposals = thesis.getProposals() == null ? new ArrayList<>() : thesis.getProposals();
        proposals.addFirst(proposal);

        thesis.setProposals(proposals);

        thesisProposalRepository.save(proposal);

        mailingService.sendProposalUploadedEmail(proposal);

        return thesisRepository.save(thesis);
    }

    @Transactional
    public Thesis deleteProposal(Thesis thesis, UUID proposalId) {
        currentUserProvider().assertCanAccessResearchGroup(thesis.getResearchGroup());
        thesis.getProposalById(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal id not found"));

        thesisProposalRepository.deleteById(proposalId);

        thesis.setProposals(new ArrayList<>(
                thesis.getProposals().stream().filter(proposal -> !proposal.getId().equals(proposalId)).toList()
        ));

        return thesis;
    }

    @Transactional
    public Thesis acceptProposal(Thesis thesis) {
        currentUserProvider().assertCanAccessResearchGroup(thesis.getResearchGroup());
        List<ThesisProposal> proposals = thesis.getProposals();

        if (proposals == null || proposals.isEmpty()) {
            throw new ResourceNotFoundException("No proposal added to thesis yet");
        }

        ThesisProposal proposal = proposals.getFirst();

        proposal.setApprovedAt(Instant.now());
        proposal.setApprovedBy(currentUserProvider().getUser());

        thesisProposalRepository.save(proposal);

        saveStateChange(thesis, ThesisState.WRITING);

        thesis.setState(ThesisState.WRITING);

        mailingService.sendProposalAcceptedEmail(proposal);

        return thesisRepository.save(thesis);
    }

    /* WRITING */

    @Transactional
    public Thesis submitThesis(Thesis thesis) {
        currentUserProvider().assertCanAccessResearchGroup(thesis.getResearchGroup());
        if (thesis.getLatestFile("THESIS").isEmpty()) {
            throw new ResourceInvalidParametersException("Thesis file not uploaded yet");
        }

        thesis.setState(ThesisState.SUBMITTED);

        saveStateChange(thesis, ThesisState.SUBMITTED);

        mailingService.sendFinalSubmissionEmail(thesis);

        return thesisRepository.save(thesis);
    }

    @Transactional
    public Thesis uploadThesisFile(Thesis thesis, String type, MultipartFile file) {
        currentUserProvider().assertCanAccessResearchGroup(thesis.getResearchGroup());
        ThesisFile thesisFile = new ThesisFile();

        thesisFile.setUploadName(file.getOriginalFilename());
        thesisFile.setFilename(uploadService.store(file, 20 * 1024 * 1024, UploadFileType.ANY));
        thesisFile.setUploadedBy(currentUserProvider().getUser());
        thesisFile.setUploadedAt(Instant.now());
        thesisFile.setThesis(thesis);
        thesisFile.setType(type);

        List<ThesisFile> files = thesis.getFiles();
        files.addFirst(thesisFileRepository.save(thesisFile));

        return thesisRepository.save(thesis);
    }

    @Transactional
    public Thesis deleteThesisFile(Thesis thesis, UUID fileId) {
        currentUserProvider().assertCanAccessResearchGroup(thesis.getResearchGroup());
        thesis.getFileById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File id not found"));

        thesisFileRepository.deleteById(fileId);

        thesis.setFiles(new ArrayList<>(
                thesis.getFiles().stream().filter(file -> !file.getId().equals(fileId)).toList()
        ));

        return thesis;
    }

    public Resource getThesisFile(ThesisFile file) {
        currentUserProvider().assertCanAccessResearchGroup(file.getResearchGroup());
        return uploadService.load(file.getFilename());
    }

    /* ASSESSMENT */
    @Transactional
    public Thesis submitAssessment(
            Thesis thesis,
            String summary,
            String positives,
            String negatives,
            String gradeSuggestion
    ) {
        currentUserProvider().assertCanAccessResearchGroup(thesis.getResearchGroup());
        ThesisAssessment assessment = new ThesisAssessment();

        assessment.setThesis(thesis);
        assessment.setCreatedBy(currentUserProvider().getUser());
        assessment.setCreatedAt(Instant.now());
        assessment.setSummary(summary);
        assessment.setPositives(positives);
        assessment.setNegatives(negatives);
        assessment.setGradeSuggestion(gradeSuggestion);

        thesisAssessmentRepository.save(assessment);

        List<ThesisAssessment> assessments = Objects.requireNonNullElse(thesis.getAssessments(), new ArrayList<>());
        assessments.addFirst(assessment);

        thesis.setAssessments(assessments);
        thesis.setState(ThesisState.ASSESSED);

        saveStateChange(thesis, ThesisState.ASSESSED);

        mailingService.sendAssessmentAddedEmail(assessment);

        return thesisRepository.save(thesis);
    }

    public Resource getAssessmentFile(Thesis thesis) {
        currentUserProvider().assertCanAccessResearchGroup(thesis.getResearchGroup());
        ThesisAssessment assessment = thesis.getAssessments().getFirst();
        ThesisPresentation presentation = thesis.getPresentations().getFirst();

        String students = String.join(", ", thesis.getStudents().stream().map(student -> student.getFirstName() + " " + student.getLastName()).toList());
        String advisors = String.join(", ", thesis.getAdvisors().stream().map(advisor -> advisor.getFirstName() + " " + advisor.getLastName()).toList());
        String supervisors = String.join(", ", thesis.getSupervisors().stream().map(supervisor -> supervisor.getFirstName() + " " + supervisor.getLastName()).toList());

        PDFBuilder builder = new PDFBuilder("Assessment of \"" + thesis.getTitle() + "\"");

        builder
                .addData("Thesis Type", DataFormatter.formatConstantName(thesis.getType()))
                .addData("Student", students)
                .addData("Advisor", advisors)
                .addData("Supervisor", supervisors)
                .addData("", "");

        for (var stateChange : thesis.getStates()) {
            if (stateChange.getId().getState() == ThesisState.ASSESSED) {
                builder.addData("Assessment Date", DataFormatter.formatDate(stateChange.getChangedAt()));
            }

            if (stateChange.getId().getState() == ThesisState.SUBMITTED) {
                builder.addData("Submission Date", DataFormatter.formatDate(stateChange.getChangedAt()));
            }
        }

        if (presentation != null) {
            builder.addData("Presentation Date", DataFormatter.formatDate(presentation.getScheduledAt()));
        }

        builder.addSection("Summary", assessment.getSummary())
                .addSection("Strengths", assessment.getPositives())
                .addSection("Weaknesses", assessment.getNegatives())
                .addSection("Grade Suggestion", assessment.getGradeSuggestion());

        return builder.build();
    }

    /* GRADING */
    @Transactional
    public Thesis gradeThesis(Thesis thesis, String finalGrade, String finalFeedback, ThesisVisibility visibility) {
        currentUserProvider().assertCanAccessResearchGroup(thesis.getResearchGroup());
        thesis.setState(ThesisState.GRADED);
        thesis.setVisibility(visibility);
        thesis.setFinalGrade(finalGrade);
        thesis.setFinalFeedback(finalFeedback);

        saveStateChange(thesis, ThesisState.GRADED);

        mailingService.sendFinalGradeEmail(thesis);

        return thesisRepository.save(thesis);
    }

    @Transactional
    public Thesis completeThesis(Thesis thesis) {
        currentUserProvider().assertCanAccessResearchGroup(thesis.getResearchGroup());
        thesis.setState(ThesisState.FINISHED);

        saveStateChange(thesis, ThesisState.FINISHED);

        thesis = thesisRepository.save(thesis);

        for (User student : thesis.getStudents()) {
            if (!existsPendingThesis(student)) {
                accessManagementService.removeStudentGroup(student);
            }
        }

        return thesis;
    }

    /* UTILITY */

    private boolean existsPendingThesis(User user) {
        Page<Thesis> theses = thesisRepository.searchTheses(
            null,
                user.getId(),
                null,
                null,
                Set.of(
                        ThesisState.PROPOSAL,
                        ThesisState.WRITING,
                        ThesisState.SUBMITTED,
                        ThesisState.ASSESSED,
                        ThesisState.GRADED
                ),
                null,
                PageRequest.ofSize(1)
        );

        return theses.getTotalElements() > 0;
    }

    public Thesis findById(UUID thesisId) {
        Thesis thesis = thesisRepository.findById(thesisId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Thesis with id %s not found.", thesisId)));
        currentUserProvider().assertCanAccessResearchGroup(thesis.getResearchGroup());
        return thesis;
    }

    private void assignThesisRoles(
            Thesis thesis,
            List<UUID> supervisorIds,
            List<UUID> advisorIds,
            List<UUID> studentIds
    ) {
        currentUserProvider().assertCanAccessResearchGroup(thesis.getResearchGroup());
        List<User> supervisors = userRepository.findAllById(supervisorIds);
        List<User> advisors = userRepository.findAllById(advisorIds);
        List<User> students = userRepository.findAllById(studentIds);

        supervisors.sort(Comparator.comparing(user -> supervisorIds.indexOf(user.getId())));
        advisors.sort(Comparator.comparing(user -> advisorIds.indexOf(user.getId())));
        students.sort(Comparator.comparing(user -> studentIds.indexOf(user.getId())));

        if (supervisors.isEmpty() || supervisors.size() != supervisorIds.size()) {
            throw new ResourceInvalidParametersException("No supervisors selected or supervisors not found");
        }

        if (advisors.isEmpty() || advisors.size() != advisorIds.size()) {
            throw new ResourceInvalidParametersException("No advisors selected or advisors not found");
        }

        if (students.isEmpty() || students.size() != studentIds.size()) {
            throw new ResourceInvalidParametersException("No students selected or students not found");
        }

        thesisRoleRepository.deleteByThesisId(thesis.getId());
        thesis.setRoles(new ArrayList<>());

        for (int i = 0; i < supervisors.size(); i++) {
            User supervisor = supervisors.get(i);

            if (!supervisor.hasAnyGroup("supervisor")) {
                throw new ResourceInvalidParametersException("User is not a supervisor");
            }

            saveThesisRole(thesis, supervisor, ThesisRoleName.SUPERVISOR, i);
        }

        for (int i = 0; i < advisors.size(); i++) {
            User advisor = advisors.get(i);

            if (!advisor.hasAnyGroup("advisor", "supervisor")) {
                throw new ResourceInvalidParametersException("User is not an advisor");
            }

            saveThesisRole(thesis, advisor, ThesisRoleName.ADVISOR, i);
        }

        for (int i = 0; i < students.size(); i++) {
            User student = students.get(i);
            saveThesisRole(thesis, student, ThesisRoleName.STUDENT, i);
        }
    }

    private void saveStateChange(Thesis thesis, ThesisState state) {
        currentUserProvider().assertCanAccessResearchGroup(thesis.getResearchGroup());
        ThesisStateChangeId stateChangeId = new ThesisStateChangeId();
        stateChangeId.setThesisId(thesis.getId());
        stateChangeId.setState(state);

        ThesisStateChange stateChange = new ThesisStateChange();
        stateChange.setId(stateChangeId);
        stateChange.setThesis(thesis);
        stateChange.setChangedAt(Instant.now());

        thesisStateChangeRepository.save(stateChange);

        Set<ThesisStateChange> stateChanges = thesis.getStates();
        stateChanges.add(stateChange);
        thesis.setStates(stateChanges);
    }

    private void saveThesisRole(Thesis thesis, User user, ThesisRoleName role, int position) {
        currentUserProvider().assertCanAccessResearchGroup(thesis.getResearchGroup());
        User assigner = currentUserProvider().getUser();
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
        thesisRole.setPosition(position);

        thesisRoleRepository.save(thesisRole);

        List<ThesisRole> roles = thesis.getRoles();

        roles.add(thesisRole);
        roles.sort(Comparator.comparingInt(ThesisRole::getPosition));

        thesis.setRoles(roles);
    }
}
