package com.lyftix.backend.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final int MAX_CORRELATION_ID_LENGTH = 128;
    private static final Pattern VALID_CORRELATION_ID =
            Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}");
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Map<String, String> previousContext = MDC.getCopyOfContextMap();
        String correlationId = resolveCorrelationId(request.getHeader(CORRELATION_ID_HEADER));
        long startedAt = System.nanoTime();

        MDC.put("correlationId", correlationId);
        MDC.put("httpMethod", request.getMethod());
        MDC.put("requestPath", request.getRequestURI());
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException exception) {
            LOGGER.atError()
                    .addKeyValue("exceptionType", exception.getClass().getName())
                    .setCause(exception)
                    .log("Unhandled exception during HTTP request");
            throw exception;
        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            LOGGER.atInfo()
                    .addKeyValue("responseStatus", response.getStatus())
                    .addKeyValue("durationMs", durationMs)
                    .log("HTTP request completed");
            restoreMdc(previousContext);
        }
    }

    private String resolveCorrelationId(String candidate) {
        if (candidate != null
                && candidate.length() <= MAX_CORRELATION_ID_LENGTH
                && VALID_CORRELATION_ID.matcher(candidate).matches()) {
            return candidate;
        }
        return UUID.randomUUID().toString();
    }

    private void restoreMdc(Map<String, String> previousContext) {
        MDC.clear();
        if (previousContext != null) {
            MDC.setContextMap(previousContext);
        }
    }
}
