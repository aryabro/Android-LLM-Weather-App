package edu.uiuc.cs427app;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CityDao {
    @Query("SELECT * FROM cities")
    List<City> getAllCities();

    @Insert
    void insertAll(List<City> cities);

    @Query("SELECT * FROM cities WHERE LOWER(city) = LOWER(:name) OR LOWER(city_ascii) = LOWER(:name)")
    List<City> findAllByName(String name);

    @Query("SELECT COUNT(*) FROM cities")
    int countCities();
}
