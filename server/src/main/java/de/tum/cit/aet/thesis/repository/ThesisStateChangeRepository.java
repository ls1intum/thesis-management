package de.tum.cit.aet.thesis.repository;

import de.tum.cit.aet.thesis.entity.ThesisStateChange;
import de.tum.cit.aet.thesis.entity.key.ThesisStateChangeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface ThesisStateChangeRepository extends JpaRepository<ThesisStateChange, ThesisStateChangeId> {
}
