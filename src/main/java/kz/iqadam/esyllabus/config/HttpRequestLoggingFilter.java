package kz.iqadam.esyllabus.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(10)
public class HttpRequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HttpRequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long startedAt = System.nanoTime();
        Exception failure = null;

        try {
            filterChain.doFilter(request, response);
        } catch (Exception exception) {
            failure = exception;
            throw exception;
        } finally {
            logRequest(request, response, startedAt, failure);
        }
    }

    private void logRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            long startedAt,
            Exception failure
    ) {
        var durationMs = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
        var query = request.getQueryString();
        var requestTarget = query == null || query.isBlank()
                ? request.getRequestURI()
                : request.getRequestURI() + "?" + query;
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var username = resolveUsername(authentication);
        var remoteIp = blankToDash(request.getRemoteAddr());
        var forwardedFor = blankToDash(request.getHeader("X-Forwarded-For"));
        var userAgent = blankToDash(request.getHeader("User-Agent"));
        var referer = blankToDash(request.getHeader("Referer"));

        if (failure == null) {
            log.info(
                    "http_request method={} path=\"{}\" status={} durationMs={} user={} remoteIp={} forwardedFor=\"{}\" userAgent=\"{}\" referer=\"{}\"",
                    request.getMethod(),
                    requestTarget,
                    response.getStatus(),
                    durationMs,
                    username,
                    remoteIp,
                    forwardedFor,
                    userAgent,
                    referer
            );
            return;
        }

        log.warn(
                "http_request method={} path=\"{}\" status={} durationMs={} user={} remoteIp={} forwardedFor=\"{}\" userAgent=\"{}\" referer=\"{}\" errorType={} errorMessage=\"{}\"",
                request.getMethod(),
                requestTarget,
                response.getStatus(),
                durationMs,
                username,
                remoteIp,
                forwardedFor,
                userAgent,
                referer,
                failure.getClass().getSimpleName(),
                sanitize(failure.getMessage())
        );
    }

    private String resolveUsername(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "anonymous";
        }
        return blankToDash(authentication.getName());
    }

    private String blankToDash(String value) {
        if (value == null) {
            return "-";
        }
        var trimmed = value.trim();
        return trimmed.isEmpty() ? "-" : trimmed;
    }

    private String sanitize(String value) {
        return blankToDash(value).replace("\"", "'");
    }
}
