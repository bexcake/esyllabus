package kz.iqadam.esyllabus.security;

import kz.iqadam.esyllabus.config.ApplicationSecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            MicrosoftOAuth2UserService microsoftOAuth2UserService,
            ApplicationSecurityProperties properties
    ) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/error", "/api/public/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(microsoftOAuth2UserService))
                        .defaultSuccessUrl(properties.postLoginRedirectUri().toString(), true)
                )
                .logout(logout -> logout.logoutSuccessUrl("/"))
                .exceptionHandling(exceptions -> exceptions.accessDeniedPage("/api/auth/access-denied"))
                .csrf(Customizer.withDefaults());

        return http.build();
    }
}
