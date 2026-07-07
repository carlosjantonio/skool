package ao.skool.sis.internal.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "enrollments",
       uniqueConstraints = @UniqueConstraint(name = "uk_enrollments_student_year",
                                             columnNames = {"student_id", "academic_year_id"}),
       indexes = {
           @Index(name = "idx_enrollments_tenant_year", columnList = "tenant_id, academic_year_id"),
           @Index(name = "idx_enrollments_turma", columnList = "turma_id")
       })
public class Enrollment {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "academic_year_id", nullable = false)
    private UUID academicYearId;

    @Column(name = "turma_id", nullable = false)
    private UUID turmaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EnrollmentStatus status = EnrollmentStatus.PENDING;

    @Column(name = "enrolled_at")
    private Instant enrolledAt;

    @Column(name = "withdrawn_at")
    private Instant withdrawnAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Enrollment() {}

    public Enrollment(UUID id, UUID tenantId, UUID studentId, UUID academicYearId, UUID turmaId) {
        this.id = id;
        this.tenantId = tenantId;
        this.studentId = studentId;
        this.academicYearId = academicYearId;
        this.turmaId = turmaId;
    }

    public UUID id() { return id; }
    public UUID tenantId() { return tenantId; }
    public UUID studentId() { return studentId; }
    public UUID academicYearId() { return academicYearId; }
    public UUID turmaId() { return turmaId; }
    public EnrollmentStatus status() { return status; }
    public Instant enrolledAt() { return enrolledAt; }
    public Instant withdrawnAt() { return withdrawnAt; }

    public void confirm() {
        this.status = EnrollmentStatus.ENROLLED;
        this.enrolledAt = Instant.now();
    }

    public void withdraw() {
        this.status = EnrollmentStatus.WITHDRAWN;
        this.withdrawnAt = Instant.now();
    }
}
