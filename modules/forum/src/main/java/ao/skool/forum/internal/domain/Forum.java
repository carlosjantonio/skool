package ao.skool.forum.internal.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "forums",
       uniqueConstraints = @UniqueConstraint(name = "uk_forums_subject_turma",
                                              columnNames = {"subject_id", "turma_id", "academic_year_id"}),
       indexes = @Index(name = "idx_forums_tenant_turma", columnList = "tenant_id, turma_id"))
public class Forum {

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

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Forum() {}

    public Forum(UUID id, UUID tenantId, UUID subjectId, UUID turmaId, UUID academicYearId,
                 String title, String description) {
        this.id = id;
        this.tenantId = tenantId;
        this.subjectId = subjectId;
        this.turmaId = turmaId;
        this.academicYearId = academicYearId;
        this.title = title;
        this.description = description;
    }

    public UUID id() { return id; }
    public UUID tenantId() { return tenantId; }
    public UUID subjectId() { return subjectId; }
    public UUID turmaId() { return turmaId; }
    public UUID academicYearId() { return academicYearId; }
    public String title() { return title; }
    public String description() { return description; }
    public Instant createdAt() { return createdAt; }
}
