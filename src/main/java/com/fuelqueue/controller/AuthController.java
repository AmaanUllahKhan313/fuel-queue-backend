package com.fuelqueue.controller;

import com.fuelqueue.dto.OtpRequest;
import com.fuelqueue.dto.OtpResponse;
import com.fuelqueue.dto.AuthRequest;
import com.fuelqueue.model.User;
import com.fuelqueue.repository.UserRepository;
import com.fuelqueue.security.JwtService;
import com.fuelqueue.service.OtpService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepo;
    private final JwtService      jwtService;
    private final OtpService      otpService;

    public AuthController(UserRepository userRepo,
                          JwtService jwtService,
                          OtpService otpService) {
        this.userRepo   = userRepo;
        this.jwtService = jwtService;
        this.otpService = otpService;
    }

    /**
     * POST /api/auth/send-otp
     * Body: { "phoneNumber": "+919876543210" }
     * Generates and sends OTP to the phone number
     */
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody OtpRequest req) {
        String phoneNumber = req.getPhoneNumber();
        
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Phone number is required"));
        }

        // Generate OTP
        String otp = otpService.generateOtp();
        
        // Find or create user
        User user = userRepo.findByPhoneNumber(phoneNumber)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setPhoneNumber(phoneNumber);
                    return newUser;
                });

        // Store OTP and expiration
        user.setOtp(otp);
        user.setOtpExpiresAt(otpService.getOtpExpirationTime());
        userRepo.save(user);

        // TODO: In production, send OTP via SMS service (Twilio, AWS SNS, etc.)
        // For now, we'll return it in response for testing
        System.out.println("OTP for " + phoneNumber + ": " + otp);

        return ResponseEntity.ok(Map.of(
                "message", "OTP sent to phone number",
                "phoneNumber", phoneNumber,
                "otp", otp  // Remove this in production
        ));
    }

    /**
     * POST /api/auth/verify-otp
     * Body: { "phoneNumber": "+919876543210", "otp": "123456", "name": "Rahul" }
     * Verifies OTP and returns JWT token (works for both login and register)
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody AuthRequest req) {
        String phoneNumber = req.getPhoneNumber();
        String otp = req.getOtp();
        String name = req.getName();

        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Phone number is required"));
        }

        if (otp == null || otp.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "OTP is required"));
        }

        // Find user by phone number
        return userRepo.findByPhoneNumber(phoneNumber)
                .map(user -> {
                    // Verify OTP
                    if (!otpService.verifyOtp(otp, user.getOtp(), user.getOtpExpiresAt())) {
                        return ResponseEntity.status(401)
                                .body(Map.of("error", "Invalid or expired OTP"));
                    }

                    // If name is provided and user doesn't have name, this is a registration
                    if (name != null && !name.trim().isEmpty() && user.getName() == null) {
                        user.setName(name);
                    }

                    // Mark phone as verified
                    user.setPhoneVerified(true);
                    user.setOtp(null);
                    user.setOtpExpiresAt(null);
                    userRepo.save(user);

                    // Generate JWT token using phone number as identifier
                    return ResponseEntity.ok(Map.of(
                            "token", jwtService.generateToken(user.getPhoneNumber(), user.getId()),
                            "userId", user.getId(),
                            "name", user.getName() != null ? user.getName() : "",
                            "phoneNumber", user.getPhoneNumber()
                    ));
                })
                .orElse(ResponseEntity.status(401)
                        .body(Map.of("error", "User not found. Please send OTP first.")));
    }

    /**
     * POST /api/auth/register
     * Deprecated - Use send-otp followed by verify-otp instead
     * @deprecated
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest req) {
        return ResponseEntity.status(410)
                .body(Map.of("error", "This endpoint is deprecated. Use /send-otp and /verify-otp instead."));
    }

    /**
     * POST /api/auth/login
     * Deprecated - Use send-otp followed by verify-otp instead
     * @deprecated
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest req) {
        return ResponseEntity.status(410)
                .body(Map.of("error", "This endpoint is deprecated. Use /send-otp and /verify-otp instead."));
    }
}
