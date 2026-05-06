package kz.iqadam.esyllabus.directory.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolRepository extends JpaRepository<SchoolEntity, String> {

    Optional<SchoolEntity> findByDirectorUsername(String directorUsername);
}
