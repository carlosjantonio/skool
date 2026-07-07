package ao.skool.academic_structure.internal.persistence;

import ao.skool.academic_structure.internal.domain.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubjectRepository extends JpaRepository<Subject, UUID> {
    List<Subject> findByTenantIdOrderByGradeLevelAscNameAsc(UUID tenantId);
}
