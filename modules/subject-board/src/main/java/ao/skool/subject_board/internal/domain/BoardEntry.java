package ao.skool.subject_board.internal.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "board_entries",
       indexes = {
           @Index(name = "idx_board_tenant_turma", columnList = "tenant_id, turma_id, published_at DESC"),
           @Index(name = "idx_board_subject_turma", columnList = "subject_id, turma_id, published_at DESC"),
           @Index(name = "idx_board_due", columnList = "due_at")
       })
public class BoardEntry {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Column(name = "turma_id", nullable = false)
    private UUID turmaId;

    @Column(name = "academic_year_id", nullable = false)
    private UUID academicYearId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private BoardEntryKind kind;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(name = "document_id")
    private UUID documentId;

    @Column(name = "external_url", length = 1024)
    private String externalUrl;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(nullable = false)
    private boolean pinned = false;

    @Column(name = "low_bandwidth", nullable = false)
    private boolean lowBandwidth = false;

    @Column(name = "author_id")
    private UUID authorId;

    @Column(name = "author_name", length = 255)
    private String authorName;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt = Instant.now();

    protected BoardEntry() {}

    public BoardEntry(UUID id, UUID tenantId, UUID subjectId, UUID turmaId, UUID academicYearId,
                      BoardEntryKind kind, String title, String body, UUID documentId,
                      String externalUrl, Instant dueAt, boolean pinned, boolean lowBandwidth,
                      UUID authorId, String authorName) {
        this.id = id;
        this.tenantId = tenantId;
        this.subjectId = subjectId;
        this.turmaId = turmaId;
        this.academicYearId = academicYearId;
        this.kind = kind;
        this.title = title;
        this.body = body;
        this.documentId = documentId;
        this.externalUrl = externalUrl;
        this.dueAt = dueAt;
        this.pinned = pinned;
        this.lowBandwidth = lowBandwidth;
        this.authorId = authorId;
        this.authorName = authorName;
    }

    public UUID id() { return id; }
    public UUID tenantId() { return tenantId; }
    public UUID subjectId() { return subjectId; }
    public UUID turmaId() { return turmaId; }
    public UUID academicYearId() { return academicYearId; }
    public BoardEntryKind kind() { return kind; }
    public String title() { return title; }
    public String body() { return body; }
    public UUID documentId() { return documentId; }
    public String externalUrl() { return externalUrl; }
    public Instant dueAt() { return dueAt; }
    public boolean pinned() { return pinned; }
    public boolean lowBandwidth() { return lowBandwidth; }
    public UUID authorId() { return authorId; }
    public String authorName() { return authorName; }
    public Instant publishedAt() { return publishedAt; }
}
