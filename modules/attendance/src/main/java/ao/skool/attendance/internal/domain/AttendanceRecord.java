package ao.skool.attendance.internal.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One attendance mark for a student on a specific date in a specific turma.
 * The primary key {@link #id} is <b>client-generated</b> — the offline PWA computes a
 * deterministic UUID from (student, turma, date) so retrying a sync doesn't create a
 * second row for the same event. The uniqueness constraint on
 * {@code (student_id, turma_id, date_recorded)} defends against a mis-behaving client
 * that ships a fresh UUID for the same logical event.
 */
@Entity
@Table(name = "attendance_records",
       uniqueConstraints = @UniqueConstraint(name = "uk_attendance_student_turma_date",
                                              columnNames = {"student_id", "turma_id", "date_recorded"}),
       indexes = {
           @Index(name = "idx_attendance_tenant_date", columnList = "tenant_id, date_recorded"),
           @Index(name = "idx_attendance_turma_date", columnList = "turma_id, date_recorded")
       })
public class AttendanceRecord {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "turma_id", nullable = false)
    private UUID turmaId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "date_recorded", nullable = false)
    private LocalDate dateRecorded;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AttendanceStatus status;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "recorded_by")
    private UUID recordedBy;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt = Instant.now();

    protected AttendanceRecord() {}

    public AttendanceRecord(UUID id, UUID tenantId, UUID turmaId, UUID studentId,
                            LocalDate dateRecorded, AttendanceStatus status,
                            String notes, UUID recordedBy) {
        this.id = id;
        this.tenantId = tenantId;
        this.turmaId = turmaId;
        this.studentId = studentId;
        this.dateRecorded = dateRecorded;
        this.status = status;
        this.notes = notes;
        this.recordedBy = recordedBy;
    }

    public UUID id() { return id; }
    public UUID tenantId() { return tenantId; }
    public UUID turmaId() { return turmaId; }
    public UUID studentId() { return studentId; }
    public LocalDate dateRecorded() { return dateRecorded; }
    public AttendanceStatus status() { return status; }
    public String notes() { return notes; }
    public UUID recordedBy() { return recordedBy; }
    public Instant recordedAt() { return recordedAt; }
}
