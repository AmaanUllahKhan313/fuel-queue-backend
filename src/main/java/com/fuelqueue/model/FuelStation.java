package com.fuelqueue.model;

import jakarta.persistence.*;

@Entity
@Table(name = "fuel_stations")
public class FuelStation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String address;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(name = "geofence_radius_meters")
    private double geofenceRadiusMeters = 80.0;

    private boolean active = true;

    public FuelStation() {}

    public FuelStation(String name, String address,
                       double latitude, double longitude,
                       double geofenceRadiusMeters) {
        this.name                 = name;
        this.address              = address;
        this.latitude             = latitude;
        this.longitude            = longitude;
        this.geofenceRadiusMeters = geofenceRadiusMeters;
        this.active               = true;
    }

    public Long getId()                           { return id; }
    public String getName()                       { return name; }
    public void setName(String v)                 { this.name = v; }
    public String getAddress()                    { return address; }
    public void setAddress(String v)              { this.address = v; }
    public double getLatitude()                   { return latitude; }
    public void setLatitude(double v)             { this.latitude = v; }
    public double getLongitude()                  { return longitude; }
    public void setLongitude(double v)            { this.longitude = v; }
    public double getGeofenceRadiusMeters()       { return geofenceRadiusMeters; }
    public void setGeofenceRadiusMeters(double v) { this.geofenceRadiusMeters = v; }
    public boolean isActive()                     { return active; }
    public void setActive(boolean v)              { this.active = v; }
}
