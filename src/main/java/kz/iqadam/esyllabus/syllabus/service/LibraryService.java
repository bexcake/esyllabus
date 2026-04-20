package kz.iqadam.esyllabus.syllabus.service;

import java.util.List;
import kz.iqadam.esyllabus.integration.megapro.MegaProBook;
import kz.iqadam.esyllabus.integration.megapro.MegaProClient;
import org.springframework.stereotype.Service;

@Service
public class LibraryService {

    private final MegaProClient megaProClient;

    public LibraryService(MegaProClient megaProClient) {
        this.megaProClient = megaProClient;
    }

    public List<MegaProBook> searchBooks(String query, int limit) {
        return megaProClient.searchBooks(query, Math.max(1, Math.min(limit, 100)));
    }
}
