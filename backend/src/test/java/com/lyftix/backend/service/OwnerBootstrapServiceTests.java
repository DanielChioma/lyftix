package com.lyftix.backend.service;

import com.lyftix.backend.config.AuthenticationProperties;
import com.lyftix.backend.model.AppUser;
import com.lyftix.backend.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnerBootstrapServiceTests {

    @Mock private AppUserRepository repository;
    @Mock private PasswordEncoder passwordEncoder;
    private final UsernameNormalizer normalizer = new UsernameNormalizer();

    @Test
    void doesNothingWhenBootstrapIsNotConfigured() {
        service("", "").run(null);
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsPartialBootstrapConfiguration() {
        assertThatThrownBy(() -> service("owner", "").run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("configured together");
        assertThatThrownBy(() -> service("", "strong-password").run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("configured together");
    }

    @Test
    void rejectsWeakBootstrapPassword() {
        assertThatThrownBy(() -> service("owner", "too-short").run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 12");
    }

    @Test
    void createsNormalizedOwnerWithEncodedPassword() {
        when(repository.findByUsername("owner@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("strong-password")).thenReturn("{bcrypt}hash");

        service(" Owner@Example.COM ", "strong-password").run(null);

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(repository).save(captor.capture());
        AppUser saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("owner@example.com");
        assertThat(saved.getPasswordHash()).isEqualTo("{bcrypt}hash");
        assertThat(saved.getRole().name()).isEqualTo("OWNER");
        assertThat(saved.isEnabled()).isTrue();
    }

    @Test
    void leavesExistingOwnerUntouched() {
        AppUser existing = new AppUser("owner", "{bcrypt}existing", com.lyftix.backend.model.AppUserRole.OWNER, true);
        when(repository.findByUsername("owner")).thenReturn(Optional.of(existing));

        service("owner", "strong-password").run(null);

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(passwordEncoder, never()).encode(org.mockito.ArgumentMatchers.anyString());
    }

    private OwnerBootstrapService service(String username, String password) {
        return new OwnerBootstrapService(new AuthenticationProperties(username, password), repository,
                passwordEncoder, normalizer);
    }
}
