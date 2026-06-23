package kz.iqadam.esyllabus.directory.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffProfileRepository extends JpaRepository<StaffProfileEntity, String> {

    Optional<StaffProfileEntity> findByUsername(String username);

    Optional<StaffProfileEntity> findByEmailIgnoreCase(String email);

    List<StaffProfileEntity> findBySchoolIdOrderByFullNameAsc(String schoolId);
}
