package ao.skool.staff.internal.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Assigns a staff member to teach a subject to a turma for a given academic year.
 * Head-teacher assignments use {@code subject_id = null} — the head teacher is
 * responsible for the turma, not any specific subject.
 */
@Entity
@Table(name = "staff_assignments",
       indexes = {
           @Index(name = "idx_staff_assignments_tenant_year", columnList = "tenant_id, academic_year_id"),
           @Index(name = "idx_staff_assignments_turma", columnList = "turma_id"),
           @Index(name = "idx_staff_assignments_staff", columnList = "staff_id")
       })
public class StaffAssignment {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "staff_id", nullable = false)
    private UUID staffId;

    @Column(name = "turma_id", nullable = false)
    private UUID turmaId;

    @Column(name = "subject_id")
    private UUID subjectId;

    @Column(name = "academic_year_id", nullable = false)
    private UUID academicYearId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AssignmentRole role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected StaffAssignment() {}

    public StaffAssignment(UUID id, UUID tenantId, UUID staffId, UUID turmaId, UUID subjectId,
                           UUID academicYearId, AssignmentRole role) {
        this.id = id;
        this.tenantId = tenantId;
        this.staffId = staffId;
        this.turmaId = turmaId;
        this.subjectId = subjectId;
        this.academicYearId = academicYearId;
        this.role = role;
    }

    public UUID id() { return id; }
    public UUID tenantId() { return tenantId; }
    public UUID staffId() { return staffId; }
    public UUID turmaId() { return turmaId; }
    public UUID subjectId() { return subjectId; }
    public UUID academicYearId() { return academicYearId; }
    public AssignmentRole role() { return role; }
}
