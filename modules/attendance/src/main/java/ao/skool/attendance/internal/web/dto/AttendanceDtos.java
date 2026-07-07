package ao.skool.attendance.internal.web.dto;

import ao.skool.attendance.internal.domain.AttendanceStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class AttendanceDtos {

    public record AttendanceEntry(
            /** Client-generated stable UUID (v5 of student+turma+date). */
            @NotNull UUID id,
            @NotNull UUID turmaId,
            @NotNull UUID studentId,
            @NotNull LocalDate date,
            @NotNull AttendanceStatus status,
            String notes
    ) {}

    public record BatchRequest(@Valid @NotEmpty List<AttendanceEntry> records) {}

    public record BatchResult(
            List<UUID> accepted,
            List<UUID> skippedDuplicates,
            List<FailedEntry> failed
    ) {}

    public record FailedEntry(UUID id, String reason) {}

    public record AttendanceResponse(
            UUID id,
            UUID turmaId,
            UUID studentId,
            LocalDate date,
            AttendanceStatus status,
            String notes,
            Instant recordedAt
    ) {}

    public record StudentAttendanceSummary(
            UUID studentId,
            long presentCount,
            long absentCount,
            long lateCount,
            long excusedCount,
            LocalDate from,
            LocalDate to
    ) {}

    private AttendanceDtos() {}
}
