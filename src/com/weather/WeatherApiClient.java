package com.weather;

/**
 * Interface defining the contract for remote weather API data providers.
 *
 * Demonstrates:
 *  - Abstraction (specifies what operations are required without exposing how network calls are made)
 *  - Interface-based programming (enables polymorphic replacement with Open-Meteo, another API provider, or mock testing clients)
 *  - Exception Specification (explicitly declares checked exceptions in the method signature)
 */
public interface WeatherApiClient {

    /**
     * Fetches real-time weather information for a specified city from an external weather service.
     *
     * @param cityName The name of the city to look up (e.g., "London", "Tokyo").
     * @return A populated, validated {@link WeatherReport} domain object.
     * @throws WeatherApiException        If a network error, timeout, HTTP failure, or city resolution failure occurs.
     * @throws InvalidWeatherDataException If the retrieved meteorological data fails physical boundary constraints.
     */
    WeatherReport fetchCurrentWeather(String cityName) 
            throws WeatherApiException, InvalidWeatherDataException;
}
