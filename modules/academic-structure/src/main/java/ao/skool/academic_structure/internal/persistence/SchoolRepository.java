package ao.skool.academic_structure.internal.persistence;

import ao.skool.academic_structure.internal.domain.School;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SchoolRepository extends JpaRepository<School, UUID> {
    Optional<School> findByCode(String code);
}
