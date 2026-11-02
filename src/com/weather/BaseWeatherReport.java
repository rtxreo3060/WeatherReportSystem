package com.weather;

/**
 * Abstract Base Class representing core weather information for any location.
 * Demonstrates:
 *  - Abstraction (abstract class with both abstract and concrete methods)
 *  - Encapsulation (private fields, validation in setters and constructors)
 *  - Superclass in Inheritance hierarchy
 */
public abstract class BaseWeatherReport {
    // Encapsulation: Private data fields
    private String cityName;
    private double temperature;       // In Celsius
    private String weatherCondition;  // e.g., Sunny, Cloudy, Rainy, Stormy

    // Default Constructor
    public BaseWeatherReport() {
        this.cityName = "Unknown";
        this.temperature = 0.0;
        this.weatherCondition = "Clear";
    }

    // Parameterized Constructor
    public BaseWeatherReport(String cityName, double temperature, String weatherCondition) 
            throws InvalidWeatherDataException {
        setCityName(cityName);
        setTemperature(temperature);
        setWeatherCondition(weatherCondition);
    }

    // Abstract method to be overridden by derived classes (Polymorphism & Abstraction)
    public abstract void displayReport();

    // Concrete method providing basic summary
    public String getBasicSummary() {
        return String.format("%s: %.1f°C, %s", cityName, temperature, weatherCondition);
    }

    // Getters and Setters with Encapsulation & Validation

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) throws InvalidWeatherDataException {
        if (cityName == null || cityName.trim().isEmpty()) {
            throw new InvalidWeatherDataException("City name cannot be empty or null.");
        }
        this.cityName = cityName.trim();
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) throws InvalidWeatherDataException {
        // Absolute Earth surface temperature limits: approximately -90°C to +60°C
        if (temperature < -90.0 || temperature > 65.0) {
            throw new InvalidWeatherDataException(
                "Temperature " + temperature + "°C is out of realistic earthly range (-90°C to 65°C)."
            );
        }
        this.temperature = temperature;
    }

    public String getWeatherCondition() {
        return weatherCondition;
    }

    public void setWeatherCondition(String weatherCondition) throws InvalidWeatherDataException {
        if (weatherCondition == null || weatherCondition.trim().isEmpty()) {
            throw new InvalidWeatherDataException("Weather condition cannot be empty or null.");
        }
        this.weatherCondition = weatherCondition.trim();
    }

    @Override
    public String toString() {
        return "City: " + cityName + ", Temperature: " + temperature + "°C, Condition: " + weatherCondition;
    }
}
