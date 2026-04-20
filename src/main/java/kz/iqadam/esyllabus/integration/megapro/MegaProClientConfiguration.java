package kz.iqadam.esyllabus.integration.megapro;

import java.net.http.HttpClient;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class MegaProClientConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "megapro", name = "enabled", havingValue = "true")
    MegaProClient httpMegaProClient(MegaProProperties properties) {
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

        return new HttpMegaProClient(restClient, properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "megapro", name = "enabled", havingValue = "false", matchIfMissing = true)
    MegaProClient disabledMegaProClient() {
        return (query, limit) -> List.of();
    }
}
