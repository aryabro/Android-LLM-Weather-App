package edu.uiuc.cs427app;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "cities")
public class City {
    private String city;
    private String city_ascii;
    private String country;
    private String iso2;
    private String iso3;
    private String admin_name;

    @PrimaryKey
    private int id;

    @Ignore
    public City() {}

    public City(String city, String city_ascii, String country,
                String iso2, String iso3, String admin_name, int id) {
        this.id = id;
        this.city = city;
        this.city_ascii = city_ascii;
        this.country = country;
        this.iso2 = iso2;
        this.iso3 = iso3;
        this.admin_name = admin_name;
    }

    // ==============================
    // Getter 和 Setter
    // ==============================
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getCity_ascii() { return city_ascii; }
    public void setCity_ascii(String city_ascii) { this.city_ascii = city_ascii; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getIso2() { return iso2; }
    public void setIso2(String iso2) { this.iso2 = iso2; }

    public String getIso3() { return iso3; }
    public void setIso3(String iso3) { this.iso3 = iso3; }

    public String getAdmin_name() { return admin_name; }
    public void setAdmin_name(String admin_name) { this.admin_name = admin_name; }
}
