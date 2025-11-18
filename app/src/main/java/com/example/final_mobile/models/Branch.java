package com.example.final_mobile.models;

import java.util.List;

public class Branch {
    private String id;
    private String name;
    private String address;
    private String phone;
    private double latitude;
    private double longitude;
    private String openingHours;
    private List<String> services;
    private double distance; // Distance from user in kilometers
    private String distanceText; // Formatted distance text

    // Constructors
    public Branch() {
    }

    public Branch(String id, String name, String address, String phone, 
                  double latitude, double longitude, String openingHours, List<String> services) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.latitude = latitude;
        this.longitude = longitude;
        this.openingHours = openingHours;
        this.services = services;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getOpeningHours() {
        return openingHours;
    }

    public void setOpeningHours(String openingHours) {
        this.openingHours = openingHours;
    }

    public List<String> getServices() {
        return services;
    }

    public void setServices(List<String> services) {
        this.services = services;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public String getDistanceText() {
        return distanceText;
    }

    public void setDistanceText(String distanceText) {
        this.distanceText = distanceText;
    }

    // Helper method to format services list
    public String getServicesText() {
        if (services == null || services.isEmpty()) {
            return "";
        }
        return String.join(", ", services);
    }
}

