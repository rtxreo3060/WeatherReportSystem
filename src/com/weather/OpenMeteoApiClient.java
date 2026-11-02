package com.weather;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

/**
 * Production implementation of WeatherApiClient utilizing the Open-Meteo REST APIs.
 *
 * Architecture & Features:
 *  - Communicates over HTTP via Java 11+ HttpClient.
 *  - Resolves city coordinates using Open-Meteo Geocoding API.
 *  - Fetches current weather metrics using Open-Meteo Forecast API.
 *  - Parses JSON responses into structured DTOs using Google Gson.
 *  - Translates WMO weather interpretation codes into user-friendly conditions.
 *  - Reuses the existing WeatherReport domain model.
 *
 * Demonstrates:
 *  - Abstraction & Polymorphism (implements WeatherApiClient)
 *  - Encapsulation (hides HTTP transport, URL formatting, and JSON deserialization)
 *  - Exception Handling (translates low-level IO/interrupted exceptions into checked domain exceptions)
 */
public class OpenMeteoApiClient implements WeatherApiClient {

    private static final String GEOCODING_BASE_URL = "https://geocoding-api.open-meteo.com/v1/search";
    private static final String FORECAST_BASE_URL = "https://api.open-meteo.com/v1/forecast";

    private static final Duration TIMEOUT_DURATION = Duration.ofSeconds(10);

    private final HttpClient httpClient;
    private final Gson gson;

    /**
     * Default constructor initializing standard HttpClient and Gson instances.
     */
    public OpenMeteoApiClient() {
        this(HttpClient.newBuilder()
                .connectTimeout(TIMEOUT_DURATION)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build(),
             new Gson());
    }

    /**
     * Parameterized constructor allowing custom HttpClient and Gson injection.
     * Demonstrates Dependency Injection and testability.
     *
     * @param httpClient Configured Java HttpClient instance.
     * @param gson       Configured Gson JSON parser instance.
     */
    public OpenMeteoApiClient(HttpClient httpClient, Gson gson) {
        if (httpClient == null) {
            throw new IllegalArgumentException("HttpClient cannot be null.");
        }
        if (gson == null) {
            throw new IllegalArgumentException("Gson instance cannot be null.");
        }
        this.httpClient = httpClient;
        this.gson = gson;
    }

    /**
     * Fetches real-time weather information for the specified city from Open-Meteo.
     *
     * @param cityName The city name to look up.
     * @return Fully populated and validated WeatherReport instance.
     * @throws WeatherApiException        If network error, non-200 HTTP status, city not found, or malformed JSON.
     * @throws InvalidWeatherDataException If returned values breach physical domain ranges.
     */
    @Override
    public WeatherReport fetchCurrentWeather(String cityName)
            throws WeatherApiException, InvalidWeatherDataException {

        if (cityName == null || cityName.trim().isEmpty()) {
            throw new WeatherApiException("City name cannot be empty or null.");
        }

        String trimmedCity = cityName.trim();

        // Step 1: Geocoding - Resolve city coordinates
        GeocodingResult location = resolveCityCoordinates(trimmedCity);

        // Step 2: Forecast - Fetch real-time weather metrics
        CurrentWeather current = fetchForecastData(location.latitude, location.longitude, location.name);

        // Step 3: Map WMO code to human-readable weather condition
        String condition = mapWmoCodeToCondition(current.weather_code);

        // Step 4: Construct and validate domain WeatherReport object
        return new WeatherReport(
                location.name,
                current.temperature_2m,
                current.relative_humidity_2m,
                current.wind_speed_10m,
                condition
        );
    }

    /**
     * Resolves the latitude and longitude of a city using Open-Meteo Geocoding API.
     */
    private GeocodingResult resolveCityCoordinates(String cityName) throws WeatherApiException {
        String encodedCity = URLEncoder.encode(cityName, StandardCharsets.UTF_8);
        String url = String.format("%s?name=%s&count=5&language=en&format=json", GEOCODING_BASE_URL, encodedCity);

        String jsonResponse = executeHttpGet(url, "Geocoding");

        GeocodingApiResponse response;
        try {
            response = gson.fromJson(jsonResponse, GeocodingApiResponse.class);
        } catch (JsonSyntaxException e) {
            throw new WeatherApiException("Failed to parse geocoding response from Open-Meteo (invalid JSON).", e);
        }

        if (response == null || response.results == null || response.results.isEmpty()) {
            throw new WeatherApiException("City '" + cityName + "' could not be found. Please check the spelling.");
        }

        return selectBestLocationMatch(cityName, response.results);
    }

    /**
     * Selects the most appropriate location from geocoding search results.
     * Prioritizes exact case-insensitive name match before falling back to top ranked match.
     */
    private GeocodingResult selectBestLocationMatch(String queryCity, List<GeocodingResult> candidates) {
        // Priority 1: Check for exact case-insensitive match
        for (GeocodingResult candidate : candidates) {
            if (candidate.name != null && candidate.name.equalsIgnoreCase(queryCity)) {
                return candidate;
            }
        }
        // Priority 2: Fall back to top-ranked candidate (Open-Meteo orders by population/relevance)
        return candidates.get(0);
    }

