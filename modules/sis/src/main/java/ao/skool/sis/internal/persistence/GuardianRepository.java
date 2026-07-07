package ao.skool.sis.internal.persistence;

import ao.skool.sis.internal.domain.Guardian;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GuardianRepository extends JpaRepository<Guardian, UUID> {
    Optional<Guardian> findByTenantIdAndEmailIgnoreCase(UUID tenantId, String email);
    Optional<Guardian> findByUserId(UUID userId);
}
