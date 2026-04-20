package kz.iqadam.esyllabus.integration.megapro;

import java.util.List;

public interface MegaProClient {

    List<MegaProBook> searchBooks(String query, int limit);
}
