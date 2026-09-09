package com.lyftix.backend.config;

import com.lyftix.backend.security.ApiSecurityErrorWriter;
import com.lyftix.backend.security.SpaCsrfTokenRequestHandler;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableConfigurationProperties(AuthenticationProperties.class)
public class SecurityConfiguration {

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    CookieCsrfTokenRepository csrfTokenRepository() {
        return CookieCsrfTokenRepository.withHttpOnlyFalse();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ApiSecurityErrorWriter errorWriter,
                                            CookieCsrfTokenRepository csrfRepository) throws Exception {
        AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();

        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepository)
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/health", "/api/auth/login", "/api/auth/csrf").permitAll()
                        .requestMatchers("/actuator", "/actuator/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> errorWriter.write(
                                response, HttpStatus.UNAUTHORIZED.value(), "Unauthorized", "Authentication is required"))
                        .accessDeniedHandler((request, response, exception) -> {
                            var authentication = SecurityContextHolder.getContext().getAuthentication();
                            if (authentication == null || trustResolver.isAnonymous(authentication)) {
                                errorWriter.write(response, HttpStatus.UNAUTHORIZED.value(),
                                        "Unauthorized", "Authentication is required");
                            } else {
                                errorWriter.write(response, HttpStatus.FORBIDDEN.value(),
                                        "Forbidden", "Access is denied");
                            }
                        }))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .requestCache(cache -> cache.disable())
                .sessionManagement(session -> session.sessionFixation(fixation -> fixation.changeSessionId()));

        return http.build();
    }
}
