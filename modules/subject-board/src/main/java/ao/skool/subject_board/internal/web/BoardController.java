package ao.skool.subject_board.internal.web;

import ao.skool.common.security.Roles;
import ao.skool.subject_board.internal.service.BoardService;
import ao.skool.subject_board.internal.web.dto.BoardDtos.CreateEntry;
import ao.skool.subject_board.internal.web.dto.BoardDtos.EntryResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/board")
public class BoardController {

    private static final String TEACHER_ROLES = "hasAnyRole('" + Roles.TEACHER + "','" + Roles.DIRECTOR + "','" + Roles.ADMIN + "')";
    private static final String ANY_LOGGED_IN = "hasAnyRole('" + Roles.STUDENT + "','" + Roles.TEACHER + "','"
            + Roles.DIRECTOR + "','" + Roles.SECRETARY + "','" + Roles.ADMIN + "','" + Roles.GUARDIAN + "')";

    private final BoardService service;

    public BoardController(BoardService service) { this.service = service; }

    @PostMapping
    @PreAuthorize(TEACHER_ROLES)
    public EntryResponse create(@Valid @RequestBody CreateEntry cmd) {
        return service.create(cmd);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(TEACHER_ROLES)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    @GetMapping
    @PreAuthorize(ANY_LOGGED_IN)
    public List<EntryResponse> feed(@RequestParam(required = false) UUID subjectId,
                                     @RequestParam UUID turmaId) {
        return subjectId != null
                ? service.feedForSubject(subjectId, turmaId)
                : service.feedForTurma(turmaId);
    }
}
