package com.fuelqueue.dto;

public class AuthRequest {
    private String email;
    private String password;
    private String name; // only for register

    public String getEmail()       { return email; }
    public void setEmail(String v) { this.email = v; }
    public String getPassword()       { return password; }
    public void setPassword(String v) { this.password = v; }
    public String getName()        { return name; }
    public void setName(String v)  { this.name = v; }
}
