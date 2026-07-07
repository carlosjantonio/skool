package ao.skool.academic_structure.internal.web;

import ao.skool.academic_structure.internal.domain.CurricularTrack;
import ao.skool.academic_structure.internal.domain.GradeLevel;
import ao.skool.academic_structure.internal.persistence.MunicipioRepository;
import ao.skool.academic_structure.internal.persistence.ProvinceRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reference")
public class ReferenceController {

    private final ProvinceRepository provinces;
    private final MunicipioRepository municipios;

    public ReferenceController(ProvinceRepository provinces, MunicipioRepository municipios) {
        this.provinces = provinces;
        this.municipios = municipios;
    }

    public record ProvinceView(String code, String name) {}
    public record MunicipioView(UUID id, String provinciaCode, String name) {}

    @GetMapping("/provinces")
    public List<ProvinceView> provinces() {
        return provinces.findAll().stream()
                .map(p -> new ProvinceView(p.code(), p.name()))
                .sorted((a, b) -> a.name().compareTo(b.name()))
                .toList();
    }

    @GetMapping("/provinces/{code}/municipios")
    public List<MunicipioView> municipios(@PathVariable String code) {
        return municipios.findByProvinciaCodeOrderByNameAsc(code).stream()
                .map(m -> new MunicipioView(m.id(), m.provinciaCode(), m.name()))
                .toList();
    }

    @GetMapping("/grade-levels")
    public List<GradeLevel> gradeLevels() {
        return List.of(GradeLevel.values());
    }

    @GetMapping("/curricular-tracks")
    public List<CurricularTrack> curricularTracks() {
        return List.of(CurricularTrack.values());
    }
}
