package kz.iqadam.esyllabus.integration.digital;

import java.net.http.HttpClient;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class DigitalUniversityRoleClientConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "digital-university", name = "enabled", havingValue = "true")
    DigitalUniversityRoleClient httpDigitalUniversityRoleClient(DigitalUniversityProperties properties) {
        var httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();

        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());

        var restClient = RestClient.builder()
                .baseUrl(properties.baseUrl().toString())
                .requestFactory(requestFactory)
                .defaultHeader(properties.authHeaderName(), properties.authHeaderValue())
                .build();

        return new HttpDigitalUniversityRoleClient(restClient, properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "digital-university", name = "enabled", havingValue = "false", matchIfMissing = true)
    DigitalUniversityRoleClient disabledDigitalUniversityRoleClient() {
        return email -> Set.of();
    }
}
