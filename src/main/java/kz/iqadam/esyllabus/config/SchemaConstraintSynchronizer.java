package kz.iqadam.esyllabus.config;

import java.util.Arrays;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import kz.iqadam.esyllabus.syllabus.model.SyllabusStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class SchemaConstraintSynchronizer {

    private static final Logger log = LoggerFactory.getLogger(SchemaConstraintSynchronizer.class);

    @Bean
    ApplicationRunner synchronizeSyllabusStatusConstraint(DataSource dataSource) {
        return args -> {
            var jdbcTemplate = new JdbcTemplate(dataSource);
            synchronizeSyllabiStatusConstraint(jdbcTemplate);
        };
    }

    private void synchronizeSyllabiStatusConstraint(JdbcTemplate jdbcTemplate) {
        var allowedStatuses = Arrays.stream(SyllabusStatus.values())
                .map(Enum::name)
                .map(value -> "'" + value + "'")
                .collect(Collectors.joining(", "));

        try {
            jdbcTemplate.execute("alter table syllabi drop constraint if exists syllabi_status_check");
            jdbcTemplate.execute(
                    "alter table syllabi add constraint syllabi_status_check " +
                            "check (status in (" + allowedStatuses + "))"
            );
            log.info("Synchronized syllabi_status_check with current SyllabusStatus enum values");
        } catch (Exception exception) {
            log.warn("Unable to synchronize syllabi_status_check: {}", exception.getMessage());
        }
    }
}
