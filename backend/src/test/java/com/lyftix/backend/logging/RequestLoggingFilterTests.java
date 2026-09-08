package com.lyftix.backend.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestLoggingFilterTests {

    private final RequestLoggingFilter filter = new RequestLoggingFilter();
    private final Logger logger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachLogAppender() {
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        MDC.clear();
    }

    @AfterEach
    void detachLogAppender() {
        logger.detachAppender(appender);
        appender.stop();
        MDC.clear();
    }

    @Test
    void generatesUuidAndReturnsItWhenRequestHasNoCorrelationId() throws Exception {
        MockHttpServletResponse response = execute(new MockHttpServletRequest("GET", "/api/health"));

        String correlationId = response.getHeader(RequestLoggingFilter.CORRELATION_ID_HEADER);
        assertThat(correlationId).isNotNull();
        assertThat(UUID.fromString(correlationId).toString()).isEqualTo(correlationId);
    }

    @Test
    void preservesValidIncomingCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
        request.addHeader(RequestLoggingFilter.CORRELATION_ID_HEADER, "client-request_123:retry.2");

        MockHttpServletResponse response = execute(request);

        assertThat(response.getHeader(RequestLoggingFilter.CORRELATION_ID_HEADER))
                .isEqualTo("client-request_123:retry.2");
    }

    @Test
    void replacesOverlongCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
        request.addHeader(RequestLoggingFilter.CORRELATION_ID_HEADER, "a".repeat(129));

        MockHttpServletResponse response = execute(request);

        assertThat(response.getHeader(RequestLoggingFilter.CORRELATION_ID_HEADER))
                .matches("[0-9a-f-]{36}");
    }

    @Test
    void replacesCorrelationIdContainingUnsafeCharacters() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
        request.addHeader(RequestLoggingFilter.CORRELATION_ID_HEADER, "unsafe correlation id");

        MockHttpServletResponse response = execute(request);

        assertThat(response.getHeader(RequestLoggingFilter.CORRELATION_ID_HEADER))
                .matches("[0-9a-f-]{36}");
    }

    @Test
    void clearsRequestMdcAfterCompletion() throws Exception {
        MDC.put("existing", "preserved");

        execute(new MockHttpServletRequest("GET", "/api/health"));

        assertThat(MDC.get("correlationId")).isNull();
        assertThat(MDC.get("httpMethod")).isNull();
        assertThat(MDC.get("requestPath")).isNull();
        assertThat(MDC.get("existing")).isEqualTo("preserved");
    }

    @Test
    void successfulRequestProducesOneCompletionLogWithRequestFields() throws Exception {
        execute(new MockHttpServletRequest("GET", "/api/health"));

        assertThat(appender.list).hasSize(1);
        ILoggingEvent event = appender.list.getFirst();
        assertThat(event.getLevel()).isEqualTo(Level.INFO);
        assertThat(event.getFormattedMessage()).isEqualTo("HTTP request completed");
        assertThat(event.getMDCPropertyMap())
                .containsEntry("httpMethod", "GET")
                .containsEntry("requestPath", "/api/health")
                .containsKey("correlationId");
        assertThat(event.getKeyValuePairs())
                .anySatisfy(pair -> {
                    assertThat(pair.key).isEqualTo("responseStatus");
                    assertThat(pair.value).isEqualTo(200);
                })
                .anySatisfy(pair -> {
                    assertThat(pair.key).isEqualTo("durationMs");
                    assertThat(pair.value).isInstanceOf(Long.class);
                });
    }

    @Test
    void failedRequestPreservesCorrelationIdAndLogsFinalStatus() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/workouts");
        request.addHeader(RequestLoggingFilter.CORRELATION_ID_HEADER, "failed-request-42");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                ((MockHttpServletResponse) servletResponse).setStatus(HttpStatus.BAD_REQUEST.value()));

        assertThat(response.getHeader(RequestLoggingFilter.CORRELATION_ID_HEADER))
                .isEqualTo("failed-request-42");
        assertThat(appender.list.getFirst().getMDCPropertyMap())
                .containsEntry("correlationId", "failed-request-42");
        assertThat(appender.list.getFirst().getKeyValuePairs())
                .anySatisfy(pair -> {
                    assertThat(pair.key).isEqualTo("responseStatus");
                    assertThat(pair.value).isEqualTo(400);
                });
    }

    @Test
    void requestLoggingDoesNotIncludeSensitiveHeadersCookiesOrBodies() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/workouts");
        request.addHeader("Authorization", "Bearer secret-token-value");
        request.setCookies(new Cookie("session", "secret-cookie-value"));
        request.setContent("password=secret-body-value".getBytes(StandardCharsets.UTF_8));

        execute(request);

        String capturedEvent = appender.list.getFirst().getFormattedMessage()
                + appender.list.getFirst().getMDCPropertyMap();
        assertThat(capturedEvent)
                .doesNotContain("Authorization")
                .doesNotContain("secret-token-value")
                .doesNotContain("secret-cookie-value")
                .doesNotContain("secret-body-value")
                .doesNotContain("password");
    }

    @Test
    void escapingExceptionIsLoggedWithContextAndMdcIsCleared() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/failure");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            throw new ServletException("failure");
        })).isInstanceOf(ServletException.class);

        assertThat(appender.list).hasSize(2);
        ILoggingEvent error = appender.list.getFirst();
        assertThat(error.getLevel()).isEqualTo(Level.ERROR);
        assertThat(error.getThrowableProxy().getClassName()).isEqualTo(ServletException.class.getName());
        assertThat(error.getMDCPropertyMap())
                .containsEntry("httpMethod", "GET")
                .containsEntry("requestPath", "/api/failure")
                .containsKeys("correlationId");
        assertThat(error.getKeyValuePairs())
                .anySatisfy(pair -> {
                    assertThat(pair.key).isEqualTo("exceptionType");
                    assertThat(pair.value).isEqualTo(ServletException.class.getName());
                });
        assertThat(MDC.get("correlationId")).isNull();
    }

    private MockHttpServletResponse execute(MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
        });
        return response;
    }
}
