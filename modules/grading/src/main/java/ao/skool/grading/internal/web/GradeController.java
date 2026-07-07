package ao.skool.grading.internal.web;

import ao.skool.common.security.Roles;
import ao.skool.grading.internal.service.GradeService;
import ao.skool.grading.internal.web.dto.GradeDtos.BatchGrades;
import ao.skool.grading.internal.web.dto.GradeDtos.CreateGrade;
import ao.skool.grading.internal.web.dto.GradeDtos.GradeResponse;
import ao.skool.grading.internal.web.dto.GradeDtos.StudentGradeSummary;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/grades")
public class GradeController {

    private final GradeService service;

    public GradeController(GradeService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('" + Roles.TEACHER + "','" + Roles.DIRECTOR + "','" + Roles.ADMIN + "')")
    public GradeResponse record(@Valid @RequestBody CreateGrade cmd) {
        return service.record(cmd);
    }

    @PostMapping("/batch")
    @PreAuthorize("hasAnyRole('" + Roles.TEACHER + "','" + Roles.DIRECTOR + "','" + Roles.ADMIN + "')")
    public List<GradeResponse> batch(@Valid @RequestBody BatchGrades cmd) {
        return service.recordBatch(cmd);
    }

    @GetMapping("/students/{studentId}/summary")
    @PreAuthorize("hasAnyRole('" + Roles.TEACHER + "','" + Roles.DIRECTOR + "','" + Roles.SECRETARY + "','" + Roles.ADMIN + "','" + Roles.GUARDIAN + "','" + Roles.STUDENT + "')")
    public StudentGradeSummary summary(@PathVariable UUID studentId, @RequestParam UUID academicYearId) {
        return service.summaryFor(studentId, academicYearId);
    }
}
