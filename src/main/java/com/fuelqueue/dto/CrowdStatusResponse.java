package com.fuelqueue.dto;

public class CrowdStatusResponse {

    private Long   stationId;
    private String stationName;
    private int    activeUsers;
    private String crowdLevel;
    private int    estimatedWaitMinutes;
    private long   updatedAt;

    public CrowdStatusResponse() {}

    public CrowdStatusResponse(Long stationId, String stationName,
                               int activeUsers, String crowdLevel,
                               int estimatedWaitMinutes) {
        this.stationId            = stationId;
        this.stationName          = stationName;
        this.activeUsers          = activeUsers;
        this.crowdLevel           = crowdLevel;
        this.estimatedWaitMinutes = estimatedWaitMinutes;
        this.updatedAt            = System.currentTimeMillis();
    }

    public Long getStationId()                 { return stationId; }
    public void setStationId(Long v)           { this.stationId = v; }
    public String getStationName()             { return stationName; }
    public void setStationName(String v)       { this.stationName = v; }
    public int getActiveUsers()                { return activeUsers; }
    public void setActiveUsers(int v)          { this.activeUsers = v; }
    public String getCrowdLevel()              { return crowdLevel; }
    public void setCrowdLevel(String v)        { this.crowdLevel = v; }
    public int getEstimatedWaitMinutes()       { return estimatedWaitMinutes; }
    public void setEstimatedWaitMinutes(int v) { this.estimatedWaitMinutes = v; }
    public long getUpdatedAt()                 { return updatedAt; }
    public void setUpdatedAt(long v)           { this.updatedAt = v; }
}
