package com.lyftix.backend.security;

import com.lyftix.backend.model.AppUser;
import com.lyftix.backend.repository.AppUserRepository;
import com.lyftix.backend.service.UsernameNormalizer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final AppUserRepository repository;
    private final UsernameNormalizer usernameNormalizer;

    public DatabaseUserDetailsService(AppUserRepository repository, UsernameNormalizer usernameNormalizer) {
        this.repository = repository;
        this.usernameNormalizer = usernameNormalizer;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = repository.findByUsername(usernameNormalizer.normalize(username))
                .orElseThrow(() -> new UsernameNotFoundException("Invalid username or password"));
        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                .disabled(!user.isEnabled())
                .build();
    }
}
