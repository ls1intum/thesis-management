package de.tum.cit.aet.thesis.core.organization.repository;

import de.tum.cit.aet.thesis.core.organization.entity.StudyProgram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudyProgramRepository extends JpaRepository<StudyProgram, UUID> {
	List<StudyProgram> findAllByOrderByNameAsc();

	Optional<StudyProgram> findByKeyIgnoreCase(String key);

	boolean existsByKeyIgnoreCaseAndIdNot(String key, UUID id);

	boolean existsBySchoolId(UUID schoolId);
}
