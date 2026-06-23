package kz.iqadam.esyllabus.security;

import kz.iqadam.esyllabus.config.ApplicationSecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ApplicationSecurityProperties properties,
            DigitalUniversityBearerAuthenticationFilter digitalUniversityBearerAuthenticationFilter
    ) throws Exception {
        if (properties.users() == null || properties.users().isEmpty()) {
            throw new IllegalStateException("At least one app.security user must be configured");
        }

        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/",
                                "/error",
                                "/api/public/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults())
                .addFilterBefore(digitalUniversityBearerAuthenticationFilter, BasicAuthenticationFilter.class)
                .logout(logout -> logout.logoutSuccessUrl("/"))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(
            ApplicationSecurityProperties properties,
            PasswordEncoder passwordEncoder
    ) {
        var users = properties.users().stream()
                .map(user -> User.withUsername(user.username())
                        .password(passwordEncoder.encode(user.password()))
                        .authorities(user.roles().stream()
                                .map(RoleNormalizer::toAuthority)
                                .toArray(String[]::new))
                        .build())
                .toList();

        return new InMemoryUserDetailsManager(users);
    }
}
