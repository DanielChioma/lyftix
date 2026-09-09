package com.lyftix.backend;

import com.lyftix.backend.logging.RequestLoggingFilter;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RequestLoggingIntegrationTests extends PostgreSqlIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void requestWithoutCorrelationHeaderReturnsGeneratedUuid() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists(RequestLoggingFilter.CORRELATION_ID_HEADER))
                .andReturn();

        String correlationId = result.getResponse()
                .getHeader(RequestLoggingFilter.CORRELATION_ID_HEADER);
        assertThat(UUID.fromString(correlationId).toString()).isEqualTo(correlationId);
    }

    @Test
    void validIncomingCorrelationHeaderIsReturnedUnchanged() throws Exception {
        mockMvc.perform(get("/api/health")
                        .header(RequestLoggingFilter.CORRELATION_ID_HEADER, "integration-request-123"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        RequestLoggingFilter.CORRELATION_ID_HEADER,
                        "integration-request-123"
                ));
    }

    @Test
    void overlongCorrelationHeaderIsReplaced() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/health")
                        .header(RequestLoggingFilter.CORRELATION_ID_HEADER, "x".repeat(129)))
                .andExpect(status().isOk())
                .andReturn();

        String correlationId = result.getResponse()
                .getHeader(RequestLoggingFilter.CORRELATION_ID_HEADER);
        assertThat(correlationId).hasSize(36).isNotEqualTo("x".repeat(129));
    }

    @Test
    void validationFailureRetainsCorrelationHeaderAndApiErrorShape() throws Exception {
        mockMvc.perform(post("/api/workouts").with(csrf())
                        .header(RequestLoggingFilter.CORRELATION_ID_HEADER, "validation-request-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(
                        RequestLoggingFilter.CORRELATION_ID_HEADER,
                        "validation-request-123"
                ))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.correlationId").doesNotExist());
    }

    @Test
    void requestMdcDoesNotLeakBackToCallingThread() throws Exception {
        mockMvc.perform(get("/api/health")).andExpect(status().isOk());

        assertThat(MDC.get("correlationId")).isNull();
        assertThat(MDC.get("httpMethod")).isNull();
        assertThat(MDC.get("requestPath")).isNull();
    }
}
