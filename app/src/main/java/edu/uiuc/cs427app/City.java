package edu.uiuc.cs427app;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "cities")
public class City {
    private String city;
    private double lat;
    private double lng;
    private String country;
    private String iso2;
    private String iso3;
    private String admin_name;

    @PrimaryKey
    private int id;

    // No-argument constructor for Room.
    @Ignore
    public City() {
    }

    // Constructor to create a new City object.
    public City(String city, double lat, double lng, String country,
            String iso2, String iso3, String admin_name, int id) {
        this.id = id;
        this.city = city;
        this.lat = lat;
        this.lng = lng;
        this.country = country;
        this.iso2 = iso2;
        this.iso3 = iso3;
        this.admin_name = admin_name;
    }

    // ==============================
    // Getters and Setters
    // ==============================
    // Gets the city ID.
    public int getId() {
        return id;
    }

    // Sets the city ID.
    public void setId(int id) {
        this.id = id;
    }

    // Gets the city name.
    public String getCity() {
        return city;
    }

    // Sets the city name.
    public void setCity(String city) {
        this.city = city;
    }

    // Gets the country name.
    public String getCountry() {
        return country;
    }

    // Sets the country name.
    public void setCountry(String country) {
        this.country = country;
    }

    // Gets the ISO2 country code.
    public String getIso2() {
        return iso2;
    }

    // Sets the ISO2 country code.
    public void setIso2(String iso2) {
        this.iso2 = iso2;
    }

    // Gets the ISO3 country code.
    public String getIso3() {
        return iso3;
    }

    // Sets the ISO3 country code.
    public void setIso3(String iso3) {
        this.iso3 = iso3;
    }

    // Gets the admin name.
    public String getAdmin_name() {
        return admin_name;
    }

    // Sets the admin name.
    public void setAdmin_name(String admin_name) {
        this.admin_name = admin_name;
    }

    // Gets the latitude.
    public double getLat() {
        return lat;
    }

    // Sets the latitude.
    public void setLat(double lat) {
        this.lat = lat;
    }

    // Gets the longitude.
    public double getLng() {
        return lng;
    }

    // Sets the longitude.
    public void setLng(double lng) {
        this.lng = lng;
    }
}
