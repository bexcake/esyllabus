package kz.iqadam.esyllabus.integration.megapro;

import kz.iqadam.esyllabus.syllabus.service.LibraryService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MegaProSyncScheduler {

    private final LibraryService libraryService;

    public MegaProSyncScheduler(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @Scheduled(cron = "${megapro.sync-cron:0 0 3 * * *}")
    public void synchronizeDaily() {
        libraryService.synchronizeAllCoursesFromMegaPro();
    }
}
