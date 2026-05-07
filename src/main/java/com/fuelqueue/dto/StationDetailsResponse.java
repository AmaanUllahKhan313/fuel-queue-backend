package com.fuelqueue.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for get station by ID endpoint.
 * Includes stock availability status based on waiting vehicles.
 */
public class StationDetailsResponse {

    private Long id;
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private double geofenceRadiusMeters;
    private boolean active;
    private boolean stockAvailable;
    @JsonProperty("isLive")
    private boolean isLive;
    private int currentCrowdCount;
    private String crowdLevel;
    private int estimatedWaitMinutes;

    public StationDetailsResponse() {}

    public StationDetailsResponse(Long id, String name, String address,
                                  double latitude, double longitude,
                                  double geofenceRadiusMeters, boolean active,
                                  boolean stockAvailable, boolean isLive, int currentCrowdCount,
                                  String crowdLevel, int estimatedWaitMinutes) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.geofenceRadiusMeters = geofenceRadiusMeters;
        this.active = active;
        this.stockAvailable = stockAvailable;
        this.isLive = isLive;
        this.currentCrowdCount = currentCrowdCount;
        this.crowdLevel = crowdLevel;
        this.estimatedWaitMinutes = estimatedWaitMinutes;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }

    public String getName() { return name; }
    public void setName(String v) { this.name = v; }

    public String getAddress() { return address; }
    public void setAddress(String v) { this.address = v; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double v) { this.latitude = v; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double v) { this.longitude = v; }

    public double getGeofenceRadiusMeters() { return geofenceRadiusMeters; }
    public void setGeofenceRadiusMeters(double v) { this.geofenceRadiusMeters = v; }

    public boolean isActive() { return active; }
    public void setActive(boolean v) { this.active = v; }

    public boolean isStockAvailable() { return stockAvailable; }
    public void setStockAvailable(boolean v) { this.stockAvailable = v; }

    @JsonProperty("isLive")
    public boolean isLive() { return isLive; }
    public void setLive(boolean v) { this.isLive = v; }

    public int getCurrentCrowdCount() { return currentCrowdCount; }
    public void setCurrentCrowdCount(int v) { this.currentCrowdCount = v; }

    public String getCrowdLevel() { return crowdLevel; }
    public void setCrowdLevel(String v) { this.crowdLevel = v; }

    public int getEstimatedWaitMinutes() { return estimatedWaitMinutes; }
    public void setEstimatedWaitMinutes(int v) { this.estimatedWaitMinutes = v; }
}

