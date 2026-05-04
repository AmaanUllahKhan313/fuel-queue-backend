package com.fuelqueue.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private Boolean phoneVerified = false;

    private String otp;
    private LocalDateTime otpExpiresAt;

    private String name;
    private String fcmToken;

    public User() {}

    public Long getId()                   { return id; }
    public String getPhoneNumber()        { return phoneNumber; }
    public void setPhoneNumber(String v)  { this.phoneNumber = v; }
    public Boolean getPhoneVerified()     { return phoneVerified; }
    public void setPhoneVerified(Boolean v) { this.phoneVerified = v; }
    public String getOtp()                { return otp; }
    public void setOtp(String v)          { this.otp = v; }
    public LocalDateTime getOtpExpiresAt() { return otpExpiresAt; }
    public void setOtpExpiresAt(LocalDateTime v) { this.otpExpiresAt = v; }
    public String getName()               { return name; }
    public void setName(String v)         { this.name = v; }
    public String getFcmToken()           { return fcmToken; }
    public void setFcmToken(String v)     { this.fcmToken = v; }
}
