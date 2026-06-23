package kz.iqadam.esyllabus.integration.digital.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DigitalUniversityProgramRepository extends JpaRepository<DigitalUniversityProgramEntity, String> {

    Optional<DigitalUniversityProgramEntity> findByExternalProgramId(Long externalProgramId);
}
