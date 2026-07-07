package ao.skool.academic_structure.internal.service;

import ao.skool.academic_structure.internal.domain.Subject;
import ao.skool.academic_structure.internal.persistence.SubjectRepository;
import ao.skool.academic_structure.internal.web.dto.SubjectDtos.CreateSubject;
import ao.skool.academic_structure.internal.web.dto.SubjectDtos.SubjectResponse;
import ao.skool.common.domain.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SubjectService {

    private final SubjectRepository subjects;
    private final TenantContext tenant;

    public SubjectService(SubjectRepository subjects, TenantContext tenant) {
        this.subjects = subjects;
        this.tenant = tenant;
    }

    public SubjectResponse create(CreateSubject cmd) {
        Subject subject = new Subject(UUID.randomUUID(), tenant.current().value(),
                cmd.name(), cmd.code(), cmd.gradeLevel());
        subjects.save(subject);
        return toResponse(subject);
    }

    @Transactional(readOnly = true)
    public List<SubjectResponse> list() {
        return subjects.findByTenantIdOrderByGradeLevelAscNameAsc(tenant.current().value())
                .stream().map(this::toResponse).toList();
    }

    private SubjectResponse toResponse(Subject s) {
        return new SubjectResponse(s.id(), s.name(), s.code(), s.gradeLevel());
    }
}
