package kz.iqadam.esyllabus.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import kz.iqadam.esyllabus.integration.digital.DigitalUniversityDirectoryCacheService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class DigitalUniversityBearerAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(DigitalUniversityBearerAuthenticationFilter.class);

    private final DigitalUniversityJwtService jwtService;
    private final DigitalUniversityUserProvisioningService userProvisioningService;
    private final DigitalUniversityDirectoryCacheService directoryCacheService;
    private final ObjectMapper objectMapper;

    public DigitalUniversityBearerAuthenticationFilter(
            DigitalUniversityJwtService jwtService,
            DigitalUniversityUserProvisioningService userProvisioningService,
            DigitalUniversityDirectoryCacheService directoryCacheService,
            ObjectMapper objectMapper
    ) {
        this.jwtService = jwtService;
        this.userProvisioningService = userProvisioningService;
        this.directoryCacheService = directoryCacheService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        var token = bearerToken(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            var claims = jwtService.verify(token);
            var user = userProvisioningService.provision(claims, token);
            var authorities = user.roles().stream()
                    .map(RoleNormalizer::toAuthority)
                    .map(SimpleGrantedAuthority::new)
                    .toList();
            var authentication = new UsernamePasswordAuthenticationToken(user.email(), token, authorities);
            authentication.setDetails(user);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            refreshDirectoryCache(token);
            filterChain.doFilter(request, response);
        } catch (BadCredentialsException exception) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), Map.of(
                    "message", "Invalid Digital University bearer token"
            ));
        }
    }

    private void refreshDirectoryCache(String token) {
        try {
            directoryCacheService.refreshReferenceDataIfStale(token);
        } catch (RuntimeException exception) {
            log.warn("digital_university_cache_refresh_failed message=\"{}\"", exception.getMessage());
        }
    }

    private String bearerToken(HttpServletRequest request) {
        var authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        var token = authorization.substring(7).trim();
        return token.isBlank() ? null : token;
    }
}
