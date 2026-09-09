package com.lyftix.backend.controller;

import com.lyftix.backend.dto.AuthenticatedUserResponse;
import com.lyftix.backend.dto.CsrfTokenResponse;
import com.lyftix.backend.dto.LoginRequest;
import com.lyftix.backend.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final AuthenticationManager authenticationManager;
    private final CookieCsrfTokenRepository csrfTokenRepository;
    private final HttpSessionSecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();
    private final SecurityContextLogoutHandler securityContextLogoutHandler = new SecurityContextLogoutHandler();
    private final CookieClearingLogoutHandler cookieClearingLogoutHandler =
            new CookieClearingLogoutHandler("JSESSIONID", "XSRF-TOKEN");

    public AuthenticationController(AuthenticationManager authenticationManager,
                                    CookieCsrfTokenRepository csrfTokenRepository) {
        this.authenticationManager = authenticationManager;
        this.csrfTokenRepository = csrfTokenRepository;
    }

    @GetMapping("/csrf")
    @Operation(summary = "Issue a CSRF token", description = "Returns the token required in the X-XSRF-TOKEN header for state-changing requests.")
    public CsrfTokenResponse csrf(HttpServletRequest request, HttpServletResponse response) {
        CsrfToken token = csrfTokenRepository.generateToken(request);
        csrfTokenRepository.saveToken(token, request, response);
        return new CsrfTokenResponse(token.getHeaderName(), token.getParameterName(), token.getToken());
    }

    @PostMapping("/login")
    @Operation(summary = "Log in", description = "Authenticates credentials and establishes an HttpOnly server-side session cookie. Requires a CSRF token.")
    @ApiResponse(responseCode = "200", description = "Authenticated")
    @ApiResponse(responseCode = "401", description = "Invalid credentials",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Missing or invalid CSRF token",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public AuthenticatedUserResponse login(@Valid @RequestBody LoginRequest request,
                                           HttpServletRequest servletRequest,
                                           HttpServletResponse servletResponse) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, servletRequest, servletResponse);
        return response(authentication);
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "sessionCookie")
    @Operation(summary = "Get current user")
    @ApiResponse(responseCode = "401", description = "No authenticated session",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public AuthenticatedUserResponse me(Authentication authentication) {
        return response(authentication);
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "sessionCookie")
    @Operation(summary = "Log out", description = "Invalidates the server-side session. Requires a CSRF token.")
    @ApiResponse(responseCode = "204", description = "Logged out")
    @ApiResponse(responseCode = "401", description = "No authenticated session",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Missing or invalid CSRF token",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<Void> logout(Authentication authentication, HttpServletRequest request,
                                       HttpServletResponse response) {
        securityContextLogoutHandler.logout(request, response, authentication);
        cookieClearingLogoutHandler.logout(request, response, authentication);
        return ResponseEntity.noContent().build();
    }

    private AuthenticatedUserResponse response(Authentication authentication) {
        String role = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .findFirst()
                .orElse("UNKNOWN");
        return new AuthenticatedUserResponse(authentication.getName(), role);
    }
}
