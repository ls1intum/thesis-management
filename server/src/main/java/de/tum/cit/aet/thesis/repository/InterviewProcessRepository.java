package de.tum.cit.aet.thesis.repository;

import de.tum.cit.aet.thesis.entity.InterviewProcess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InterviewProcessRepository extends JpaRepository<InterviewProcess, UUID> {
}
