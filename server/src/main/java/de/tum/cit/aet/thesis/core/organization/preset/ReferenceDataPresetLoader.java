package de.tum.cit.aet.thesis.core.organization.preset;

import de.tum.cit.aet.thesis.core.organization.entity.Department;
import de.tum.cit.aet.thesis.core.organization.entity.School;
import de.tum.cit.aet.thesis.core.organization.entity.StudyProgram;
import de.tum.cit.aet.thesis.core.organization.repository.DepartmentRepository;
import de.tum.cit.aet.thesis.core.organization.repository.SchoolRepository;
import de.tum.cit.aet.thesis.core.organization.repository.StudyProgramRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Pre-fills schools, departments and study programs from a bundled preset (for example {@code tum}) at startup.
 *
 * <p>The preset is only applied when {@code thesis-management.reference-data.preset} is set and no school exists
 * yet, so it never resurrects entries that admins deleted and never overwrites what they edited. Study programs
 * that already exist (for example those migrated from the old free-text values) keep their name and only get the
 * preset's school if they had none.</p>
 */
@Component
public class ReferenceDataPresetLoader implements ApplicationRunner {
	private static final Logger log = LoggerFactory.getLogger(ReferenceDataPresetLoader.class);
	private static final Pattern PRESET_NAME = Pattern.compile("[a-z0-9-]+");

	private final String preset;
	private final ObjectMapper objectMapper;
	private final SchoolRepository schoolRepository;
	private final DepartmentRepository departmentRepository;
	private final StudyProgramRepository studyProgramRepository;
	private final TransactionTemplate transactionTemplate;

	/**
	 * Creates the loader.
	 *
	 * @param preset the configured preset name, empty for none
	 * @param objectMapper the object mapper used to read the preset file
	 * @param schoolRepository the school repository
	 * @param departmentRepository the department repository
	 * @param studyProgramRepository the study program repository
	 * @param transactionManager the transaction manager used to apply the preset atomically
	 */
	@Autowired
	public ReferenceDataPresetLoader(
			@Value("${thesis-management.reference-data.preset:}") String preset,
			ObjectMapper objectMapper,
			SchoolRepository schoolRepository,
			DepartmentRepository departmentRepository,
			StudyProgramRepository studyProgramRepository,
			PlatformTransactionManager transactionManager
	) {
		this.preset = preset == null ? "" : preset.trim().toLowerCase(Locale.ROOT);
		this.objectMapper = objectMapper;
		this.schoolRepository = schoolRepository;
		this.departmentRepository = departmentRepository;
		this.studyProgramRepository = studyProgramRepository;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	/**
	 * Applies the configured preset, if any and if no school exists yet.
	 *
	 * @param args the application arguments (unused)
	 * @throws IOException if the preset file cannot be read
	 */
	@Override
	public void run(ApplicationArguments args) throws IOException {
		if (preset.isEmpty()) {
			return;
		}

		if (!PRESET_NAME.matcher(preset).matches()) {
			log.warn("Ignoring reference data preset with invalid name '{}'", preset);
			return;
		}

		ClassPathResource resource = new ClassPathResource("reference-data/" + preset + ".json");

		if (!resource.exists()) {
			log.warn("Reference data preset '{}' does not exist", preset);
			return;
		}

		if (schoolRepository.count() > 0) {
			log.info("Skipping reference data preset '{}': schools already exist", preset);
			return;
		}

		PresetData data;

		try (InputStream in = resource.getInputStream()) {
			data = objectMapper.readValue(in, PresetData.class);
		}

		applyAtomically(data);
	}

	/**
	 * Applies the preset in a single transaction. The preset is skipped once a school exists, so a partially
	 * applied preset (for example after a crash) would never be completed; either everything is written or nothing.
	 *
	 * @param data the preset data
	 */
	void applyAtomically(PresetData data) {
		transactionTemplate.executeWithoutResult(status -> apply(data));
	}

	/**
	 * Applies the preset data. Package-private so tests can apply presets without configuring the property.
	 *
	 * @param data the preset data
	 */
	void apply(PresetData data) {
		for (PresetSchool presetSchool : data.schools()) {
			School school = schoolRepository.findByAbbreviationIgnoreCase(presetSchool.abbreviation()).orElseGet(School::new);

			if (school.getId() == null) {
				school.setName(presetSchool.name());
				school.setAbbreviation(presetSchool.abbreviation());
				school.setWebsiteUrl(presetSchool.websiteUrl());
				school.setThesisPortalUrl(presetSchool.thesisPortalUrl());
				school = schoolRepository.save(school);
			}

			for (PresetDepartment presetDepartment : presetSchool.departments() == null ? List.<PresetDepartment>of() : presetSchool.departments()) {
				if (departmentRepository.findBySchoolIdAndNameIgnoreCase(school.getId(), presetDepartment.name()).isEmpty()) {
					Department department = new Department();
					department.setSchool(school);
					department.setName(presetDepartment.name());
					department.setAbbreviation(presetDepartment.abbreviation());
					departmentRepository.save(department);
				}
			}
		}

		for (PresetStudyProgram presetProgram : data.studyPrograms()) {
			School school = presetProgram.school() == null
					? null
					: schoolRepository.findByAbbreviationIgnoreCase(presetProgram.school()).orElse(null);

			StudyProgram studyProgram = studyProgramRepository.findByKeyIgnoreCase(presetProgram.key()).orElseGet(StudyProgram::new);

			if (studyProgram.getId() == null) {
				studyProgram.setKey(presetProgram.key());
				studyProgram.setName(presetProgram.name());
				studyProgram.setSchool(school);
				studyProgramRepository.save(studyProgram);
			} else if (studyProgram.getSchool() == null && school != null) {
				studyProgram.setSchool(school);
				studyProgramRepository.save(studyProgram);
			}
		}

		log.info("Applied reference data preset '{}': {} schools, {} study programs", preset, data.schools().size(), data.studyPrograms().size());
	}

	/**
	 * The content of a preset file.
	 *
	 * @param schools the schools with their departments
	 * @param studyPrograms the study programs
	 */
	public record PresetData(List<PresetSchool> schools, List<PresetStudyProgram> studyPrograms) {
		/**
		 * Normalises missing lists to empty ones.
		 *
		 * @param schools the schools with their departments
		 * @param studyPrograms the study programs
		 */
		public PresetData {
			schools = schools == null ? List.of() : schools;
			studyPrograms = studyPrograms == null ? List.of() : studyPrograms;
		}
	}

	/**
	 * A school of a preset.
	 *
	 * @param name the name
	 * @param abbreviation the abbreviation
	 * @param websiteUrl the website
	 * @param thesisPortalUrl the thesis portal
	 * @param departments the departments
	 */
	public record PresetSchool(String name, String abbreviation, String websiteUrl, String thesisPortalUrl, List<PresetDepartment> departments) {
	}

	/**
	 * A department of a preset school.
	 *
	 * @param name the name
	 * @param abbreviation the abbreviation
	 */
	public record PresetDepartment(String name, String abbreviation) {
	}

	/**
	 * A study program of a preset.
	 *
	 * @param key the stable key
	 * @param name the name
	 * @param school the abbreviation of the school
	 */
	public record PresetStudyProgram(String key, String name, String school) {
	}
}
