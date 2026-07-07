package ao.skool.academic_structure.internal.domain;

import jakarta.persistence.*;

import java.util.UUID;

/**
 * Turma = school class group (e.g., "10ª Classe A" — 10ª grade, group A).
 * Distinct from "class" the Java keyword and from a scheduled lesson.
 */
@Entity
@Table(name = "turmas",
       uniqueConstraints = @UniqueConstraint(name = "uk_turmas_year_school_name", columnNames = {"academic_year_id", "school_id", "name"}))
public class Turma {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "academic_year_id", nullable = false)
    private UUID academicYearId;

    @Column(nullable = false, length = 32)
    private String name; // e.g. "10A"

    @Enumerated(EnumType.STRING)
    @Column(name = "grade_level", nullable = false, length = 16)
    private GradeLevel gradeLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "track", nullable = false, length = 40)
    private CurricularTrack track;

    @Column(nullable = false)
    private int capacity;

    protected Turma() {}

    public Turma(UUID id, UUID tenantId, UUID schoolId, UUID academicYearId,
                 String name, GradeLevel gradeLevel, CurricularTrack track, int capacity) {
        this.id = id;
        this.tenantId = tenantId;
        this.schoolId = schoolId;
        this.academicYearId = academicYearId;
        this.name = name;
        this.gradeLevel = gradeLevel;
        this.track = track;
        this.capacity = capacity;
    }

    public UUID id() { return id; }
    public UUID tenantId() { return tenantId; }
    public UUID schoolId() { return schoolId; }
    public UUID academicYearId() { return academicYearId; }
    public String name() { return name; }
    public GradeLevel gradeLevel() { return gradeLevel; }
    public CurricularTrack track() { return track; }
    public int capacity() { return capacity; }
}
