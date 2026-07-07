package ao.skool.academic_structure.internal.service;

import ao.skool.academic_structure.internal.domain.School;
import ao.skool.academic_structure.internal.persistence.MunicipioRepository;
import ao.skool.academic_structure.internal.persistence.SchoolRepository;
import ao.skool.academic_structure.internal.web.dto.SchoolDtos.CreateSchool;
import ao.skool.academic_structure.internal.web.dto.SchoolDtos.SchoolResponse;
import ao.skool.common.web.error.ApplicationException;
import ao.skool.common.web.error.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SchoolService {

    private final SchoolRepository schools;
    private final MunicipioRepository municipios;

    public SchoolService(SchoolRepository schools, MunicipioRepository municipios) {
        this.schools = schools;
        this.municipios = municipios;
    }

    public SchoolResponse create(CreateSchool cmd) {
        if (!municipios.existsById(cmd.municipioId())) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, "error.validation");
        }
        schools.findByCode(cmd.code()).ifPresent(s -> {
            throw new ApplicationException(HttpStatus.CONFLICT, "error.conflict");
        });
        School school = new School(UUID.randomUUID(), cmd.name(), cmd.code(), cmd.municipioId(),
                cmd.comunaOuBairro(), cmd.addressLine1(), cmd.addressComplement());
        schools.save(school);
        return toResponse(school);
    }

    @Transactional(readOnly = true)
    public SchoolResponse get(UUID id) {
        return schools.findById(id).map(this::toResponse).orElseThrow(NotFoundException::new);
    }

    @Transactional(readOnly = true)
    public List<SchoolResponse> list() {
        return schools.findAll().stream().map(this::toResponse).toList();
    }

    private SchoolResponse toResponse(School s) {
        return new SchoolResponse(s.id(), s.name(), s.code(), s.municipioId(),
                s.comunaOuBairro(), s.addressLine1(), s.addressComplement(), s.active());
    }
}
