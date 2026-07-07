package ao.skool.sis.internal.service;

import ao.skool.common.domain.tenant.TenantContext;
import ao.skool.common.web.error.NotFoundException;
import ao.skool.sis.internal.domain.Guardian;
import ao.skool.sis.internal.domain.Student;
import ao.skool.sis.internal.domain.StudentGuardianLink;
import ao.skool.sis.internal.persistence.GuardianRepository;
import ao.skool.sis.internal.persistence.StudentGuardianRepository;
import ao.skool.sis.internal.persistence.StudentRepository;
import ao.skool.sis.internal.web.dto.StudentDtos.CreateStudent;
import ao.skool.sis.internal.web.dto.StudentDtos.GuardianSummary;
import ao.skool.sis.internal.web.dto.StudentDtos.LinkGuardian;
import ao.skool.sis.internal.web.dto.StudentDtos.StudentResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class StudentService {

    private final StudentRepository students;
    private final GuardianRepository guardians;
    private final StudentGuardianRepository links;
    private final TenantContext tenant;

    public StudentService(StudentRepository students, GuardianRepository guardians,
                          StudentGuardianRepository links, TenantContext tenant) {
        this.students = students;
        this.guardians = guardians;
        this.links = links;
        this.tenant = tenant;
    }

    public StudentResponse create(CreateStudent cmd) {
        UUID tenantId = tenant.current().value();
        Student student = new Student(UUID.randomUUID(), tenantId, cmd.fullName(), cmd.dateOfBirth(), cmd.sex());
        if (cmd.bi() != null && !cmd.bi().isBlank()) student.setBi(cmd.bi());
        student.setAddress(cmd.comunaOuBairro(), cmd.addressLine1());
        if (cmd.healthNotes() != null) student.setHealthNotes(cmd.healthNotes());
        students.save(student);
        return toResponse(student, List.of());
    }

    public StudentResponse linkGuardian(UUID studentId, LinkGuardian cmd) {
        Student student = students.findById(studentId).orElseThrow(NotFoundException::new);
        Guardian guardian = guardians.findById(cmd.guardianId()).orElseThrow(NotFoundException::new);
        if (!student.tenantId().equals(guardian.tenantId())) {
            throw new NotFoundException();
        }
        StudentGuardianLink link = new StudentGuardianLink(
                student.id(), guardian.id(), cmd.relationship(),
                cmd.primaryGuardian(), cmd.emergencyContact());
        links.save(link);
        return get(student.id());
    }

    @Transactional(readOnly = true)
    public StudentResponse get(UUID id) {
        Student student = students.findById(id).orElseThrow(NotFoundException::new);
        List<StudentGuardianLink> studentLinks = links.findByKeyStudentId(student.id());
        return toResponse(student, resolveGuardians(studentLinks));
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> list() {
        return students.findByTenantIdOrderByFullNameAsc(tenant.current().value())
                .stream()
                .map(s -> toResponse(s, List.of()))
                .toList();
    }

    private List<GuardianSummary> resolveGuardians(List<StudentGuardianLink> studentLinks) {
        if (studentLinks.isEmpty()) return List.of();
        Map<UUID, Guardian> byId = guardians.findAllById(studentLinks.stream().map(StudentGuardianLink::guardianId).toList())
                .stream().collect(java.util.stream.Collectors.toMap(Guardian::id, g -> g));
        return studentLinks.stream()
                .map(link -> {
                    Guardian g = byId.get(link.guardianId());
                    return new GuardianSummary(g.id(), g.fullName(), g.phone(), g.email(),
                            link.relationship(), link.primaryGuardian());
                })
                .toList();
    }

    private StudentResponse toResponse(Student s, List<GuardianSummary> guardianList) {
        return new StudentResponse(
                s.id(), s.fullName(), s.dateOfBirth(), s.sex(), s.bi(),
                s.comunaOuBairro(), s.addressLine1(), s.healthNotes(), s.active(),
                guardianList
        );
    }
}
