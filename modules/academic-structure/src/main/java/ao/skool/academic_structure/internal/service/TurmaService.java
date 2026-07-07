package ao.skool.academic_structure.internal.service;

import ao.skool.academic_structure.internal.domain.Turma;
import ao.skool.academic_structure.internal.persistence.TurmaRepository;
import ao.skool.academic_structure.internal.web.dto.TurmaDtos.CreateTurma;
import ao.skool.academic_structure.internal.web.dto.TurmaDtos.TurmaResponse;
import ao.skool.common.domain.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TurmaService {

    private final TurmaRepository turmas;
    private final TenantContext tenant;

    public TurmaService(TurmaRepository turmas, TenantContext tenant) {
        this.turmas = turmas;
        this.tenant = tenant;
    }

    public TurmaResponse create(CreateTurma cmd) {
        Turma turma = new Turma(UUID.randomUUID(), tenant.current().value(),
                cmd.schoolId(), cmd.academicYearId(),
                cmd.name(), cmd.gradeLevel(), cmd.track(), cmd.capacity());
        turmas.save(turma);
        return toResponse(turma);
    }

    @Transactional(readOnly = true)
    public List<TurmaResponse> list(UUID academicYearId) {
        return turmas.findByTenantIdAndAcademicYearIdOrderByGradeLevelAscNameAsc(
                tenant.current().value(), academicYearId)
                .stream().map(this::toResponse).toList();
    }

    private TurmaResponse toResponse(Turma t) {
        return new TurmaResponse(t.id(), t.schoolId(), t.academicYearId(),
                t.name(), t.gradeLevel(), t.track(), t.capacity());
    }
}
