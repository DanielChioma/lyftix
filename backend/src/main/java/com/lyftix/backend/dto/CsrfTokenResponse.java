package com.lyftix.backend.dto;

public record CsrfTokenResponse(String headerName, String parameterName, String token) {
}
