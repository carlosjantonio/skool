package ao.skool.attendance.internal.web;

import ao.skool.attendance.internal.service.AttendanceService;
import ao.skool.attendance.internal.web.dto.AttendanceDtos.AttendanceResponse;
import ao.skool.attendance.internal.web.dto.AttendanceDtos.BatchRequest;
import ao.skool.attendance.internal.web.dto.AttendanceDtos.BatchResult;
import ao.skool.attendance.internal.web.dto.AttendanceDtos.StudentAttendanceSummary;
import ao.skool.common.security.Roles;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService service;

    public AttendanceController(AttendanceService service) {
        this.service = service;
    }

    @PostMapping("/batch")
    @PreAuthorize("hasAnyRole('" + Roles.TEACHER + "','" + Roles.DIRECTOR + "','" + Roles.SECRETARY + "','" + Roles.ADMIN + "')")
    public BatchResult batch(@Valid @RequestBody BatchRequest request) {
        return service.ingest(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('" + Roles.TEACHER + "','" + Roles.DIRECTOR + "','" + Roles.SECRETARY + "','" + Roles.ADMIN + "')")
    public List<AttendanceResponse> forTurmaOnDate(
            @RequestParam UUID turmaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.forTurmaOnDate(turmaId, date);
    }

    @GetMapping("/students/{studentId}/summary")
    @PreAuthorize("hasAnyRole('" + Roles.TEACHER + "','" + Roles.DIRECTOR + "','" + Roles.SECRETARY + "','" + Roles.ADMIN + "','" + Roles.GUARDIAN + "')")
    public StudentAttendanceSummary summary(
            @PathVariable UUID studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.summaryFor(studentId, from, to);
    }
}
