package edu.uiuc.cs427app;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CityDao {
    // Retrieves all cities from the database.
    @Query("SELECT * FROM cities")
    List<City> getAllCities();

    // Inserts a list of cities into the database.
    @Insert
    void insertAll(List<City> cities);

    // Inserts a single city
    @Insert
    long insert(City city);


    // Finds a city by its name (case-insensitive).
    @Query("SELECT * FROM cities WHERE LOWER(city) = LOWER(:name)")
    List<City> findAllByName(String name);

    // Finds a city by its id.
    @Query("SELECT * FROM cities WHERE id = :id")
    City findById(int id);

    // Counts the total number of cities in the database.
    @Query("SELECT COUNT(*) FROM cities")
    int countCities();
}
