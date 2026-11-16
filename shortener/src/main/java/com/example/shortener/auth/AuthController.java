package com.example.shortener.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    record Credentials(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 8, max = 72) String password
    ) {}

    record TokenResponse(String token) {}

    @PostMapping("/register")
    public ResponseEntity<Void> register(
            @Valid @RequestBody Credentials c) {

        if (users.existsByEmail(c.email())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Email taken");
        }

        AppUser u = new AppUser();
        u.setEmail(c.email());
        u.setPasswordHash(encoder.encode(c.password()));

        users.save(u);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public TokenResponse login(
            @Valid @RequestBody Credentials c) {

        AppUser u = users.findByEmail(c.email())
                .filter(x -> encoder.matches(
                        c.password(), x.getPasswordHash()))
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Bad credentials"));

        return new TokenResponse(jwt.generate(u.getId()));
    }
}