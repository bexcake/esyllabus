package kz.iqadam.esyllabus.syllabus.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyllabusRepository extends JpaRepository<SyllabusEntity, String> {

    List<SyllabusEntity> findByOwnerEmailOrderByUpdatedAtDesc(String ownerEmail);

    List<SyllabusEntity> findByOwnerEmailAndCourseIdNotNullOrderByUpdatedAtDesc(String ownerEmail);

    java.util.Optional<SyllabusEntity> findTopByOwnerEmailAndCourseIdOrderByUpdatedAtDesc(String ownerEmail, String courseId);
}
