package com.lyftix.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyftix.backend.exception.ApiErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Component
public class ApiSecurityErrorWriter {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public void write(HttpServletResponse response, int status, String error, String detail) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(),
                new ApiErrorResponse(Instant.now(), status, error, List.of(detail)));
    }
}
