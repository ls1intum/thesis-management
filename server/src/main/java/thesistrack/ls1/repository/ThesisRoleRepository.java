package thesismanagement.ls1.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import thesismanagement.ls1.entity.ThesisRole;
import thesismanagement.ls1.entity.key.ThesisRoleId;

import java.util.List;
import java.util.UUID;


@Repository
public interface ThesisRoleRepository extends JpaRepository<ThesisRole, ThesisRoleId> {
    List<ThesisRole> deleteByThesisId(UUID thesisId);
}
