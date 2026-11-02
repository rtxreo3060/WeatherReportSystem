package com.weather;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Derived Class representing a comprehensive weather report for a city.
 * Extends BaseWeatherReport.
 * Demonstrates:
 *  - Inheritance (subclass of BaseWeatherReport)
 *  - Polymorphism (Method Overriding and Method Overloading)
 *  - Encapsulation (private fields, validation in setters)
 *  - Constructors (invoking super(...) constructor)
 */
public class WeatherReport extends BaseWeatherReport {
    // Encapsulation: additional private fields
    private double humidity;   // In percentage (0.0% to 100.0%)
    private double windSpeed;  // In km/h (must be non-negative)
    private String lastUpdated;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss");

    // Default Constructor
    public WeatherReport() {
        super();
        this.humidity = 0.0;
        this.windSpeed = 0.0;
        this.lastUpdated = LocalDateTime.now().format(FORMATTER);
    }

    // Parameterized Constructor
    public WeatherReport(String cityName, double temperature, double humidity, double windSpeed, String weatherCondition)
            throws InvalidWeatherDataException {
        super(cityName, temperature, weatherCondition); // Invoking superclass constructor
        setHumidity(humidity);
        setWindSpeed(windSpeed);
        this.lastUpdated = LocalDateTime.now().format(FORMATTER);
    }

    // Overloaded Constructor allowing custom timestamp
    public WeatherReport(String cityName, double temperature, double humidity, double windSpeed, 
                         String weatherCondition, String lastUpdated) throws InvalidWeatherDataException {
        super(cityName, temperature, weatherCondition);
        setHumidity(humidity);
        setWindSpeed(windSpeed);
        this.lastUpdated = lastUpdated;
    }

    // Getters and Setters with Validation

    public double getHumidity() {
        return humidity;
    }

    public void setHumidity(double humidity) throws InvalidWeatherDataException {
        if (!Double.isFinite(humidity) || humidity < 0.0 || humidity > 100.0) {
            throw new InvalidWeatherDataException(
                "Humidity (" + humidity + "%) is invalid. It must be between 0.0% and 100.0%."
            );
        }
        this.humidity = humidity;
    }

    public double getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(double windSpeed) throws InvalidWeatherDataException {
        if (!Double.isFinite(windSpeed) || windSpeed < 0.0) {
            throw new InvalidWeatherDataException(
                "Wind speed (" + windSpeed + " km/h) cannot be negative."
            );
        }
        this.windSpeed = windSpeed;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public void refreshTimestamp() {
        this.lastUpdated = LocalDateTime.now().format(FORMATTER);
    }

    // Utility method: Temperature conversion
    public double getTemperatureInFahrenheit() {
        return (getTemperature() * 9.0 / 5.0) + 32.0;
    }

    // Polymorphism: Method Overriding (Overriding displayReport from BaseWeatherReport)
    @Override
    public void displayReport() {
        displayReport(true); // delegates to overloaded method
    }

    // Polymorphism: Method Overloading (same method name, different parameter signature)
    public void displayReport(boolean showDetails) {
        System.out.println("--------------------------------------------------");
        System.out.printf("  City Name        : %s%n", getCityName());
        System.out.printf("  Temperature      : %.1f °C (%.1f °F)%n", getTemperature(), getTemperatureInFahrenheit());
        System.out.printf("  Humidity         : %.1f %%%n", humidity);
        System.out.printf("  Wind Speed       : %.1f km/h%n", windSpeed);
        System.out.printf("  Condition        : %s%n", getWeatherCondition());
        if (showDetails) {
            System.out.printf("  Last Updated     : %s%n", lastUpdated);
        }
        System.out.println("--------------------------------------------------");
    }

    @Override
    public String toString() {
        return String.format(
            "WeatherReport [City=%s, Temp=%.1f°C, Humidity=%.1f%%, Wind=%.1f km/h, Condition=%s, Updated=%s]",
            getCityName(), getTemperature(), humidity, windSpeed, getWeatherCondition(), lastUpdated
        );
    }
}
