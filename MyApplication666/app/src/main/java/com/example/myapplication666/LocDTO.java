package com.example.myapplication666;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class LocDTO {
    private String locName; // 위치 이름
    private double latitude; // 위도
    private double longitude; // 경도

    public LocDTO(String saveName, double latitude, double longitude){
        this.locName = saveName;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getLocName() {
        return locName;
    }

    public void setLocName(String locName) {
        this.locName = locName;
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

    @Override
    public String toString(){
        return "Locs{" +
                "locName='" + locName + '\'' +
                ", latitude='" + latitude + '\'' +
                ", longitude='" + longitude + '\'' +
                '}';
    }
}
