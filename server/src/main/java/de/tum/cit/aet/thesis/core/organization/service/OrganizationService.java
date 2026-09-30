package de.tum.cit.aet.thesis.core.organization.service;

import de.tum.cit.aet.thesis.core.exception.request.ResourceAlreadyExistsException;
import de.tum.cit.aet.thesis.core.exception.request.ResourceInvalidParametersException;
import de.tum.cit.aet.thesis.core.exception.request.ResourceNotFoundException;
import de.tum.cit.aet.thesis.core.group.repository.ResearchGroupRepository;
import de.tum.cit.aet.thesis.core.organization.entity.Department;
import de.tum.cit.aet.thesis.core.organization.entity.School;
import de.tum.cit.aet.thesis.core.organization.entity.StudyProgram;
import de.tum.cit.aet.thesis.core.organization.repository.DepartmentRepository;
import de.tum.cit.aet.thesis.core.organization.repository.SchoolRepository;
import de.tum.cit.aet.thesis.core.organization.repository.StudyProgramRepository;
import de.tum.cit.aet.thesis.core.user.repository.UserRepository;
import de.tum.cit.aet.thesis.core.utility.RequestValidator;
import de.tum.cit.aet.thesis.thesis.repository.ThesisRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Manages the organisational reference data: schools (faculties), their departments and the study programs.
 * Entries that are still referenced by research groups, students or theses cannot be deleted, only deactivated.
 */
@Service
public class OrganizationService {
	private static final int MAX_NAME_LENGTH = 255;
	private static final int MAX_ABBREVIATION_LENGTH = 50;
	private static final int MAX_URL_LENGTH = 500;

	private final SchoolRepository schoolRepository;
	private final DepartmentRepository departmentRepository;
	private final StudyProgramRepository studyProgramRepository;
	private final ResearchGroupRepository researchGroupRepository;
	private final UserRepository userRepository;
	private final ThesisRepository thesisRepository;

	/**
	 * Creates the service with the repositories it needs to validate and protect reference data.
	 *
	 * @param schoolRepository the school repository
	 * @param departmentRepository the department repository
	 * @param studyProgramRepository the study program repository
	 * @param researchGroupRepository the research group repository
	 * @param userRepository the user repository
	 * @param thesisRepository the thesis repository
	 */
	@Autowired
	public OrganizationService(
			SchoolRepository schoolRepository,
			DepartmentRepository departmentRepository,
			StudyProgramRepository studyProgramRepository,
			ResearchGroupRepository researchGroupRepository,
			UserRepository userRepository,
			ThesisRepository thesisRepository
	) {
		this.schoolRepository = schoolRepository;
		this.departmentRepository = departmentRepository;
		this.studyProgramRepository = studyProgramRepository;
		this.researchGroupRepository = researchGroupRepository;
		this.userRepository = userRepository;
		this.thesisRepository = thesisRepository;
	}

	/**
	 * Returns all schools sorted by name.
	 *
	 * @return the schools
	 */
	public List<School> getSchools() {
		return schoolRepository.findAllByOrderByNameAsc();
	}

	/**
	 * Returns all departments sorted by name.
	 *
	 * @return the departments
	 */
	public List<Department> getDepartments() {
		return departmentRepository.findAllByOrderByNameAsc();
	}

	/**
	 * Returns all study programs sorted by name.
	 *
	 * @return the study programs
	 */
	public List<StudyProgram> getStudyPrograms() {
		return studyProgramRepository.findAllByOrderByNameAsc();
	}

