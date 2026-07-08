package ao.skool.assessment.internal.web;

import ao.skool.assessment.internal.service.QuestionService;
import ao.skool.assessment.internal.web.dto.AssessmentDtos.CreateQuestion;
import ao.skool.assessment.internal.web.dto.AssessmentDtos.QuestionResponse;
import ao.skool.common.security.Roles;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionService service;

    public QuestionController(QuestionService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("hasAnyRole('" + Roles.TEACHER + "','" + Roles.DIRECTOR + "','" + Roles.ADMIN + "')")
    public QuestionResponse create(@Valid @RequestBody CreateQuestion cmd) {
        return service.create(cmd);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('" + Roles.TEACHER + "','" + Roles.DIRECTOR + "','" + Roles.ADMIN + "')")
    public List<QuestionResponse> list(@RequestParam UUID subjectId,
                                       @RequestParam(required = false) String gradeLevel) {
        return service.listBySubject(subjectId, gradeLevel);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('" + Roles.TEACHER + "','" + Roles.DIRECTOR + "','" + Roles.ADMIN + "')")
    public QuestionResponse get(@PathVariable UUID id) {
        return service.get(id);
    }
}
