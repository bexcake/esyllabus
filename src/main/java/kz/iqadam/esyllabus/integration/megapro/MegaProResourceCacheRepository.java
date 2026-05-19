package kz.iqadam.esyllabus.integration.megapro;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MegaProResourceCacheRepository extends JpaRepository<MegaProResourceCacheEntity, String> {

    List<MegaProResourceCacheEntity> findByCourseId(String courseId);

    Optional<MegaProResourceCacheEntity> findByExternalId(String externalId);

    void deleteByCourseId(String courseId);
}
