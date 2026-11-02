package com.weather;

import java.util.Scanner;

/**
 * Main application class providing a console-based, menu-driven user interface.
 * Coordinates user interaction with the WeatherService.
 * Demonstrates:
 *  - Separation of Concerns (UI separated from business logic)
 *  - Abstraction (working through WeatherService interface)
 *  - Exception Handling (validating inputs, try-catch blocks)
 *  - Robust Scanner input processing
 */
public class Main {

    private static final WeatherService weatherService = new WeatherManager();
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        boolean running = true;

        System.out.println("==================================================");
        System.out.println("       Welcome to the Weather Report System       ");
        System.out.println("==================================================");

        while (running) {
            displayMenu();
            int choice = readIntegerInput("Enter your choice (1-6): ", 1, 6);

            switch (choice) {
                case 1:
                    handleAddWeather();
                    break;
                case 2:
                    handleDisplayAllReports();
                    break;
                case 3:
                    handleSearchCity();
                    break;
                case 4:
                    handleUpdateWeather();
                    break;
                case 5:
                    handleDisplayKeyMetrics();
                    break;
                case 6:
                    running = false;
                    System.out.println("\n[SUCCESS] Thank you for using the Weather Report System. Exiting application.");
                    break;
                default:
                    System.out.println("\n[!] Invalid selection. Please choose an option between 1 and 6.");
            }

            if (running) {
                promptEnterKey();
            }
        }

