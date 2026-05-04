package com.fuelqueue.service;

import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class OtpService {
    private static final int OTP_SIZE = 6;
    private static final int OTP_VALIDITY_MINUTES = 10;
    private static final SecureRandom random = new SecureRandom();

    /**
     * Generate a random 6-digit OTP
     */
    public String generateOtp() {
        int otp = random.nextInt((int) Math.pow(10, OTP_SIZE));
        return String.format("%0" + OTP_SIZE + "d", otp);
    }

    /**
     * Get OTP expiration time (current time + 10 minutes)
     */
    public LocalDateTime getOtpExpirationTime() {
        return LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES);
    }

    /**
     * Check if OTP is expired
     */
    public boolean isOtpExpired(LocalDateTime expiresAt) {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Verify OTP matches and is not expired
     */
    public boolean verifyOtp(String providedOtp, String storedOtp, LocalDateTime expiresAt) {
        if (storedOtp == null || expiresAt == null) {
            return false;
        }
        return storedOtp.equals(providedOtp) && !isOtpExpired(expiresAt);
    }
}

