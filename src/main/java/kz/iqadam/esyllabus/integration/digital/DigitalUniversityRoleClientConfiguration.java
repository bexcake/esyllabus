package kz.iqadam.esyllabus.integration.digital;

import java.net.http.HttpClient;
import java.util.Set;
import kz.iqadam.esyllabus.security.DigitalUniversityBearerTokenResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class DigitalUniversityRoleClientConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "digital-university", name = "enabled", havingValue = "true")
    DigitalUniversityBridgeClient httpDigitalUniversityBridgeClient(
            DigitalUniversityProperties properties,
            DigitalUniversityBearerTokenResolver tokenResolver
    ) {
        var httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();

        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());

        var restClient = RestClient.builder()
                .baseUrl(properties.baseUrl().toString())
                .requestFactory(requestFactory)
                .build();

        return new HttpDigitalUniversityBridgeClient(restClient, properties, tokenResolver);
    }

    @Bean
    @ConditionalOnProperty(prefix = "digital-university", name = "enabled", havingValue = "false", matchIfMissing = true)
    DigitalUniversityBridgeClient disabledDigitalUniversityBridgeClient() {
        return new DisabledDigitalUniversityBridgeClient();
    }

    @Bean
    @ConditionalOnProperty(prefix = "digital-university", name = "enabled", havingValue = "true")
    DigitalUniversityRoleClient httpDigitalUniversityRoleClient() {
        return email -> Set.of();
    }

    @Bean
    @ConditionalOnProperty(prefix = "digital-university", name = "enabled", havingValue = "false", matchIfMissing = true)
    DigitalUniversityRoleClient disabledDigitalUniversityRoleClient() {
        return email -> Set.of();
    }
}
