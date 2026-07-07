package ao.skool.academic_structure.internal.service;

import ao.skool.academic_structure.internal.domain.AcademicYear;
import ao.skool.academic_structure.internal.domain.Trimester;
import ao.skool.academic_structure.internal.persistence.AcademicYearRepository;
import ao.skool.academic_structure.internal.web.dto.AcademicYearDtos.AcademicYearResponse;
import ao.skool.academic_structure.internal.web.dto.AcademicYearDtos.CreateAcademicYear;
import ao.skool.academic_structure.internal.web.dto.AcademicYearDtos.TrimesterResponse;
import ao.skool.common.domain.tenant.TenantContext;
import ao.skool.common.web.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AcademicYearService {

    private final AcademicYearRepository years;
    private final TenantContext tenant;

    public AcademicYearService(AcademicYearRepository years, TenantContext tenant) {
        this.years = years;
        this.tenant = tenant;
    }

    public AcademicYearResponse create(CreateAcademicYear cmd) {
        UUID tenantId = tenant.current().value();
        AcademicYear year = new AcademicYear(UUID.randomUUID(), tenantId, cmd.name(), cmd.startDate(), cmd.endDate());
        if (cmd.trimesters() != null) {
            cmd.trimesters().forEach(t -> year.addTrimester(
                    new Trimester(UUID.randomUUID(), year.id(), t.key(), t.startDate(), t.endDate())));
        }
        years.save(year);
        return toResponse(year);
    }

    public AcademicYearResponse markCurrent(UUID yearId) {
        UUID tenantId = tenant.current().value();
        years.findByTenantIdAndCurrentTrue(tenantId).ifPresent(y -> y.markCurrent(false));
        AcademicYear year = years.findById(yearId).orElseThrow(NotFoundException::new);
        year.markCurrent(true);
        return toResponse(year);
    }

    @Transactional(readOnly = true)
    public List<AcademicYearResponse> list() {
        return years.findByTenantIdOrderByStartDateDesc(tenant.current().value())
                .stream().map(this::toResponse).toList();
    }

    private AcademicYearResponse toResponse(AcademicYear y) {
        List<TrimesterResponse> trimesters = y.trimesters().stream()
                .map(t -> new TrimesterResponse(t.id(), t.key(), t.startDate(), t.endDate()))
                .toList();
        return new AcademicYearResponse(y.id(), y.name(), y.startDate(), y.endDate(), y.current(), trimesters);
    }
}
