package kz.iqadam.esyllabus.integration.megapro;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MegaProResourceCacheRepository extends JpaRepository<MegaProResourceCacheEntity, String> {

    List<MegaProResourceCacheEntity> findByCourseId(String courseId);

    void deleteByCourseId(String courseId);
}
