package com.fuelqueue.model;

public class LocationPing {

    private Long   userId;
    private double latitude;
    private double longitude;
    private double speedKmh;
    private long   timestamp;

    public LocationPing() {}

    public LocationPing(Long userId, double latitude, double longitude,
                        double speedKmh, long timestamp) {
        this.userId    = userId;
        this.latitude  = latitude;
        this.longitude = longitude;
        this.speedKmh  = speedKmh;
        this.timestamp = timestamp;
    }

    public Long getUserId()            { return userId; }
    public void setUserId(Long v)      { this.userId = v; }
    public double getLatitude()        { return latitude; }
    public void setLatitude(double v)  { this.latitude = v; }
    public double getLongitude()       { return longitude; }
    public void setLongitude(double v) { this.longitude = v; }
    public double getSpeedKmh()        { return speedKmh; }
    public void setSpeedKmh(double v)  { this.speedKmh = v; }
    public long getTimestamp()         { return timestamp; }
    public void setTimestamp(long v)   { this.timestamp = v; }
}
