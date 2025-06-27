package com.example.riderepair;

public class SOSRequest {
    public String getVehicleNumber;
    private String id;
    private String userId;
    private String userName;
    private String vehicleName;
    private String vehicleNumber;
    private String vehicleType;
    private double lat;
    private double lng;
    private String status;
    private long timestamp;

    public SOSRequest(String id, String userId, String userName, String vehicleName, String vehicleNumber,
                      String vehicleType, Double lat, Double lng, String status, Long timestamp) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.vehicleName = vehicleName;
        this.vehicleNumber = vehicleNumber;
        this.vehicleType = vehicleType;
        this.lat = lat != null ? lat : 0.0;
        this.lng = lng != null ? lng : 0.0;
        this.status = status;
        this.timestamp = timestamp != null ? timestamp : 0L;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getVehicleName() {
        return vehicleName;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public String getStatus() {
        return status;
    }

    public long getTimestamp() {
        return timestamp;
    }
}