package com.weather;

import java.util.List;

/**
 * Interface defining the contract for weather management operations.
 * Demonstrates:
 *  - Abstraction (decoupling implementation details from system interface)
 *  - Architecture readiness for plugging in future real-time Weather API services.
 */
public interface WeatherService {
    
    /**
     * Adds a new weather record for a city.
     * Prevents duplicate entries.
     */
    boolean addWeatherReport(WeatherReport report) throws InvalidWeatherDataException;

    /**
     * Retrieves all stored weather reports.
     */
    List<WeatherReport> getAllReports();

    /**
     * Searches for a weather report by city name (case-insensitive).
     */
    WeatherReport searchCity(String cityName);

    /**
     * Updates an existing city's weather metrics.
     */
    boolean updateWeatherReport(String cityName, double temperature, double humidity, 
                                double windSpeed, String condition) throws InvalidWeatherDataException;

    /**
     * Checks if a city record already exists in the system.
     */
    boolean cityExists(String cityName);

    /**
     * Deletes an existing city's weather record (case-insensitive, trimmed).
     *
     * @param cityName The name of the city to delete.
     * @return true if the report was found and removed, false otherwise.
     */
    boolean deleteWeatherReport(String cityName);

    /**
     * Clears all weather reports currently in memory.
     */
    void clearAllReports();

    /**
     * Displays all weather reports in a formatted view.
     */
    void displayAllReports();

    /**
     * Displays only Temperature, Humidity, and Wind Speed for all cities.
     */
    void displayKeyMetrics();

    /**
     * Returns total number of cities stored.
     */
    int getTotalRecordCount();
}