    /**
     * Retrieves current weather parameters from Open-Meteo Forecast API.
     */
    private CurrentWeather fetchForecastData(double latitude, double longitude, String resolvedCityName)
            throws WeatherApiException {

        // Explicitly request wind_speed_unit=kmh to match WeatherReport domain model
        String url = String.format(
                Locale.US,
                "%s?latitude=%.5f&longitude=%.5f&current=temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code&wind_speed_unit=kmh",
                FORECAST_BASE_URL,
                latitude,
                longitude
        );

        String jsonResponse = executeHttpGet(url, "Weather Forecast");

        ForecastApiResponse response;
        try {
            response = gson.fromJson(jsonResponse, ForecastApiResponse.class);
        } catch (JsonSyntaxException e) {
            throw new WeatherApiException("Failed to parse forecast response from Open-Meteo (invalid JSON).", e);
        }

        if (response == null || response.current == null) {
            throw new WeatherApiException("Current weather data for '" + resolvedCityName + "' is unavailable.");
        }

        CurrentWeather current = response.current;
        if (current.temperature_2m == null || current.relative_humidity_2m == null
                || current.wind_speed_10m == null || current.weather_code == null) {
            throw new WeatherApiException("Incomplete weather data fields received for '" + resolvedCityName + "'.");
        }

        return current;
    }

    /**
     * Executes an HTTP GET request with standard headers and robust timeout/status handling.
     */
    private String executeHttpGet(String url, String requestType) throws WeatherApiException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT_DURATION)
                .header("Accept", "application/json")
                .header("User-Agent", "WeatherReportSystem-CollegeProject/1.0")
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new WeatherApiException(
                    "Network error while connecting to " + requestType + " service. Check your internet connection.",
                    e
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupted status
            throw new WeatherApiException(
                    requestType + " request was interrupted before completion.",
                    e
            );
        }

        int statusCode = response.statusCode();
        if (statusCode != 200) {
            throw new WeatherApiException(
                    requestType + " service returned HTTP status " + statusCode + " (Request failed)."
            );
        }

        String body = response.body();
        if (body == null || body.trim().isEmpty()) {
            throw new WeatherApiException(requestType + " service returned an empty response body.");
        }

        return body;
    }

    /**
     * Translates World Meteorological Organization (WMO) Weather Interpretation Codes
     * into clear, beginner-friendly textual weather descriptions.
     *
     * Reference: Open-Meteo WMO Weather interpretation codes table.
     *
     * @param wmoCode Numeric code representing the current weather phenomenon.
     * @return Readable string representation of the weather condition.
     */
    public static String mapWmoCodeToCondition(int wmoCode) {
        switch (wmoCode) {
            case 0:
                return "Clear sky";
            case 1:
                return "Mainly clear";
            case 2:
                return "Partly cloudy";
            case 3:
                return "Overcast";
            case 45:
                return "Fog";
            case 48:
                return "Depositing rime fog";
            case 51:
                return "Light Drizzle";
            case 53:
                return "Moderate Drizzle";
            case 55:
                return "Dense Drizzle";
            case 56:
                return "Light Freezing Drizzle";
            case 57:
                return "Dense Freezing Drizzle";
            case 61:
                return "Slight Rain";
            case 63:
                return "Moderate Rain";
            case 65:
                return "Heavy Rain";
            case 66:
                return "Light Freezing Rain";
            case 67:
                return "Heavy Freezing Rain";
            case 71:
                return "Slight Snow fall";
            case 73:
                return "Moderate Snow fall";
            case 75:
                return "Heavy Snow fall";
            case 77:
                return "Snow grains";
            case 80:
                return "Slight Rain showers";
            case 81:
                return "Moderate Rain showers";
            case 82:
                return "Violent Rain showers";
            case 85:
                return "Slight Snow showers";
            case 86:
                return "Heavy Snow showers";
            case 95:
                return "Thunderstorm";
            case 96:
                return "Thunderstorm with slight hail";
            case 99:
                return "Thunderstorm with heavy hail";
            default:
                return "Atmospheric Condition (WMO Code " + wmoCode + ")";
        }
    }

    // =========================================================================
    // Inner Data Transfer Objects (DTOs) for Gson Deserialization
    // Demonstrates: Data Transfer Object (DTO) pattern & Encapsulation
    // =========================================================================

    /**
     * DTO mapping Open-Meteo Geocoding search response.
     */
    private static class GeocodingApiResponse {
        List<GeocodingResult> results;
    }

    /**
     * DTO representing an individual location result from the Geocoding API.
     */
    static class GeocodingResult {
        String name;
        double latitude;
        double longitude;
        String country;
        String admin1;
    }

    /**
     * DTO mapping Open-Meteo Forecast response.
     */
    private static class ForecastApiResponse {
        CurrentWeather current;
    }

    /**
     * DTO representing current meteorological metrics returned by Open-Meteo.
     */
    static class CurrentWeather {
        Double temperature_2m;
        Double relative_humidity_2m;
        Double wind_speed_10m;
        Integer weather_code;
    }
}
