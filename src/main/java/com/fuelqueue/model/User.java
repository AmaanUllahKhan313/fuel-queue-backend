package com.fuelqueue.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    private String name;
    private String fcmToken;

    public User() {}

    public Long getId()                   { return id; }
    public String getEmail()              { return email; }
    public void setEmail(String v)        { this.email = v; }
    public String getPasswordHash()       { return passwordHash; }
    public void setPasswordHash(String v) { this.passwordHash = v; }
    public String getName()               { return name; }
    public void setName(String v)         { this.name = v; }
    public String getFcmToken()           { return fcmToken; }
    public void setFcmToken(String v)     { this.fcmToken = v; }
}
