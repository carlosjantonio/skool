package ao.skool.attendance.internal.service;

import ao.skool.attendance.api.event.AttendanceMarkedAbsent;
import ao.skool.attendance.internal.domain.AttendanceRecord;
import ao.skool.attendance.internal.domain.AttendanceStatus;
import ao.skool.attendance.internal.persistence.AttendanceRepository;
import ao.skool.attendance.internal.web.dto.AttendanceDtos.AttendanceEntry;
import ao.skool.attendance.internal.web.dto.AttendanceDtos.AttendanceResponse;
import ao.skool.attendance.internal.web.dto.AttendanceDtos.BatchRequest;
import ao.skool.attendance.internal.web.dto.AttendanceDtos.BatchResult;
import ao.skool.attendance.internal.web.dto.AttendanceDtos.FailedEntry;
import ao.skool.attendance.internal.web.dto.AttendanceDtos.StudentAttendanceSummary;
import ao.skool.common.domain.SkoolPrincipal;
import ao.skool.common.domain.tenant.TenantContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class AttendanceService {

    private final AttendanceRepository attendance;
    private final TenantContext tenant;
    private final ApplicationEventPublisher events;

    public AttendanceService(AttendanceRepository attendance, TenantContext tenant,
                              ApplicationEventPublisher events) {
        this.attendance = attendance;
        this.tenant = tenant;
        this.events = events;
    }

    /**
     * Idempotent batch save. Records whose id is already stored are reported as skipped
     * without touching the row. Records that violate the (student, turma, date)
     * uniqueness constraint under a different id are reported as failed.
     */
    public BatchResult ingest(BatchRequest request) {
        var tenantId = tenant.current();
        UUID actor = currentActor();

        Set<UUID> incomingIds = new HashSet<>();
        for (var e : request.records()) incomingIds.add(e.id());
        Set<UUID> existingIds = new HashSet<>(
                attendance.findAllById(incomingIds).stream().map(AttendanceRecord::id).toList());

        List<UUID> accepted = new ArrayList<>();
        List<UUID> skipped = new ArrayList<>();
        List<FailedEntry> failed = new ArrayList<>();

        for (AttendanceEntry e : request.records()) {
            if (existingIds.contains(e.id())) {
                skipped.add(e.id());
                continue;
            }
            AttendanceRecord record = new AttendanceRecord(
                    e.id(), tenantId.value(), e.turmaId(), e.studentId(),
                    e.date(), e.status(), e.notes(), actor);
            try {
                attendance.save(record);
                accepted.add(e.id());
                if (e.status() == AttendanceStatus.ABSENT) {
                    events.publishEvent(new AttendanceMarkedAbsent(
                            UUID.randomUUID(), tenantId, e.studentId(), e.turmaId(),
                            e.date(), Instant.now()));
                }
            } catch (DataIntegrityViolationException ex) {
                failed.add(new FailedEntry(e.id(), "duplicate_student_turma_date"));
            }
        }
        return new BatchResult(accepted, skipped, failed);
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> forTurmaOnDate(UUID turmaId, LocalDate date) {
        return attendance.findByTurmaIdAndDateRecordedOrderByStudentIdAsc(turmaId, date)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public StudentAttendanceSummary summaryFor(UUID studentId, LocalDate from, LocalDate to) {
        long present = attendance.countByStudentIdAndStatusAndDateRecordedBetween(
                studentId, AttendanceStatus.PRESENT, from, to);
        long absent = attendance.countByStudentIdAndStatusAndDateRecordedBetween(
                studentId, AttendanceStatus.ABSENT, from, to);
        long late = attendance.countByStudentIdAndStatusAndDateRecordedBetween(
                studentId, AttendanceStatus.LATE, from, to);
        long excused = attendance.countByStudentIdAndStatusAndDateRecordedBetween(
                studentId, AttendanceStatus.EXCUSED, from, to);
        return new StudentAttendanceSummary(studentId, present, absent, late, excused, from, to);
    }

    private UUID currentActor() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SkoolPrincipal p) {
            try {
                return UUID.fromString(p.userId());
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    private AttendanceResponse toResponse(AttendanceRecord r) {
        return new AttendanceResponse(r.id(), r.turmaId(), r.studentId(), r.dateRecorded(),
                r.status(), r.notes(), r.recordedAt());
    }
}
