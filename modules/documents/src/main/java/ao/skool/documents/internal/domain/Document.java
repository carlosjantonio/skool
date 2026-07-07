package ao.skool.documents.internal.domain;

import ao.skool.documents.api.DocumentType;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documents",
       indexes = {
           @Index(name = "idx_documents_tenant", columnList = "tenant_id"),
           @Index(name = "idx_documents_owner", columnList = "owner_type, owner_id")
       })
public class Document {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 255)
    private String filename;

    @Column(name = "content_type", nullable = false, length = 128)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 32)
    private DocumentType documentType;

    /** e.g. STUDENT, GUARDIAN, STAFF. Free-form so any module can own documents. */
    @Column(name = "owner_type", nullable = false, length = 32)
    private String ownerType;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    /** MinIO object key. Path shape: {@code <tenantId>/<documentId>/<filename>}. */
    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;

    @Column(name = "uploaded_by")
    private String uploadedBy;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt = Instant.now();

    protected Document() {}

    public Document(UUID id, UUID tenantId, String filename, String contentType, long sizeBytes,
                    DocumentType documentType, String ownerType, UUID ownerId, String storageKey,
                    String uploadedBy) {
        this.id = id;
        this.tenantId = tenantId;
        this.filename = filename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.documentType = documentType;
        this.ownerType = ownerType;
        this.ownerId = ownerId;
        this.storageKey = storageKey;
        this.uploadedBy = uploadedBy;
    }

    public UUID id() { return id; }
    public UUID tenantId() { return tenantId; }
    public String filename() { return filename; }
    public String contentType() { return contentType; }
    public long sizeBytes() { return sizeBytes; }
    public DocumentType documentType() { return documentType; }
    public String ownerType() { return ownerType; }
    public UUID ownerId() { return ownerId; }
    public String storageKey() { return storageKey; }
    public String uploadedBy() { return uploadedBy; }
    public Instant uploadedAt() { return uploadedAt; }
}
