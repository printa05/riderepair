package com.example.riderepair;

public class Vehicle {
    private String id;
    private String userId;
    private String name;
    private String number;
    private String type;

    public Vehicle() {
    }

    public Vehicle(String id, String userId, String name, String number, String type) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.number = number;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getNumber() {
        return number;
    }

    public String getType() {
        return type;
    }
}