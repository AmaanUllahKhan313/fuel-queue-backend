package com.fuelqueue.controller;

import com.fuelqueue.dto.AuthRequest;
import com.fuelqueue.model.User;
import com.fuelqueue.repository.UserRepository;
import com.fuelqueue.security.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository  userRepo;
    private final PasswordEncoder encoder;
    private final JwtService      jwtService;

    public AuthController(UserRepository userRepo,
                          PasswordEncoder encoder,
                          JwtService jwtService) {
        this.userRepo   = userRepo;
        this.encoder    = encoder;
        this.jwtService = jwtService;
    }

    /**
     * POST /api/auth/register
     * Body: { "email": "user@example.com", "password": "pass123", "name": "Rahul" }
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest req) {
        if (userRepo.existsByEmail(req.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email already registered"));
        }
        User user = new User();
        user.setEmail(req.getEmail());
        user.setPasswordHash(encoder.encode(req.getPassword()));
        user.setName(req.getName());
        userRepo.save(user);
        return ResponseEntity.ok(Map.of("message", "Registered successfully"));
    }

    /**
     * POST /api/auth/login
     * Body: { "email": "user@example.com", "password": "pass123" }
     * Returns: { "token": "eyJ...", "userId": 1, "name": "Rahul" }
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest req) {
        return userRepo.findByEmail(req.getEmail())
                .filter(u -> encoder.matches(req.getPassword(), u.getPasswordHash()))
                .map(u -> ResponseEntity.ok(Map.of(
                        "token",  jwtService.generateToken(u.getEmail(), u.getId()),
                        "userId", u.getId(),
                        "name",   u.getName() != null ? u.getName() : ""
                )))
                .orElse(ResponseEntity.status(401)
                        .body(Map.of("error", "Invalid email or password")));
    }
}
