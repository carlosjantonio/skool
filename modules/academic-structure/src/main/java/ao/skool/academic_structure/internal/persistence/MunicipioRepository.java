package ao.skool.academic_structure.internal.persistence;

import ao.skool.academic_structure.internal.domain.Municipio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MunicipioRepository extends JpaRepository<Municipio, UUID> {
    List<Municipio> findByProvinciaCodeOrderByNameAsc(String provinciaCode);
}
