package ao.skool.academic_structure.internal.persistence;

import ao.skool.academic_structure.internal.domain.Province;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProvinceRepository extends JpaRepository<Province, String> {}
