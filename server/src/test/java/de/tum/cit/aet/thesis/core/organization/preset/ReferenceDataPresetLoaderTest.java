package de.tum.cit.aet.thesis.core.organization.preset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.tum.cit.aet.thesis.core.organization.entity.School;
import de.tum.cit.aet.thesis.core.organization.entity.StudyProgram;
import de.tum.cit.aet.thesis.core.organization.preset.ReferenceDataPresetLoader.PresetData;
import de.tum.cit.aet.thesis.core.organization.repository.DepartmentRepository;
import de.tum.cit.aet.thesis.core.organization.repository.SchoolRepository;
import de.tum.cit.aet.thesis.core.organization.repository.StudyProgramRepository;
import de.tum.cit.aet.thesis.mock.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Testcontainers
class ReferenceDataPresetLoaderTest extends BaseIntegrationTest {

	@DynamicPropertySource
	static void configureDynamicProperties(DynamicPropertyRegistry registry) {
		configureProperties(registry);
	}

	@Autowired
	private ReferenceDataPresetLoader loader;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private SchoolRepository schoolRepository;

	@Autowired
	private DepartmentRepository departmentRepository;

	@Autowired
	private StudyProgramRepository studyProgramRepository;

	private PresetData tumPreset() throws IOException {
		try (InputStream in = getClass().getResourceAsStream("/reference-data/tum.json")) {
			return mapper.readValue(in, PresetData.class);
		}
	}

	@Test
	void tumPreset_ContainsTheSevenSchoolsWithDepartmentsAndPortals() throws IOException {
		PresetData preset = tumPreset();

		assertThat(preset.schools()).extracting(ReferenceDataPresetLoader.PresetSchool::abbreviation)
				.containsExactlyInAnyOrder("CIT", "ED", "NAT", "LS", "MH", "MGT", "SOT");
		assertThat(preset.schools()).allSatisfy(school -> assertThat(school.departments()).isNotEmpty());
		assertThat(preset.schools()).filteredOn(school -> school.thesisPortalUrl() != null)
				.extracting(ReferenceDataPresetLoader.PresetSchool::thesisPortalUrl)
				.contains("https://portal.cit.tum.de/", "https://portal.mgt.tum.de/", "https://portal.mh.tum.de/");
		assertThat(preset.studyPrograms()).extracting(ReferenceDataPresetLoader.PresetStudyProgram::key)
				.doesNotHaveDuplicates()
				.contains("COMPUTER_SCIENCE", "GAMES_ENGINEERING", "INFORMATION_SYSTEMS", "MANAGEMENT_AND_TECHNOLOGY", "OTHER");
		Set<String> schoolAbbreviations = preset.schools().stream()
				.map(ReferenceDataPresetLoader.PresetSchool::abbreviation).collect(Collectors.toSet());
		assertThat(preset.studyPrograms()).filteredOn(program -> program.school() != null)
				.allSatisfy(program -> assertThat(schoolAbbreviations).contains(program.school()));
	}

	@Test
	void apply_FillsGapsWithoutOverwritingEditsAndIsIdempotent() throws IOException {
		String abbreviation = "P" + java.util.UUID.randomUUID().toString().substring(0, 8);
		String key = "PRESET_" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
		StudyProgram existing = new StudyProgram();
		existing.setKey(key);
		existing.setName("Edited by admin");
		studyProgramRepository.save(existing);

		PresetData preset = new PresetData(
				List.of(new ReferenceDataPresetLoader.PresetSchool("Preset School " + abbreviation, abbreviation, "https://example.org/", "https://portal.example.org/",
						List.of(new ReferenceDataPresetLoader.PresetDepartment("Preset Department", null)))),
				List.of(new ReferenceDataPresetLoader.PresetStudyProgram(key, "Preset name", abbreviation),
						new ReferenceDataPresetLoader.PresetStudyProgram(key + "_NEW", "New program", abbreviation)));

		loader.apply(preset);
		loader.apply(preset);

		School school = schoolRepository.findByAbbreviationIgnoreCase(abbreviation).orElseThrow();
		assertThat(school.getThesisPortalUrl()).isEqualTo("https://portal.example.org/");
		assertThat(departmentRepository.findBySchoolIdAndNameIgnoreCase(school.getId(), "Preset Department")).isPresent();
		assertThat(schoolRepository.findAll().stream().filter(s -> s.getAbbreviation().equals(abbreviation))).hasSize(1);

		StudyProgram filled = studyProgramRepository.findByKeyIgnoreCase(key).orElseThrow();
		assertThat(filled.getName()).isEqualTo("Edited by admin");
		assertThat(filled.getSchool().getId()).isEqualTo(school.getId());
		StudyProgram created = studyProgramRepository.findByKeyIgnoreCase(key + "_NEW").orElseThrow();
		assertThat(created.getName()).isEqualTo("New program");
		assertThat(created.getSchool().getId()).isEqualTo(school.getId());
	}

	@Test
	void apply_KeepsSchoolThatAnAdminAlreadyAssigned() {
		String keyPrefix = java.util.UUID.randomUUID().toString().substring(0, 8);
		School adminChoice = new School();
		adminChoice.setName("Admin School " + keyPrefix);
		adminChoice.setAbbreviation("AS" + keyPrefix);
		adminChoice = schoolRepository.save(adminChoice);
		StudyProgram existing = new StudyProgram();
		existing.setKey("ADMIN_" + keyPrefix);
		existing.setName("Assigned");
		existing.setSchool(adminChoice);
		studyProgramRepository.save(existing);

		String abbreviation = "PS" + keyPrefix;
		loader.apply(new PresetData(
				List.of(new ReferenceDataPresetLoader.PresetSchool("Other " + keyPrefix, abbreviation, null, null, null)),
				List.of(new ReferenceDataPresetLoader.PresetStudyProgram("ADMIN_" + keyPrefix, "Assigned", abbreviation))));

		assertThat(studyProgramRepository.findByKeyIgnoreCase("ADMIN_" + keyPrefix).orElseThrow().getSchool().getId())
				.isEqualTo(adminChoice.getId());
	}

	@Test
	void applyAtomically_RollsBackEverythingWhenALaterEntryFails() {
		String abbreviation = "RB" + java.util.UUID.randomUUID().toString().substring(0, 8);
		PresetData broken = new PresetData(
				List.of(new ReferenceDataPresetLoader.PresetSchool("Rollback School " + abbreviation, abbreviation, null, null,
						List.of(new ReferenceDataPresetLoader.PresetDepartment("Department", null)))),
				// a study program without key violates the not-null constraint after the school was written
				List.of(new ReferenceDataPresetLoader.PresetStudyProgram(null, "Broken", abbreviation)));

		assertThatThrownBy(() -> loader.applyAtomically(broken)).isInstanceOf(Exception.class);

		assertThat(schoolRepository.findByAbbreviationIgnoreCase(abbreviation)).isEmpty();
	}

	@Test
	void run_WithoutConfiguredPreset_DoesNothing() throws Exception {
		long before = schoolRepository.count();

		loader.run(null);

		assertThat(schoolRepository.count()).isEqualTo(before);
	}
}
