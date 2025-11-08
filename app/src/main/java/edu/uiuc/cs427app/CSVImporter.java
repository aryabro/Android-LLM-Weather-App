package edu.uiuc.cs427app;

import android.content.Context;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class CSVImporter {

    // Imports city data from a CSV file and stores it in the database.
    public static void importCities(Context context) {
        new Thread(() -> {
            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(context.getAssets().open("cities.csv")));

                List<City> cities = new ArrayList<>();
                String line;
                reader.readLine(); // Skip the header
                while ((line = reader.readLine()) != null) {
                    List<String> parts = parseCsvLine(line);
                    if (parts.size() < 8) {
                        continue;
                    }
                    String city = parts.get(0).trim();
                    double lat;
                    double lng;
                    String country = parts.get(3).trim();
                    String iso2 = parts.get(4).trim();
                    String iso3 = parts.get(5).trim();
                    String admin_name = parts.get(6).trim();
                    int id;
                    try {
                        lat = Double.parseDouble(parts.get(1).trim());
                        lng = Double.parseDouble(parts.get(2).trim());
                        id = Integer.parseInt(parts.get(7).trim());
                    } catch (NumberFormatException nfe) {
                        continue;
                    }

                    cities.add(new City(city, lat, lng, country, iso2, iso3, admin_name, id));

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

    // Parse CSV line handling quoted fields
    private static List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        if (line == null) {
            return result;
        }
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        return result;
    }
}
