package com.weather;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service implementation class that manages weather records using Java Collections.
 * Implements WeatherService interface.
 * Demonstrates:
 *  - Collections Framework (ArrayList)
 *  - Encapsulation (encapsulating collection and business rules)
 *  - Polymorphism / Abstraction (implements interface methods)
 *  - Exception Handling
 */
public class WeatherManager implements WeatherService {
    // Encapsulated collection of weather reports
    private final List<WeatherReport> weatherReports;

    /**
     * Constructor initializing the internal collection.
     */
    public WeatherManager() {
        this.weatherReports = new ArrayList<>();
    }

    /**
     * Adds a new weather report.
     * Prevents duplicate city entries (case-insensitive).
     */
    @Override
    public boolean addWeatherReport(WeatherReport report) throws InvalidWeatherDataException {
        if (report == null) {
            throw new InvalidWeatherDataException("Weather report object cannot be null.");
        }

        // Check for duplicates
        if (cityExists(report.getCityName())) {
            return false; // Duplicate found
        }

        weatherReports.add(report);
        return true;
    }

    /**
     * Retrieves an unmodifiable view of all reports to preserve encapsulation.
     */
    @Override
    public List<WeatherReport> getAllReports() {
        return Collections.unmodifiableList(weatherReports);
    }

    /**
     * Case-insensitive search for a weather report by city name.
     */
    @Override
    public WeatherReport searchCity(String cityName) {
        if (cityName == null || cityName.trim().isEmpty()) {
            return null;
        }
        String searchTarget = cityName.trim();
        for (WeatherReport report : weatherReports) {
            if (report.getCityName().equalsIgnoreCase(searchTarget)) {
                return report;
            }
        }
        return null;
    }

    /**
     * Checks if a city record already exists in the system.
     */
    @Override
    public boolean cityExists(String cityName) {
        return searchCity(cityName) != null;
    }

    /**
     * Updates an existing city's weather record while preserving its name.
     */
    @Override
    public boolean updateWeatherReport(String cityName, double temperature, double humidity,
                                       double windSpeed, String condition) throws InvalidWeatherDataException {
        WeatherReport existingReport = searchCity(cityName);
        if (existingReport == null) {
            return false; // City not found
        }

        // Apply new values (setters will throw InvalidWeatherDataException if invalid)
        existingReport.setTemperature(temperature);
        existingReport.setHumidity(humidity);
        existingReport.setWindSpeed(windSpeed);
        existingReport.setWeatherCondition(condition);
        existingReport.refreshTimestamp();
        return true;
    }

    /**
     * Displays all weather reports currently in the system.
     */
    @Override
    public void displayAllReports() {
        if (weatherReports.isEmpty()) {
            System.out.println("\n[!] No weather records available in the system.");
            System.out.println("    Use Option 1 from the menu to add new weather records.\n");
            return;
        }

        System.out.println("\n==================================================");
        System.out.printf("       ALL STORED WEATHER REPORTS (Total: %d)     %n", weatherReports.size());
        System.out.println("==================================================");
        for (int i = 0; i < weatherReports.size(); i++) {
            System.out.printf("%nRecord #%d:%n", i + 1);
            weatherReports.get(i).displayReport();
        }
    }

    /**
     * Displays only Temperature, Humidity, and Wind Speed in a clean tabular view.
     */
    @Override
    public void displayKeyMetrics() {
        if (weatherReports.isEmpty()) {
            System.out.println("\n[!] No weather records available to display key metrics.");
            System.out.println("    Please add city weather records first.\n");
            return;
        }

        System.out.println("\n========================================================================================");
        System.out.println("               CITY WEATHER METRICS (TEMPERATURE, HUMIDITY & WIND SPEED)                ");
        System.out.println("========================================================================================");
        System.out.printf("%-4s | %-18s | %-18s | %-16s | %-16s%n", 
                "No.", "City Name", "Temperature", "Humidity", "Wind Speed");
        System.out.println("-----+--------------------+--------------------+------------------+---------------------");

        int index = 1;
        for (WeatherReport report : weatherReports) {
            String tempStr = String.format("%.1f °C", report.getTemperature());
            String humStr = String.format("%.1f %%", report.getHumidity());
            String windStr = String.format("%.1f km/h", report.getWindSpeed());
            
            System.out.printf("%-4d | %-18s | %-18s | %-16s | %-16s%n",
                    index++, report.getCityName(), tempStr, humStr, windStr);
        }
        System.out.println("========================================================================================\n");
    }

    /**
     * Returns total count of records stored.
     */
    @Override
    public int getTotalRecordCount() {
        return weatherReports.size();
    }
}
