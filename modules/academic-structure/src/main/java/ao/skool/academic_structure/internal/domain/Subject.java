package ao.skool.academic_structure.internal.domain;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "subjects",
       uniqueConstraints = @UniqueConstraint(name = "uk_subjects_tenant_code", columnNames = {"tenant_id", "code"}))
public class Subject {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 32)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade_level", nullable = false, length = 16)
    private GradeLevel gradeLevel;

    protected Subject() {}

    public Subject(UUID id, UUID tenantId, String name, String code, GradeLevel gradeLevel) {
        this.id = id;
        this.tenantId = tenantId;
        this.name = name;
        this.code = code;
        this.gradeLevel = gradeLevel;
    }

    public UUID id() { return id; }
    public UUID tenantId() { return tenantId; }
    public String name() { return name; }
    public String code() { return code; }
    public GradeLevel gradeLevel() { return gradeLevel; }
}
