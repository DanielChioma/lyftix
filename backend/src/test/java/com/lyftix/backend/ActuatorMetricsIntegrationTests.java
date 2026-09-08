package com.lyftix.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = "management.prometheus.metrics.export.enabled=true")
class ActuatorMetricsIntegrationTests extends PostgreSqlIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void actuatorHealthIsAvailableWithoutReplacingApplicationHealth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    @Test
    void prometheusEndpointPublishesRepresentativeMetricsAndApplicationTag() throws Exception {
        mockMvc.perform(get("/api/health")).andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentType()).startsWith("text/plain");
        String metrics = result.getResponse().getContentAsString();
        assertThat(metrics).contains("jvm_memory_used_bytes");
        assertThat(metrics).contains("process_uptime_seconds");
        assertThat(metrics).contains("system_cpu_count");
        assertThat(metrics).contains("http_server_requests_seconds_count");
        assertThat(metrics).contains("application=\"lyftix-backend\"");
    }

    @Test
    void onlyRequiredActuatorEndpointsAreExposed() throws Exception {
        mockMvc.perform(get("/actuator"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.health").exists())
                .andExpect(jsonPath("$._links.info").exists())
                .andExpect(jsonPath("$._links.prometheus").exists())
                .andExpect(jsonPath("$._links.env").doesNotExist())
                .andExpect(jsonPath("$._links.beans").doesNotExist())
                .andExpect(jsonPath("$._links.configprops").doesNotExist())
                .andExpect(jsonPath("$._links.heapdump").doesNotExist())
                .andExpect(jsonPath("$._links.threaddump").doesNotExist())
                .andExpect(jsonPath("$._links.mappings").doesNotExist());

        mockMvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
        mockMvc.perform(get("/actuator/heapdump")).andExpect(status().isNotFound());
    }
}