	/**
	 * Finds a school by id.
	 *
	 * @param id the school id
	 * @return the school
	 */
	public School getSchool(UUID id) {
		return schoolRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("School not found"));
	}

	/**
	 * Finds a study program by id.
	 *
	 * @param id the study program id
	 * @return the study program
	 */
	public StudyProgram getStudyProgram(UUID id) {
		return studyProgramRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Study program not found"));
	}

	/**
	 * Resolves the study program to store for a user or thesis. {@code null} means "none".
	 * A deactivated study program may only be kept, not newly selected.
	 *
	 * @param id the study program id or {@code null}
	 * @param currentlyAssigned the study program that is already assigned, may be {@code null}
	 * @return the study program or {@code null}
	 */
	public StudyProgram resolveStudyProgramForAssignment(UUID id, StudyProgram currentlyAssigned) {
		if (id == null) {
			return null;
		}

		StudyProgram studyProgram = getStudyProgram(id);

		if (!studyProgram.isActive() && (currentlyAssigned == null || !currentlyAssigned.getId().equals(id))) {
			throw new ResourceInvalidParametersException("Study program is no longer available");
		}

		return studyProgram;
	}

	/**
	 * Resolves and validates the school and department of a research group. A department is only valid
	 * together with the school it belongs to.
	 *
	 * @param schoolId the school id or {@code null}
	 * @param departmentId the department id or {@code null}
	 * @param currentSchool the school currently assigned to the group, may be {@code null}
	 * @param currentDepartment the department currently assigned to the group, may be {@code null}
	 * @return the resolved school and department
	 */
	public SchoolAndDepartment resolveSchoolAndDepartment(
			UUID schoolId,
			UUID departmentId,
			School currentSchool,
			Department currentDepartment
	) {
		if (schoolId == null) {
			if (departmentId != null) {
				throw new ResourceInvalidParametersException("A department requires a school");
			}

			return new SchoolAndDepartment(null, null);
		}

		School school = getSchool(schoolId);

		if (!school.isActive() && (currentSchool == null || !currentSchool.getId().equals(schoolId))) {
			throw new ResourceInvalidParametersException("School is no longer available");
		}

		if (departmentId == null) {
			return new SchoolAndDepartment(school, null);
		}

		Department department = departmentRepository.findById(departmentId)
				.orElseThrow(() -> new ResourceNotFoundException("Department not found"));

		if (!department.getSchool().getId().equals(school.getId())) {
			throw new ResourceInvalidParametersException("The department does not belong to the selected school");
		}

		if (!department.isActive() && (currentDepartment == null || !currentDepartment.getId().equals(departmentId))) {
			throw new ResourceInvalidParametersException("Department is no longer available");
		}

		return new SchoolAndDepartment(school, department);
	}

	/**
	 * Creates a school.
	 *
	 * @param name the name
	 * @param abbreviation the abbreviation
	 * @param websiteUrl the website, optional
	 * @param thesisPortalUrl the portal students submit their thesis to, optional
	 * @param active whether the school can be selected
	 * @return the created school
	 */
	public School createSchool(String name, String abbreviation, String websiteUrl, String thesisPortalUrl, Boolean active) {
		School school = new School();

		applySchool(school, name, abbreviation, websiteUrl, thesisPortalUrl, active);

		return schoolRepository.save(school);
	}

	/**
	 * Updates a school.
	 *
	 * @param school the school to update
	 * @param name the name
	 * @param abbreviation the abbreviation
	 * @param websiteUrl the website, optional
	 * @param thesisPortalUrl the portal students submit their thesis to, optional
	 * @param active whether the school can be selected
	 * @return the updated school
	 */
	public School updateSchool(School school, String name, String abbreviation, String websiteUrl, String thesisPortalUrl, Boolean active) {
		applySchool(school, name, abbreviation, websiteUrl, thesisPortalUrl, active);

		return schoolRepository.save(school);
	}

	private void applySchool(School school, String name, String abbreviation, String websiteUrl, String thesisPortalUrl, Boolean active) {
		String validName = requireText(name, "name", MAX_NAME_LENGTH);
		String validAbbreviation = requireText(abbreviation, "abbreviation", MAX_ABBREVIATION_LENGTH);
		UUID id = school.getId() == null ? new UUID(0, 0) : school.getId();

		if (schoolRepository.existsByNameIgnoreCaseAndIdNot(validName, id)) {
			throw new ResourceAlreadyExistsException("A school with this name already exists");
		}

		if (schoolRepository.existsByAbbreviationIgnoreCaseAndIdNot(validAbbreviation, id)) {
			throw new ResourceAlreadyExistsException("A school with this abbreviation already exists");
		}

		school.setName(validName);
		school.setAbbreviation(validAbbreviation);
		school.setWebsiteUrl(optionalHttpUrl(websiteUrl, "website"));
		school.setThesisPortalUrl(optionalHttpUrl(thesisPortalUrl, "thesis portal"));
		school.setActive(active == null || active);
	}

	/**
	 * Deletes a school that is not used anywhere.
	 *
	 * @param school the school to delete
	 */
	public void deleteSchool(School school) {
		if (departmentRepository.existsBySchoolId(school.getId())
				|| studyProgramRepository.existsBySchoolId(school.getId())
				|| researchGroupRepository.existsBySchoolId(school.getId())) {
			throw new ResourceInvalidParametersException(
					"The school is still used by departments, study programs or research groups. Deactivate it instead.");
		}

		schoolRepository.delete(school);
	}

	/**
	 * Creates a department.
	 *
	 * @param schoolId the school the department belongs to
	 * @param name the name
	 * @param abbreviation the abbreviation, optional
	 * @param active whether the department can be selected
	 * @return the created department
	 */
	public Department createDepartment(UUID schoolId, String name, String abbreviation, Boolean active) {
		Department department = new Department();

		department.setSchool(getSchool(RequestValidator.validateNotNull(schoolId)));
		applyDepartment(department, name, abbreviation, active);

		return departmentRepository.save(department);
	}

	/**
	 * Updates a department. A department cannot move to another school.
	 *
	 * @param department the department to update
	 * @param schoolId the school id, must match the current school
	 * @param name the name
	 * @param abbreviation the abbreviation, optional
	 * @param active whether the department can be selected
	 * @return the updated department
	 */
	public Department updateDepartment(Department department, UUID schoolId, String name, String abbreviation, Boolean active) {
		if (schoolId != null && !schoolId.equals(department.getSchool().getId())) {
			throw new ResourceInvalidParametersException("A department cannot be moved to another school");
		}

		applyDepartment(department, name, abbreviation, active);

		return departmentRepository.save(department);
	}

	private void applyDepartment(Department department, String name, String abbreviation, Boolean active) {
		String validName = requireText(name, "name", MAX_NAME_LENGTH);
		UUID id = department.getId() == null ? new UUID(0, 0) : department.getId();

		if (departmentRepository.existsBySchoolIdAndNameIgnoreCaseAndIdNot(department.getSchool().getId(), validName, id)) {
			throw new ResourceAlreadyExistsException("This school already has a department with this name");
		}

		department.setName(validName);
		department.setAbbreviation(optionalText(abbreviation, "abbreviation", MAX_ABBREVIATION_LENGTH));
		department.setActive(active == null || active);
	}

	/**
	 * Finds a department by id.
	 *
	 * @param id the department id
	 * @return the department
	 */
	public Department getDepartment(UUID id) {
		return departmentRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Department not found"));
	}

	/**
	 * Deletes a department that is not used by a research group.
	 *
	 * @param department the department to delete
	 */
	public void deleteDepartment(Department department) {
		if (researchGroupRepository.existsByDepartmentId(department.getId())) {
			throw new ResourceInvalidParametersException("The department is still used by research groups. Deactivate it instead.");
		}

		departmentRepository.delete(department);
	}

	/**
	 * Creates a study program.
	 *
	 * @param key the stable key
	 * @param name the display name
	 * @param schoolId the school the program belongs to, optional
	 * @param active whether the study program can be selected
	 * @return the created study program
	 */
	public StudyProgram createStudyProgram(String key, String name, UUID schoolId, Boolean active) {
		StudyProgram studyProgram = new StudyProgram();

		applyStudyProgram(studyProgram, key, name, schoolId, active);

		return studyProgramRepository.save(studyProgram);
	}

	/**
	 * Updates a study program.
	 *
	 * @param studyProgram the study program to update
	 * @param key the stable key
	 * @param name the display name
	 * @param schoolId the school the program belongs to, optional
	 * @param active whether the study program can be selected
	 * @return the updated study program
	 */
	public StudyProgram updateStudyProgram(StudyProgram studyProgram, String key, String name, UUID schoolId, Boolean active) {
		applyStudyProgram(studyProgram, key, name, schoolId, active);

		return studyProgramRepository.save(studyProgram);
	}

	private void applyStudyProgram(StudyProgram studyProgram, String key, String name, UUID schoolId, Boolean active) {
		String validName = requireText(name, "name", MAX_NAME_LENGTH);
		String validKey = key == null || key.isBlank() ? generateKey(validName) : normalizeKey(key);
		UUID id = studyProgram.getId() == null ? new UUID(0, 0) : studyProgram.getId();

		if (studyProgramRepository.existsByKeyIgnoreCaseAndIdNot(validKey, id)) {
			throw new ResourceAlreadyExistsException("A study program with this key already exists");
		}

		studyProgram.setKey(validKey);
		studyProgram.setName(validName);
		studyProgram.setSchool(schoolId == null ? null : getSchool(schoolId));
		studyProgram.setActive(active == null || active);
	}

	/**
	 * Deletes a study program that is not used by a student or thesis.
	 *
	 * @param studyProgram the study program to delete
	 */
	public void deleteStudyProgram(StudyProgram studyProgram) {
		if (userRepository.existsByStudyProgramId(studyProgram.getId())
				|| thesisRepository.existsByStudyProgramId(studyProgram.getId())) {
			throw new ResourceInvalidParametersException("The study program is still used by students or theses. Deactivate it instead.");
		}

		studyProgramRepository.delete(studyProgram);
	}

	private static String normalizeKey(String key) {
		String normalized = key.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");

		if (normalized.isEmpty() || normalized.length() > MAX_NAME_LENGTH) {
			throw new ResourceInvalidParametersException("Invalid study program key");
		}

		return normalized;
	}

	private static String generateKey(String name) {
		return normalizeKey(name);
	}

	private static String requireText(String value, String field, int maxLength) {
		if (value == null || value.isBlank()) {
			throw new ResourceInvalidParametersException("The " + field + " must not be empty");
		}

		String trimmed = value.trim();

		if (trimmed.length() > maxLength) {
			throw new ResourceInvalidParametersException("The " + field + " must not be longer than " + maxLength + " characters");
		}

		return trimmed;
	}

	private static String optionalText(String value, String field, int maxLength) {
		return value == null || value.isBlank() ? null : requireText(value, field, maxLength);
	}

	/** Links are rendered as hyperlinks, so only plain http(s) URLs are accepted. */
	private static String optionalHttpUrl(String value, String field) {
		String trimmed = optionalText(value, field, MAX_URL_LENGTH);

		if (trimmed == null) {
			return null;
		}

		try {
			URI uri = URI.create(trimmed);
			String scheme = uri.getScheme();

			if (uri.getHost() == null || scheme == null
					|| !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
				throw new IllegalArgumentException();
			}
		} catch (IllegalArgumentException e) {
			throw new ResourceInvalidParametersException("The " + field + " must be a valid http(s) URL");
		}

		return trimmed;
	}

	/**
	 * The school and department chosen for a research group.
	 *
	 * @param school the school, may be {@code null}
	 * @param department the department, may be {@code null}
	 */
	public record SchoolAndDepartment(School school, Department department) {
	}
}
