package edu.uiuc.cs427app;

import android.content.Context;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class CSVImporter {

    public static void importCities(Context context) {
        new Thread(() -> {
            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(context.getAssets().open("cities.csv"))
                );

                List<City> cities = new ArrayList<>();
                String line;
                reader.readLine(); // 跳过 header
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    String city = parts[0];
                    String city_ascii = parts[1];
                    String country = parts[2];
                    String iso2 = parts[3];
                    String iso3 = parts[4];
                    String admin_name = parts[5];
                    int id = Integer.parseInt(parts[6]);

                    cities.add(new City(city, city_ascii, country, iso2, iso3, admin_name, id));

                    if (cities.size() >= 500) {
                        DatabaseClient.getInstance(context).getAppDatabase().cityDao().insertAll(cities);
                        cities.clear();
                    }
                }

                if (!cities.isEmpty()) {
                    DatabaseClient.getInstance(context).getAppDatabase().cityDao().insertAll(cities);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
