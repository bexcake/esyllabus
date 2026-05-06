package kz.iqadam.esyllabus.syllabus.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import kz.iqadam.esyllabus.integration.megapro.MegaProClient;
import kz.iqadam.esyllabus.integration.megapro.MegaProProperties;
import kz.iqadam.esyllabus.integration.megapro.MegaProResourceCacheEntity;
import kz.iqadam.esyllabus.integration.megapro.MegaProResourceCacheRepository;
import kz.iqadam.esyllabus.requests.service.LibraryRequestService;
import kz.iqadam.esyllabus.security.CurrentUser;
import kz.iqadam.esyllabus.syllabus.api.DisciplineCatalogItemResponse;
import kz.iqadam.esyllabus.syllabus.persistence.CourseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class LibraryService {

    private final MegaProClient megaProClient;
    private final MegaProProperties megaProProperties;
    private final CourseRepository courseRepository;
    private final MegaProResourceCacheRepository megaProResourceCacheRepository;
    private final LibraryRequestService libraryRequestService;

    public LibraryService(
            MegaProClient megaProClient,
            MegaProProperties megaProProperties,
            CourseRepository courseRepository,
            MegaProResourceCacheRepository megaProResourceCacheRepository,
            LibraryRequestService libraryRequestService
    ) {
        this.megaProClient = megaProClient;
        this.megaProProperties = megaProProperties;
        this.courseRepository = courseRepository;
        this.megaProResourceCacheRepository = megaProResourceCacheRepository;
        this.libraryRequestService = libraryRequestService;
    }

    @Transactional(readOnly = true)
    public List<BookSearchResult> searchBooks(String query, int limit) {
        var normalizedQuery = normalized(query);
        if (normalizedQuery == null) {
            return List.of();
        }

        int normalizedLimit = Math.max(1, Math.min(limit, 100));
        var cached = megaProResourceCacheRepository.findAll().stream()
                .filter(item -> matchesQuery(item, normalizedQuery))
                .sorted(Comparator.comparing(MegaProResourceCacheEntity::getSyncedAt).reversed())
                .limit(normalizedLimit)
                .map(item -> new BookSearchResult(
                        item.getExternalId(),
                        item.getTitle(),
                        item.getAuthor(),
                        item.getPublicationYear(),
                        item.getUrl(),
                        item.getType(),
                        item.getDiscipline(),
                        CourseMetadataSupport.parseCsv(item.getDisciplineTagsCsv()),
                        item.getSyncedAt()
                ))
                .toList();

        if (!cached.isEmpty()) {
            return cached;
        }

        return megaProClient.searchBooks(normalizedQuery, normalizedLimit).stream()
                .map(book -> new BookSearchResult(
                        book.externalId(),
                        book.title(),
                        book.author(),
                        book.year(),
                        book.url(),
                        book.type(),
                        null,
                        List.of(),
                        null
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DisciplineCatalogItemResponse> getDisciplines(String search) {
        var normalizedSearch = normalized(search);
        return courseRepository.findAll().stream()
                .filter(course -> normalizedSearch == null
                        || (course.getTitle() + " " + course.getProgram() + " " + course.getCode()).toLowerCase(Locale.ROOT)
                        .contains(normalizedSearch.toLowerCase(Locale.ROOT)))
                .sorted(Comparator.comparing(course -> course.getTitle().toLowerCase(Locale.ROOT)))
                .map(course -> {
                    var cached = megaProResourceCacheRepository.findByCourseId(course.getId());
                    var lastSynced = cached.stream()
                            .map(MegaProResourceCacheEntity::getSyncedAt)
                            .max(Comparator.naturalOrder())
                            .orElse(null);
                    return new DisciplineCatalogItemResponse(
                            course.getId(),
                            course.getTitle(),
                            course.getCode(),
                            course.getProgram(),
                            course.getSchoolId(),
                            CourseMetadataSupport.parseCsv(course.getDisciplineTagsCsv()),
                            cached.size(),
                            lastSynced
                    );
                })
                .toList();
    }

    public MegaProSyncReport syncMegaProCatalog(CurrentUser user) {
        if (!user.hasAnyRole("LIBRARIAN", "DIRECTOR")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only librarian or director can synchronize MegaPro");
        }
        return synchronizeAllCoursesFromMegaPro();
    }

    public MegaProSyncReport synchronizeAllCoursesFromMegaPro() {
        if (!megaProProperties.enabled()) {
            return new MegaProSyncReport(0, 0, Instant.now(), "Megapro integration is disabled");
        }
        if (!megaProProperties.syncEnabled()) {
            return new MegaProSyncReport(0, 0, Instant.now(), "Megapro synchronization is disabled");
        }

        int booksCached = 0;
        var syncedAt = Instant.now();
        var courses = courseRepository.findAll();
        int limit = Math.max(1, Math.min(megaProProperties.syncLimitPerCourse(), 20));

        for (var course : courses) {
            megaProResourceCacheRepository.deleteByCourseId(course.getId());
            var books = megaProClient.searchBooks(course.getTitle(), limit);
            for (var book : books) {
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
                booksCached++;
            }
        }

        return new MegaProSyncReport(courses.size(), booksCached, syncedAt, "Megapro synchronization completed");
    }

    @Transactional(readOnly = true)
    public byte[] exportRequests(CurrentUser user) {
        return libraryRequestService.exportRequestsForLibrary(user);
    }

    private boolean matchesQuery(MegaProResourceCacheEntity entity, String query) {
        var tags = CourseMetadataSupport.parseCsv(entity.getDisciplineTagsCsv());
        var searchable = String.join(" ", List.of(
                safe(entity.getTitle()),
                safe(entity.getAuthor()),
                safe(entity.getDiscipline()),
                String.join(" ", tags)
        )).toLowerCase(Locale.ROOT);
        return searchable.contains(query.toLowerCase(Locale.ROOT));
    }

    private String normalized(String value) {
        if (value == null) {
            return null;
        }
        var result = value.trim();
        return result.isBlank() ? null : result;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public record BookSearchResult(
            String externalId,
            String title,
            String author,
            String year,
            String url,
            String type,
            String discipline,
            List<String> disciplineTags,
            Instant syncedAt
    ) {
    }

    public record MegaProSyncReport(
            int disciplinesSynchronized,
            int booksCached,
            Instant syncedAt,
            String message
    ) {
    }
}
