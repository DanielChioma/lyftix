package com.lyftix.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lyftix.auth.bootstrap")
public record AuthenticationProperties(String username, String password) {
}
