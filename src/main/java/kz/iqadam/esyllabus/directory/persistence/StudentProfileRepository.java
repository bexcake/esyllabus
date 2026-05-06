package kz.iqadam.esyllabus.directory.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentProfileRepository extends JpaRepository<StudentProfileEntity, String> {
}
