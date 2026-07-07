package ao.skool.documents.internal.service;

import ao.skool.common.domain.SkoolPrincipal;
import ao.skool.common.domain.tenant.TenantContext;
import ao.skool.common.web.error.ApplicationException;
import ao.skool.common.web.error.NotFoundException;
import ao.skool.documents.api.DocumentType;
import ao.skool.documents.internal.domain.Document;
import ao.skool.documents.internal.minio.MinioProperties;
import ao.skool.documents.internal.persistence.DocumentRepository;
import ao.skool.documents.internal.web.dto.DocumentDtos.DocumentResponse;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DocumentService {

    private final DocumentRepository documents;
    private final MinioClient minio;
    private final MinioProperties minioProps;
    private final TenantContext tenant;

    public DocumentService(DocumentRepository documents, MinioClient minio,
                           MinioProperties minioProps, TenantContext tenant) {
        this.documents = documents;
        this.minio = minio;
        this.minioProps = minioProps;
        this.tenant = tenant;
    }

    public DocumentResponse upload(MultipartFile file, DocumentType type, String ownerType, UUID ownerId) {
        UUID tenantId = tenant.current().value();
        UUID documentId = UUID.randomUUID();
        String storageKey = "%s/%s/%s".formatted(tenantId, documentId, safeFilename(file.getOriginalFilename()));

        try (InputStream in = file.getInputStream()) {
            minio.putObject(PutObjectArgs.builder()
                    .bucket(minioProps.bucket())
                    .object(storageKey)
                    .stream(in, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (Exception e) {
            throw new ApplicationException(HttpStatus.INTERNAL_SERVER_ERROR, "error.internal");
        }

        Document doc = new Document(
                documentId, tenantId,
                safeFilename(file.getOriginalFilename()),
                file.getContentType() == null ? "application/octet-stream" : file.getContentType(),
                file.getSize(),
                type, ownerType, ownerId, storageKey,
                currentActorId()
        );
        documents.save(doc);
        return toResponse(doc);
    }

    public DownloadStream download(UUID id) {
        Document doc = documents.findById(id).orElseThrow(NotFoundException::new);
        if (!doc.tenantId().equals(tenant.current().value())) throw new NotFoundException();
        try {
            InputStream stream = minio.getObject(GetObjectArgs.builder()
                    .bucket(minioProps.bucket())
                    .object(doc.storageKey())
                    .build());
            return new DownloadStream(doc, stream);
        } catch (Exception e) {
            throw new ApplicationException(HttpStatus.INTERNAL_SERVER_ERROR, "error.internal");
        }
    }

    public void delete(UUID id) {
        Document doc = documents.findById(id).orElseThrow(NotFoundException::new);
        if (!doc.tenantId().equals(tenant.current().value())) throw new NotFoundException();
        try {
            minio.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioProps.bucket())
                    .object(doc.storageKey())
                    .build());
        } catch (Exception ignored) {
            // Object may already be gone from MinIO — still delete the metadata row.
        }
        documents.delete(doc);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> listForOwner(String ownerType, UUID ownerId) {
        return documents.findByTenantIdAndOwnerTypeAndOwnerId(tenant.current().value(), ownerType, ownerId)
                .stream().map(this::toResponse).toList();
    }

    private String safeFilename(String name) {
        if (name == null || name.isBlank()) return "unnamed";
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private String currentActorId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SkoolPrincipal p) return p.userId();
        return "system";
    }

    private DocumentResponse toResponse(Document d) {
        return new DocumentResponse(d.id(), d.filename(), d.contentType(), d.sizeBytes(),
                d.documentType(), d.ownerType(), d.ownerId(), d.uploadedAt());
    }

    public record DownloadStream(Document metadata, InputStream stream) implements AutoCloseable {
        @Override public void close() throws IOException { stream.close(); }
    }
}
