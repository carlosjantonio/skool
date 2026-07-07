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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
     * Idempotent upsert. Records are keyed by their client-supplied {@link AttendanceEntry#id()};
     * a new id inserts, a repeat id updates the same row. This lets the offline client
     * change a student's status ("I marked them PRESENT then remembered they were LATE")
     * without needing a separate PATCH endpoint. Retrying an unchanged batch after a
     * partial send is still safe — the same values are written back and the request
     * is reported as accepted, not failed.
     * <p>
     * The uniqueness constraint on {@code (student, turma, date)} still defends against
     * a mis-behaving client that ships two different UUIDs for the same event — those
     * are reported as {@code failed} rather than silently accepted.
     */
    public BatchResult ingest(BatchRequest request) {
        var tenantId = tenant.current();
        UUID actor = currentActor();

        Set<UUID> incomingIds = new HashSet<>();
        for (var e : request.records()) incomingIds.add(e.id());
        Map<UUID, AttendanceRecord> existingById = new HashMap<>();
        for (var r : attendance.findAllById(incomingIds)) existingById.put(r.id(), r);

        List<UUID> accepted = new ArrayList<>();
        List<UUID> skipped = List.of(); // no-op skips under upsert semantics
        List<FailedEntry> failed = new ArrayList<>();

        for (AttendanceEntry e : request.records()) {
            AttendanceRecord existing = existingById.get(e.id());
            boolean wasAbsentBefore = existing != null && existing.status() == AttendanceStatus.ABSENT;
            AttendanceRecord record = existing != null
                    ? updateExisting(existing, e, actor)
                    : new AttendanceRecord(e.id(), tenantId.value(), e.turmaId(), e.studentId(),
                            e.date(), e.status(), e.notes(), actor);
            try {
                attendance.save(record);
                accepted.add(e.id());
                if (e.status() == AttendanceStatus.ABSENT && !wasAbsentBefore) {
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

    private AttendanceRecord updateExisting(AttendanceRecord existing, AttendanceEntry e, UUID actor) {
        existing.applyUpdate(e.status(), e.notes(), actor);
        return existing;
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
