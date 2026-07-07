package ao.skool.sis.internal.web;

import ao.skool.common.domain.SkoolPrincipal;
import ao.skool.common.security.Roles;
import ao.skool.common.web.error.ApplicationException;
import ao.skool.sis.internal.domain.Guardian;
import ao.skool.sis.internal.domain.Student;
import ao.skool.sis.internal.domain.StudentGuardianLink;
import ao.skool.sis.internal.persistence.GuardianRepository;
import ao.skool.sis.internal.persistence.StudentGuardianRepository;
import ao.skool.sis.internal.persistence.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Endpoints for a logged-in guardian to see their own linked children.
 * Distinct from {@link StudentController} which serves staff.
 */
@RestController
@RequestMapping("/api/guardians/me")
public class GuardianPortalController {

    private final GuardianRepository guardians;
    private final StudentRepository students;
    private final StudentGuardianRepository links;

    public GuardianPortalController(GuardianRepository guardians, StudentRepository students,
                                    StudentGuardianRepository links) {
        this.guardians = guardians;
        this.students = students;
        this.links = links;
    }

    public record ChildSummary(UUID id, String fullName, LocalDate dateOfBirth, String relationship) {}

    @GetMapping("/children")
    @PreAuthorize("hasRole('" + Roles.GUARDIAN + "')")
    public List<ChildSummary> myChildren(@AuthenticationPrincipal SkoolPrincipal principal) {
        Guardian guardian = guardians.findByUserId(UUID.fromString(principal.userId()))
                .orElseThrow(() -> new ApplicationException(HttpStatus.NOT_FOUND, "error.not_found"));
        List<StudentGuardianLink> myLinks = links.findByKeyGuardianId(guardian.id());
        if (myLinks.isEmpty()) return List.of();

        List<UUID> studentIds = myLinks.stream().map(StudentGuardianLink::studentId).toList();
        return students.findAllById(studentIds).stream()
                .map(s -> new ChildSummary(s.id(), s.fullName(), s.dateOfBirth(),
                        myLinks.stream()
                                .filter(l -> l.studentId().equals(s.id()))
                                .findFirst()
                                .map(l -> l.relationship().name())
                                .orElse(null)))
                .toList();
    }
}
