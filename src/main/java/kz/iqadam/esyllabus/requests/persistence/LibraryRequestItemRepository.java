package kz.iqadam.esyllabus.requests.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LibraryRequestItemRepository extends JpaRepository<LibraryRequestItemEntity, Long> {

    List<LibraryRequestItemEntity> findByRequestIdOrderByLineNumberAsc(String requestId);

    void deleteByRequestId(String requestId);
}
