package com.lyftix.backend.service;

import com.lyftix.backend.config.AuthenticationProperties;
import com.lyftix.backend.model.AppUser;
import com.lyftix.backend.model.AppUserRole;
import com.lyftix.backend.repository.AppUserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

@Component
public class OwnerBootstrapService implements ApplicationRunner {

    private static final int MINIMUM_PASSWORD_LENGTH = 12;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("[a-z0-9][a-z0-9._@-]{2,99}");

    private final AuthenticationProperties properties;
    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final UsernameNormalizer usernameNormalizer;

    public OwnerBootstrapService(AuthenticationProperties properties, AppUserRepository repository,
                                 PasswordEncoder passwordEncoder, UsernameNormalizer usernameNormalizer) {
        this.properties = properties;
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.usernameNormalizer = usernameNormalizer;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String configuredUsername = properties.username();
        String configuredPassword = properties.password();
        boolean hasUsername = StringUtils.hasText(configuredUsername);
        boolean hasPassword = StringUtils.hasText(configuredPassword);

        if (!hasUsername && !hasPassword) {
            return;
        }
        if (hasUsername != hasPassword) {
            throw new IllegalStateException("LYFTIX_AUTH_BOOTSTRAP_USERNAME and LYFTIX_AUTH_BOOTSTRAP_PASSWORD must be configured together");
        }

        String username = usernameNormalizer.normalize(configuredUsername);
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalStateException("LYFTIX_AUTH_BOOTSTRAP_USERNAME must be 3-100 characters using letters, numbers, '.', '_', '@', or '-'");
        }
        if (configuredPassword.length() < MINIMUM_PASSWORD_LENGTH) {
            throw new IllegalStateException("LYFTIX_AUTH_BOOTSTRAP_PASSWORD must be at least 12 characters");
        }

        if (repository.findByUsername(username).isEmpty()) {
            repository.save(new AppUser(username, passwordEncoder.encode(configuredPassword), AppUserRole.OWNER, true));
        }
    }
}
