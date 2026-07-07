package ao.skool.sis.internal.persistence;

import ao.skool.sis.internal.domain.StudentGuardianLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StudentGuardianRepository extends JpaRepository<StudentGuardianLink, StudentGuardianLink.Key> {
    List<StudentGuardianLink> findByKeyStudentId(UUID studentId);
    List<StudentGuardianLink> findByKeyGuardianId(UUID guardianId);
}
