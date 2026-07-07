package ao.skool.sis.internal.persistence;

import ao.skool.sis.internal.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StudentRepository extends JpaRepository<Student, UUID> {
    List<Student> findByTenantIdOrderByFullNameAsc(UUID tenantId);
}
