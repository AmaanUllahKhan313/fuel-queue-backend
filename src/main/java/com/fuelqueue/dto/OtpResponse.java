package com.fuelqueue.dto;

public class OtpResponse {
    private String message;
    private String phoneNumber;

    public OtpResponse() {}

    public OtpResponse(String message, String phoneNumber) {
        this.message = message;
        this.phoneNumber = phoneNumber;
    }

    public String getMessage()           { return message; }
    public void setMessage(String v)     { this.message = v; }
    public String getPhoneNumber()       { return phoneNumber; }
    public void setPhoneNumber(String v) { this.phoneNumber = v; }
}

