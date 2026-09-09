package com.lyftix.backend.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UsernameNormalizerTests {

    private final UsernameNormalizer normalizer = new UsernameNormalizer();

    @Test
    void trimsAndLowercasesUsingStableRules() {
        assertThat(normalizer.normalize("  Owner.Name@Example.COM ")).isEqualTo("owner.name@example.com");
    }

    @Test
    void normalizesNullToEmptyInput() {
        assertThat(normalizer.normalize(null)).isEmpty();
    }
}
