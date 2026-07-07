package ao.skool.documents.internal.persistence;

import ao.skool.documents.internal.domain.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    List<Document> findByTenantIdAndOwnerTypeAndOwnerId(UUID tenantId, String ownerType, UUID ownerId);
}