        scanner.close();
    }

    /**
     * Prints the primary application menu.
     */
    private static void displayMenu() {
        System.out.println("\n========== WEATHER REPORT SYSTEM ==========");
        System.out.println("  1. Add Weather Information");
        System.out.println("  2. Display All Weather Reports");
        System.out.println("  3. Search Weather by City");
        System.out.println("  4. Update Weather Information");
        System.out.println("  5. Display Temperature, Humidity and Wind Speed");
        System.out.println("  6. Exit");
        System.out.println("===========================================");
    }

    /**
     * Handles Requirement 1: Adding weather information for a city.
     */
    private static void handleAddWeather() {
        System.out.println("\n--- [ Add Weather Information ] ---");
        String cityName = readNonEmptyString("Enter City Name: ");

        // Check if city already exists before asking for all metrics
        if (weatherService.cityExists(cityName)) {
            System.out.printf("%n[!] A weather record for '%s' already exists.%n", cityName);
            System.out.println("    Duplicate entries are not permitted. Use Option 4 to update this city's records.\n");
            return;
        }

        double temperature = readDoubleInput("Enter Temperature in Celsius (-90.0 to 65.0): ", -90.0, 65.0);
        double humidity = readDoubleInput("Enter Humidity Percentage (0.0 to 100.0): ", 0.0, 100.0);
        double windSpeed = readDoubleInput("Enter Wind Speed in km/h (>= 0.0): ", 0.0, 500.0);
        String condition = readWeatherCondition();

        try {
            WeatherReport newReport = new WeatherReport(cityName, temperature, humidity, windSpeed, condition);
            boolean added = weatherService.addWeatherReport(newReport);
            if (added) {
                System.out.printf("%n[SUCCESS] Weather report for '%s' successfully recorded!%n", cityName);
            } else {
                System.out.printf("%n[!] Failed to add weather report for '%s'. City already exists.%n", cityName);
            }
        } catch (InvalidWeatherDataException e) {
            System.out.printf("%n[!] Validation Error: %s%n", e.getMessage());
        }
    }

    /**
     * Handles Requirement 2: Displaying weather reports for all cities.
     */
    private static void handleDisplayAllReports() {
        System.out.println("\n--- [ All Weather Reports ] ---");
        weatherService.displayAllReports();
    }

    /**
     * Handles Requirement 3: Searching weather report by city name (case-insensitive).
     */
    private static void handleSearchCity() {
        System.out.println("\n--- [ Search Weather by City ] ---");
        String cityName = readNonEmptyString("Enter City Name to search: ");

        WeatherReport report = weatherService.searchCity(cityName);
        if (report != null) {
            System.out.printf("%n[FOUND] Match found for '%s':%n", report.getCityName());
            report.displayReport();
        } else {
            System.out.printf("%n[!] No weather report found for city '%s'. Please check spelling.%n", cityName);
        }
    }

    /**
     * Handles Requirement 4: Updating an existing city's weather record.
     */
    private static void handleUpdateWeather() {
        System.out.println("\n--- [ Update Weather Information ] ---");
        String cityName = readNonEmptyString("Enter City Name to update: ");

        WeatherReport existingReport = weatherService.searchCity(cityName);
        if (existingReport == null) {
            System.out.printf("%n[!] City '%s' is not registered in the system.%n", cityName);
            System.out.println("    Cannot update a non-existent record. Please use Option 1 to add it first.\n");
            return;
        }

        System.out.printf("%n[i] Current Record for '%s':%n", existingReport.getCityName());
        existingReport.displayReport();

        System.out.println("\nEnter updated values:");
        double newTemp = readDoubleInput("Enter New Temperature in Celsius (-90.0 to 65.0): ", -90.0, 65.0);
        double newHum = readDoubleInput("Enter New Humidity Percentage (0.0 to 100.0): ", 0.0, 100.0);
        double newWind = readDoubleInput("Enter New Wind Speed in km/h (>= 0.0): ", 0.0, 500.0);
        String newCond = readWeatherCondition();

        try {
            boolean updated = weatherService.updateWeatherReport(existingReport.getCityName(), newTemp, newHum, newWind, newCond);
            if (updated) {
                System.out.printf("%n[SUCCESS] Weather information for '%s' updated successfully!%n", existingReport.getCityName());
                System.out.println("Updated Report:");
                existingReport.displayReport();
            } else {
                System.out.printf("%n[!] Could not update weather information for '%s'.%n", existingReport.getCityName());
            }
        } catch (InvalidWeatherDataException e) {
            System.out.printf("%n[!] Update failed due to invalid data: %s%n", e.getMessage());
        }
    }

    /**
     * Handles Requirement 5: Displaying Temperature, Humidity, and Wind Speed only.
     */
    private static void handleDisplayKeyMetrics() {
        weatherService.displayKeyMetrics();
    }

    // ==========================================
    // Robust User Input & Validation Utilities
    // ==========================================

    /**
     * Reads a non-empty, trimmed string from the console.
     */
    private static String readNonEmptyString(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) {
                return input;
            }
            System.out.println("[!] Input cannot be empty. Please enter a valid text.");
        }
    }

    /**
     * Prompts the user to pick or type a weather condition.
     */
    private static String readWeatherCondition() {
        System.out.println("Select Weather Condition:");
        System.out.println("  1. Sunny");
        System.out.println("  2. Cloudy");
        System.out.println("  3. Rainy");
        System.out.println("  4. Stormy");
        System.out.println("  5. Foggy");
        System.out.println("  6. Other (Type manually)");

        int choice = readIntegerInput("Enter condition option (1-6): ", 1, 6);
        switch (choice) {
            case 1: return "Sunny";
            case 2: return "Cloudy";
            case 3: return "Rainy";
            case 4: return "Stormy";
            case 5: return "Foggy";
            case 6: return readNonEmptyString("Enter custom weather condition: ");
            default: return "Sunny";
        }
    }

    /**
     * Reads an integer within [min, max] range with robust error handling.
     */
    private static int readIntegerInput(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.printf("[!] Please enter an integer between %d and %d.%n", min, max);
            } catch (NumberFormatException e) {
                System.out.println("[!] Invalid input. Please enter a valid whole number.");
            }
        }
    }

    /**
     * Reads a double within [min, max] range with robust error handling.
     */
    private static double readDoubleInput(String prompt, double min, double max) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                double value = Double.parseDouble(line);
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.printf("[!] Value must be between %.1f and %.1f.%n", min, max);
            } catch (NumberFormatException e) {
                System.out.println("[!] Invalid numeric format. Please enter a valid decimal or number.");
            }
        }
    }

    /**
     * Pauses the console and asks the user to press Enter before displaying menu again.
     */
    private static void promptEnterKey() {
        System.out.print("Press [Enter] to return to the main menu...");
        scanner.nextLine();
    }
}
