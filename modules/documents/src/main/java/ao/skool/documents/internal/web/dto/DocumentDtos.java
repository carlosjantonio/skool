package ao.skool.documents.internal.web.dto;

import ao.skool.documents.api.DocumentType;

import java.time.Instant;
import java.util.UUID;

public final class DocumentDtos {

    public record DocumentResponse(
            UUID id,
            String filename,
            String contentType,
            long sizeBytes,
            DocumentType documentType,
            String ownerType,
            UUID ownerId,
            Instant uploadedAt
    ) {}

    private DocumentDtos() {}
}
