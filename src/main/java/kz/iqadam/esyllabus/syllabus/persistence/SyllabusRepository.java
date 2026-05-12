package kz.iqadam.esyllabus.syllabus.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyllabusRepository extends JpaRepository<SyllabusEntity, String> {

    List<SyllabusEntity> findByOwnerEmailOrderByUpdatedAtDesc(String ownerEmail);

    List<SyllabusEntity> findByOwnerEmailAndCourseIdNotNullOrderByUpdatedAtDesc(String ownerEmail);

    List<SyllabusEntity> findByDirectorUsernameOrderByUpdatedAtDesc(String directorUsername);

    List<SyllabusEntity> findByStatusOrderByUpdatedAtDesc(kz.iqadam.esyllabus.syllabus.model.SyllabusStatus status);

    java.util.Optional<SyllabusEntity> findTopByOwnerEmailAndCourseIdOrderByUpdatedAtDesc(String ownerEmail, String courseId);

    java.util.Optional<SyllabusEntity> findTopByCourseIdAndStatusOrderByUpdatedAtDesc(
            String courseId,
            kz.iqadam.esyllabus.syllabus.model.SyllabusStatus status
    );
}
