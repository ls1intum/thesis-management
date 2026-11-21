package de.tum.cit.aet.thesis.core.organization.controller;

import de.tum.cit.aet.thesis.core.organization.controller.payload.StudyProgramPayload;
import de.tum.cit.aet.thesis.core.organization.dto.StudyProgramDto;
import de.tum.cit.aet.thesis.core.organization.entity.StudyProgram;
import de.tum.cit.aet.thesis.core.organization.service.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** REST controller for study programs. Reading is open to signed-in users, changes are admin only. */
@RestController
@RequestMapping("/v2/study-programs")
public class StudyProgramController {
	private final OrganizationService organizationService;

	/**
	 * Creates the controller.
	 *
	 * @param organizationService the organization service
	 */
	@Autowired
	public StudyProgramController(OrganizationService organizationService) {
		this.organizationService = organizationService;
	}

	/**
	 * Lists all study programs including deactivated ones (which clients hide from selection lists).
	 *
	 * @return the study programs
	 */
	@GetMapping
	public ResponseEntity<List<StudyProgramDto>> getStudyPrograms() {
		return ResponseEntity.ok(organizationService.getStudyPrograms().stream()
				.map(StudyProgramDto::fromStudyProgramEntity)
				.toList());
	}

	/**
	 * Creates a study program.
	 *
	 * @param payload the study program data
	 * @return the created study program
	 */
	@PostMapping
	@PreAuthorize("hasRole('admin')")
	public ResponseEntity<StudyProgramDto> createStudyProgram(@RequestBody StudyProgramPayload payload) {
		StudyProgram studyProgram = organizationService.createStudyProgram(
				payload.key(), payload.name(), payload.schoolId(), payload.active());

		return ResponseEntity.ok(StudyProgramDto.fromStudyProgramEntity(studyProgram));
	}

	/**
	 * Updates a study program.
	 *
	 * @param studyProgramId the study program id
	 * @param payload the study program data
	 * @return the updated study program
	 */
	@PutMapping("/{studyProgramId}")
	@PreAuthorize("hasRole('admin')")
	public ResponseEntity<StudyProgramDto> updateStudyProgram(@PathVariable UUID studyProgramId, @RequestBody StudyProgramPayload payload) {
		StudyProgram studyProgram = organizationService.updateStudyProgram(
				organizationService.getStudyProgram(studyProgramId),
				payload.key(), payload.name(), payload.schoolId(), payload.active());

		return ResponseEntity.ok(StudyProgramDto.fromStudyProgramEntity(studyProgram));
	}

	/**
	 * Deletes a study program that is not used by a student or thesis.
	 *
	 * @param studyProgramId the study program id
	 * @return an empty response
	 */
	@DeleteMapping("/{studyProgramId}")
	@PreAuthorize("hasRole('admin')")
	public ResponseEntity<Void> deleteStudyProgram(@PathVariable UUID studyProgramId) {
		organizationService.deleteStudyProgram(organizationService.getStudyProgram(studyProgramId));

		return ResponseEntity.noContent().build();
	}
}
