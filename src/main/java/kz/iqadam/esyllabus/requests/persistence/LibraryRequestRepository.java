package kz.iqadam.esyllabus.requests.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LibraryRequestRepository extends JpaRepository<LibraryRequestEntity, String> {

    List<LibraryRequestEntity> findByRequesterUsernameOrderByUpdatedAtDesc(String requesterUsername);

    List<LibraryRequestEntity> findByDirectorUsernameOrderByUpdatedAtDesc(String directorUsername);

    java.util.Optional<LibraryRequestEntity> findBySyllabusId(String syllabusId);

    List<LibraryRequestEntity> findByStatusInOrderByUpdatedAtDesc(java.util.Collection<kz.iqadam.esyllabus.requests.model.LibraryRequestStatus> statuses);
}
