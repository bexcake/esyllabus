package kz.iqadam.esyllabus.integration.digital;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DigitalUniversityStartupSyncRunner implements ApplicationRunner {

    private final DigitalUniversityDirectoryCacheService directoryCacheService;

    public DigitalUniversityStartupSyncRunner(
            DigitalUniversityDirectoryCacheService directoryCacheService
    ) {
        this.directoryCacheService = directoryCacheService;
    }

    @Override
    public void run(ApplicationArguments args) {
        directoryCacheService.requestRefreshWhenTokenAvailable("startup");
    }
}
