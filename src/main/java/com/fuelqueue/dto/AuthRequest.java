package com.fuelqueue.dto;

public class AuthRequest {
    private String phoneNumber;
    private String otp;
    private String name; // only for register

    public String getPhoneNumber()       { return phoneNumber; }
    public void setPhoneNumber(String v) { this.phoneNumber = v; }
    public String getOtp()               { return otp; }
    public void setOtp(String v)         { this.otp = v; }
    public String getName()              { return name; }
    public void setName(String v)        { this.name = v; }
}
