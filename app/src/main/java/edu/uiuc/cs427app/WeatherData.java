package edu.uiuc.cs427app;

import com.google.gson.annotations.SerializedName;

public class WeatherData {
    @SerializedName("main")
    private Main main;

    @SerializedName("weather")
    private Weather[] weather;

    @SerializedName("wind")
    private Wind wind;

    // return main class
    public Main getMain() {
        return main;
    }

    // return weather data
    public Weather[] getWeather() {
        return weather;
    }

    // returns wind data
    public Wind getWind() {
        return wind;
    }

    public static class Main {
        @SerializedName("temp")
        private double temp;

        @SerializedName("humidity")
        private int humidity;

        // returns the temp
        public double getTemp() {
            return temp;
        }

        // returns humidity
        public int getHumidity() {
            return humidity;
        }
    }

    public static class Weather {
        @SerializedName("main")
        private String main;

        @SerializedName("description")
        private String description;

        // gets main
        public String getMain() {
            return main;
        }

        // gets the weather description
        public String getDescription() {
            return description;
        }
    }

    public static class Wind {
        @SerializedName("speed")
        private double speed;

        @SerializedName("deg")
        private double deg;

        // gets the wind speed
        public double getSpeed() {
            return speed;
        }

        // gets the wind degree
        public double getDeg() {
            return deg;
        }
    }
}
