package com.fuelqueue.dto;

public class NearbyStationsResponse {

    private Long   stationId;
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private double distanceMeters;
    private String crowdLevel;
    private int    activeUsers;
    private int    estimatedWaitMinutes;

    public NearbyStationsResponse() {}

    public NearbyStationsResponse(Long stationId, String name, String address,
                                  double latitude, double longitude,
                                  double distanceMeters, String crowdLevel,
                                  int activeUsers, int estimatedWaitMinutes) {
        this.stationId            = stationId;
        this.name                 = name;
        this.address              = address;
        this.latitude             = latitude;
        this.longitude            = longitude;
        this.distanceMeters       = distanceMeters;
        this.crowdLevel           = crowdLevel;
        this.activeUsers          = activeUsers;
        this.estimatedWaitMinutes = estimatedWaitMinutes;
    }

    public Long getStationId()              { return stationId; }
    public String getName()                 { return name; }
    public String getAddress()              { return address; }
    public double getLatitude()             { return latitude; }
    public double getLongitude()            { return longitude; }
    public double getDistanceMeters()       { return distanceMeters; }
    public String getCrowdLevel()           { return crowdLevel; }
    public int getActiveUsers()             { return activeUsers; }
    public int getEstimatedWaitMinutes()    { return estimatedWaitMinutes; }
}
