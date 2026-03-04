package com.poc_spring_batch.web;


import com.poc_spring_batch.configuration.JwtTokenProvider;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * POST /api/auth/login  →  retourne un token JWT valable 5 minutes.
 */
@Log4j2
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(AuthenticationManager authManager, JwtTokenProvider jwtTokenProvider) {
        this.authManager = authManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        try {
            var auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.username(), req.password()));

            UserDetails user = (UserDetails) auth.getPrincipal();
            String token = jwtTokenProvider.generateToken(user);

            log.info("Login OK : {}", req.username());

            return ResponseEntity.ok(new LoginResponse(
                    token,
                    "Bearer",
                    jwtTokenProvider.getExpirationMinutes() * 60L,
                    user.getUsername(),
                    LocalDateTime.now().plusMinutes(jwtTokenProvider.getExpirationMinutes()).toString()
            ));

        } catch (BadCredentialsException e) {
            log.warn("Login échoué pour : {}", req.username());
            return ResponseEntity.status(401).body(Map.of("error", "Identifiants invalides"));
        }
    }

    public record LoginRequest(String username, String password) {}

    public record LoginResponse(
            String accessToken,
            String tokenType,
            Long expiresIn,       // secondes
            String username,
            String expiresAt      // ISO datetime
    ) {}
}
