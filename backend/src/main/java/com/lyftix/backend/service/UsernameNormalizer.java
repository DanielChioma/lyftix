package com.lyftix.backend.service;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class UsernameNormalizer {

    public String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
