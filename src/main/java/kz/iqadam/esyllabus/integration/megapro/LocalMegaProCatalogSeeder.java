package kz.iqadam.esyllabus.integration.megapro;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import kz.iqadam.esyllabus.syllabus.persistence.CourseRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(30)
public class LocalMegaProCatalogSeeder implements CommandLineRunner {

    private final MegaProProperties megaProProperties;
    private final MegaProResourceCacheRepository megaProResourceCacheRepository;
    private final CourseRepository courseRepository;

    public LocalMegaProCatalogSeeder(
            MegaProProperties megaProProperties,
            MegaProResourceCacheRepository megaProResourceCacheRepository,
            CourseRepository courseRepository
    ) {
        this.megaProProperties = megaProProperties;
        this.megaProResourceCacheRepository = megaProResourceCacheRepository;
        this.courseRepository = courseRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (megaProProperties.enabled() || megaProResourceCacheRepository.count() > 0) {
            return;
        }

        var coursesById = courseRepository.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(course -> course.getId(), course -> course, (left, right) -> left));
        var syncedAt = Instant.now();

        for (var entry : seedBooks().entrySet()) {
            var course = coursesById.get(entry.getKey());
            if (course == null) {
                continue;
            }
            for (var book : entry.getValue()) {
                var entity = new MegaProResourceCacheEntity();
                entity.setId("megapro-cache-" + UUID.randomUUID());
                entity.setCourseId(course.getId());
                entity.setDiscipline(course.getTitle());
                entity.setDisciplineTagsCsv(course.getDisciplineTagsCsv());
                entity.setExternalId(book.externalId());
                entity.setTitle(book.title());
                entity.setAuthor(book.author());
                entity.setPublicationYear(book.year());
                entity.setUrl(book.url());
                entity.setType(book.type());
                entity.setSyncedAt(syncedAt);
                megaProResourceCacheRepository.save(entity);
            }
        }
    }

    private Map<String, List<SeedBook>> seedBooks() {
        return Map.of(
                "syllabus-public-policy-2026", List.of(
                        new SeedBook("mp-policy-001", "Public Policy Analysis", "William N. Dunn", "2024", "https://megapro.local/public-policy-analysis", "Textbook"),
                        new SeedBook("mp-policy-002", "Design for Policy", "Christian Bason", "2023", "https://megapro.local/design-for-policy", "Supplementary")
                ),
                "eco-214", List.of(
                        new SeedBook("mp-eco-001", "Macroeconomics", "N. Gregory Mankiw", "2024", "https://megapro.local/macroeconomics", "Textbook"),
                        new SeedBook("mp-eco-002", "Monetary Economics and Strategy", "Frederic Mishkin", "2023", "https://megapro.local/monetary-economics-strategy", "Supplementary")
                ),
                "cs-540", List.of(
                        new SeedBook("mp-cs-001", "Hands-On Machine Learning with Scikit-Learn, Keras, and TensorFlow", "Aurelien Geron", "2023", "https://megapro.local/hands-on-machine-learning", "Textbook"),
                        new SeedBook("mp-cs-002", "Machine Learning Engineering", "Andriy Burkov", "2024", "https://megapro.local/machine-learning-engineering", "Supplementary")
                ),
                "bus-415", List.of(
                        new SeedBook("mp-bus-001", "Operations Management", "Nigel Slack", "2024", "https://megapro.local/operations-management", "Textbook"),
                        new SeedBook("mp-bus-002", "Strategic Operations", "Robert Hayes", "2022", "https://megapro.local/strategic-operations", "Supplementary")
                ),
                "edu-601", List.of(
                        new SeedBook("mp-edu-001", "Inclusive Education in Action", "Tony Booth", "2023", "https://megapro.local/inclusive-education-action", "Textbook"),
                        new SeedBook("mp-edu-002", "Curriculum Design for Equity", "Deborah Meier", "2024", "https://megapro.local/curriculum-design-equity", "Supplementary")
                ),
                "law-331", List.of(
                        new SeedBook("mp-law-001", "Comparative Constitutional Law", "Tom Ginsburg", "2024", "https://megapro.local/comparative-constitutional-law", "Textbook"),
                        new SeedBook("mp-law-002", "The Global Model of Constitutional Rights", "Kai Moller", "2023", "https://megapro.local/global-model-constitutional-rights", "Supplementary")
                )
        );
    }

    private record SeedBook(
            String externalId,
            String title,
            String author,
            String year,
            String url,
            String type
    ) {
    }
}
