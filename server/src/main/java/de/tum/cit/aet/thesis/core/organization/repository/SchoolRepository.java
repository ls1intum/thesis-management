package de.tum.cit.aet.thesis.core.organization.repository;

import de.tum.cit.aet.thesis.core.organization.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SchoolRepository extends JpaRepository<School, UUID> {
	List<School> findAllByOrderByNameAsc();

	Optional<School> findByAbbreviationIgnoreCase(String abbreviation);

	boolean existsByAbbreviationIgnoreCaseAndIdNot(String abbreviation, UUID id);

	boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
}
