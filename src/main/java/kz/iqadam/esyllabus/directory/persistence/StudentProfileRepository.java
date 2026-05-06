package kz.iqadam.esyllabus.directory.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentProfileRepository extends JpaRepository<StudentProfileEntity, String> {

    Optional<StudentProfileEntity> findByUsername(String username);
}
