package de.tum.cit.aet.thesis.core.organization.repository;

import de.tum.cit.aet.thesis.core.organization.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID> {
	List<Department> findAllByOrderByNameAsc();

	Optional<Department> findBySchoolIdAndNameIgnoreCase(UUID schoolId, String name);

	boolean existsBySchoolIdAndNameIgnoreCaseAndIdNot(UUID schoolId, String name, UUID id);

	boolean existsBySchoolId(UUID schoolId);
}
