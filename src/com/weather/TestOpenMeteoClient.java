package com.weather;

/**
 * Standalone verification runner for OpenMeteoApiClient.
 *
 * Tests:
 *  1. Live weather retrieval for Kolkata.
 *  2. Live weather retrieval for a second valid city (London).
 *  3. Fictional / non-existent city lookup (AtlantisXYZ).
 *  4. Empty / blank input validation.
 *  5. Domain object integrity and polymorphic method execution.
 *  6. WMO weather code mapping translations.
 */
public class TestOpenMeteoClient {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("    OpenMeteoApiClient Verification Test Suite    ");
        System.out.println("==================================================\n");

        WeatherApiClient client = new OpenMeteoApiClient();
        int passed = 0;
        int failed = 0;

        // Test 1: Fetching current weather for Kolkata
        System.out.println("[TEST 1] Fetching live weather for 'Kolkata'...");
        try {
            WeatherReport report = client.fetchCurrentWeather("Kolkata");
            if (report != null && report.getCityName().equalsIgnoreCase("Kolkata")) {
                System.out.println("  -> SUCCESS: Report retrieved for " + report.getCityName());
                report.displayReport();
                passed++;
            } else {
                System.out.println("  -> FAILED: Report is null or city name mismatch.");
                failed++;
            }
        } catch (Exception e) {
            System.out.println("  -> FAILED with exception: " + e.getMessage());
            failed++;
        }

        // Test 2: Fetching current weather for London
        System.out.println("\n[TEST 2] Fetching live weather for 'London'...");
        try {
            WeatherReport report = client.fetchCurrentWeather("London");
            if (report != null && report.getCityName().equalsIgnoreCase("London")) {
                System.out.println("  -> SUCCESS: Report retrieved for " + report.getCityName());
                report.displayReport();
                passed++;
            } else {
                System.out.println("  -> FAILED: Report is null or city name mismatch.");
                failed++;
            }
        } catch (Exception e) {
            System.out.println("  -> FAILED with exception: " + e.getMessage());
            failed++;
        }

        // Test 3: Fictional city 'AtlantisXYZ'
        System.out.println("\n[TEST 3] Testing non-existent city 'AtlantisXYZ'...");
        try {
            client.fetchCurrentWeather("AtlantisXYZ");
            System.out.println("  -> FAILED: Expected WeatherApiException was not thrown.");
            failed++;
        } catch (WeatherApiException e) {
            System.out.println("  -> SUCCESS: Correctly caught WeatherApiException: " + e.getMessage());
            passed++;
        } catch (Exception e) {
            System.out.println("  -> FAILED: Unexpected exception type: " + e.getClass().getName());
            failed++;
        }

        // Test 4: Empty string validation
        System.out.println("\n[TEST 4] Testing empty city name input...");
        try {
            client.fetchCurrentWeather("   ");
            System.out.println("  -> FAILED: Expected WeatherApiException was not thrown.");
            failed++;
        } catch (WeatherApiException e) {
            System.out.println("  -> SUCCESS: Correctly caught WeatherApiException: " + e.getMessage());
            passed++;
        } catch (Exception e) {
            System.out.println("  -> FAILED: Unexpected exception type: " + e.getClass().getName());
            failed++;
        }

        // Test 5: Domain object conversion and Fahrenheit conversion verification
        System.out.println("\n[TEST 5] Testing WeatherReport domain integration & conversion...");
        try {
            WeatherReport report = client.fetchCurrentWeather("Tokyo");
            double fahrenheit = report.getTemperatureInFahrenheit();
            double celsius = report.getTemperature();
            double expectedF = (celsius * 9.0 / 5.0) + 32.0;

            if (Math.abs(fahrenheit - expectedF) < 0.001) {
                System.out.printf("  -> SUCCESS: %.1f °C correctly converted to %.1f °F%n", celsius, fahrenheit);
                passed++;
            } else {
                System.out.println("  -> FAILED: Fahrenheit conversion calculation error.");
                failed++;
            }
        } catch (Exception e) {
            System.out.println("  -> FAILED with exception: " + e.getMessage());
            failed++;
        }

        // Test 6: WMO weather code translation coverage
        System.out.println("\n[TEST 6] Testing WMO code mapping translations...");
        boolean wmoPass = true;
        if (!OpenMeteoApiClient.mapWmoCodeToCondition(0).equals("Clear sky")) wmoPass = false;
        if (!OpenMeteoApiClient.mapWmoCodeToCondition(61).equals("Slight Rain")) wmoPass = false;
        if (!OpenMeteoApiClient.mapWmoCodeToCondition(95).equals("Thunderstorm")) wmoPass = false;
        if (!OpenMeteoApiClient.mapWmoCodeToCondition(999).contains("WMO Code 999")) wmoPass = false;

        if (wmoPass) {
            System.out.println("  -> SUCCESS: WMO weather codes mapped correctly.");
            passed++;
        } else {
            System.out.println("  -> FAILED: WMO weather code mapping mismatch.");
            failed++;
        }

        // Summary
        System.out.println("\n==================================================");
        System.out.printf("  TEST RESULTS: Passed: %d, Failed: %d%n", passed, failed);
        System.out.println("==================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }
}
