package edu.uiuc.cs427app;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {City.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract CityDao cityDao();
}
