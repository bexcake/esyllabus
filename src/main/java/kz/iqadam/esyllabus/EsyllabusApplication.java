package kz.iqadam.esyllabus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class EsyllabusApplication {

    public static void main(String[] args) {
        SpringApplication.run(EsyllabusApplication.class, args);
    }

}
