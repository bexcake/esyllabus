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
        if (megaProProperties.enabled()) {
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
                var entity = megaProResourceCacheRepository.findByExternalId(book.externalId())
                        .orElseGet(MegaProResourceCacheEntity::new);
                if (entity.getId() == null) {
                    entity.setId("megapro-cache-" + UUID.randomUUID());
                }
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
                        new SeedBook("mp-policy-002", "Design for Policy", "Christian Bason", "2023", "https://megapro.local/design-for-policy", "Supplementary"),
                        new SeedBook("mp-policy-ru-001", "Анализ государственной политики", "Уильям Данн", "2022", "https://megapro.local/analiz-gosudarstvennoy-politiki", "Учебник"),
                        new SeedBook("mp-policy-kz-001", "Мемлекеттік саясатты талдау", "Уильям Данн", "2023", "https://megapro.local/memlekettik-sayasatty-taldau", "Оқулық")
                ),
                "eco-214", List.of(
                        new SeedBook("mp-eco-001", "Macroeconomics", "N. Gregory Mankiw", "2024", "https://megapro.local/macroeconomics", "Textbook"),
                        new SeedBook("mp-eco-002", "Monetary Economics and Strategy", "Frederic Mishkin", "2023", "https://megapro.local/monetary-economics-strategy", "Supplementary"),
                        new SeedBook("mp-eco-ru-001", "Макроэкономика", "Н. Грегори Мэнкью", "2021", "https://megapro.local/makroekonomika", "Учебник"),
                        new SeedBook("mp-eco-kz-001", "Макроэкономика негіздері", "Н. Грегори Мэнкью", "2022", "https://megapro.local/makroekonomika-negizderi", "Оқулық")
                ),
                "cs-540", List.of(
                        new SeedBook("mp-cs-001", "Hands-On Machine Learning with Scikit-Learn, Keras, and TensorFlow", "Aurelien Geron", "2023", "https://megapro.local/hands-on-machine-learning", "Textbook"),
                        new SeedBook("mp-cs-002", "Machine Learning Engineering", "Andriy Burkov", "2024", "https://megapro.local/machine-learning-engineering", "Supplementary"),
                        new SeedBook("mp-cs-ru-001", "Машинное обучение", "Андрей Бурков", "2022", "https://megapro.local/mashinnoe-obuchenie", "Учебник"),
                        new SeedBook("mp-cs-kz-001", "Машиналық оқыту негіздері", "Андрей Бурков", "2023", "https://megapro.local/mashinalyk-okytu-negizderi", "Оқулық")
                ),
                "bus-415", List.of(
                        new SeedBook("mp-bus-001", "Operations Management", "Nigel Slack", "2024", "https://megapro.local/operations-management", "Textbook"),
                        new SeedBook("mp-bus-002", "Strategic Operations", "Robert Hayes", "2022", "https://megapro.local/strategic-operations", "Supplementary"),
                        new SeedBook("mp-bus-ru-001", "Операционный менеджмент", "Найджел Слэк", "2021", "https://megapro.local/operatsionnyy-menedzhment", "Учебник"),
                        new SeedBook("mp-bus-kz-001", "Операциялық менеджмент", "Найджел Слэк", "2022", "https://megapro.local/operatsiyalyk-menedzhment", "Оқулық")
                ),
                "edu-601", List.of(
                        new SeedBook("mp-edu-001", "Inclusive Education in Action", "Tony Booth", "2023", "https://megapro.local/inclusive-education-action", "Textbook"),
                        new SeedBook("mp-edu-002", "Curriculum Design for Equity", "Deborah Meier", "2024", "https://megapro.local/curriculum-design-equity", "Supplementary"),
                        new SeedBook("mp-edu-ru-001", "Инклюзивное образование", "Тони Бут", "2022", "https://megapro.local/inklyuzivnoe-obrazovanie", "Учебник"),
                        new SeedBook("mp-edu-kz-001", "Инклюзивті білім беру", "Тони Бут", "2023", "https://megapro.local/inklyuzivti-bilim-beru", "Оқулық")
                ),
                "law-331", List.of(
                        new SeedBook("mp-law-001", "Comparative Constitutional Law", "Tom Ginsburg", "2024", "https://megapro.local/comparative-constitutional-law", "Textbook"),
                        new SeedBook("mp-law-002", "The Global Model of Constitutional Rights", "Kai Moller", "2023", "https://megapro.local/global-model-constitutional-rights", "Supplementary"),
                        new SeedBook("mp-law-ru-001", "Сравнительное конституционное право", "Том Гинсбург", "2022", "https://megapro.local/sravnitelnoe-konstitutsionnoe-pravo", "Учебник"),
                        new SeedBook("mp-law-kz-001", "Салыстырмалы конституциялық құқық", "Том Гинсбург", "2023", "https://megapro.local/salystyrmaly-konstitutsiyalyk-kukyk", "Оқулық")
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
