package com.weather;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Main Driver class providing a console-based User Interface for the Weather Report System.
 * Demonstrates:
 *  - Separation of Concerns / Modular Programming (UI separate from Domain and Service logic)
 *  - Interactive Console Application Design with robust input validation
 *  - Integration of live API services (OpenMeteoApiClient) with fallback capabilities
 *  - Integration of persistent storage (WeatherStorage) with integrity and corruption safeguards
 */
public class Main {

    private static final WeatherService weatherService = new WeatherManager();
    private static final WeatherApiClient weatherApiClient = new OpenMeteoApiClient();
    private static final WeatherStorage weatherStorage = new WeatherStorage();
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        boolean running = true;

        System.out.println("==================================================");
        System.out.println("       Welcome to the Weather Report System       ");
        System.out.println("==================================================");

        // Load existing weather reports from persistent storage
        loadPersistedReports();

        while (running) {
            displayMenu();
            int choice = readIntegerInput("Enter your choice (1-8): ", 1, 8);

            switch (choice) {
                case 1:
                    handleAddWeather();
                    break;
                case 2:
                    handleFetchLiveWeather();
                    break;
                case 3:
                    handleDisplayAllReports();
                    break;
                case 4:
                    handleSearchCity();
                    break;
                case 5:
                    handleUpdateWeather();
                    break;
                case 6:
                    handleDisplayKeyMetrics();
                    break;
                case 7:
                    handleDeleteSavedWeather();
                    break;
                case 8:
                    running = false;
                    System.out.println("\n[SUCCESS] Thank you for using the Weather Report System. Exiting application.");
                    break;
                default:
                    System.out.println("\n[!] Invalid selection. Please choose an option between 1 and 8.");
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
        System.out.println("  1. Add Weather Information Manually");
        System.out.println("  2. Fetch Live Weather from Open-Meteo API");
        System.out.println("  3. Display All Weather Reports");
        System.out.println("  4. Search Weather by City");
        System.out.println("  5. Update Weather Information");
        System.out.println("  6. Display Temperature, Humidity and Wind Speed");
        System.out.println("  7. Delete Saved Weather Reports");
        System.out.println("  8. Exit");
        System.out.println("===========================================");
    }

    /**
     * Loads existing weather reports from persistent JSON storage into WeatherService
     * before displaying the main menu.
     */
    private static void loadPersistedReports() {
        List<WeatherReport> loadedReports = weatherStorage.loadReports();
        if (!loadedReports.isEmpty()) {
            int successCount = 0;
            for (WeatherReport report : loadedReports) {
                try {
                    boolean added = weatherService.addWeatherReport(report);
                    if (added) {
                        successCount++;
                    } else {
                        System.err.printf("[!] Warning: Duplicate city '%s' from storage was not added to memory.%n", report.getCityName());
                    }
                } catch (InvalidWeatherDataException e) {
                    System.err.printf("[!] Skipped invalid stored record: %s%n", e.getMessage());
                }
            }
            if (successCount > 0) {
                System.out.printf("%n[INFO] Loaded %d weather report(s) from persistent storage.%n", successCount);
            }
        }
        if (weatherStorage.isCorruptionDetected()) {
            System.err.printf("%n[!] NOTICE: Storage file '%s' has integrity issues or duplicate records.%n", weatherStorage.getFilePath());
            System.err.println("    Disk saves are disabled for this session to prevent data loss. Please resolve the file manually.");
        }
    }

    /**
     * Handles Option 1: Adding weather information for a city manually.
     */
    private static void handleAddWeather() {
        System.out.println("\n--- Add Weather Report Manually ---");
        String cityName = readNonEmptyString("Enter City Name: ");

        // Check if city already exists
        if (weatherService.cityExists(cityName)) {
            System.out.printf("%n[!] A weather report for '%s' already exists in the system.%n", cityName);
            System.out.println("    Please use Option 5 from the main menu to update existing city records.");
            return;
        }

        promptAndSaveManualReport(cityName);
    }

    /**
     * Prompts for physical weather metrics and records the report in WeatherService and WeatherStorage.
     * Transactional pattern: Persists proposed state to JSON first, and only mutates WeatherService after save succeeds.
     */
    private static void promptAndSaveManualReport(String cityName) {
        if (weatherService.cityExists(cityName)) {
            System.out.printf("%n[!] Failed to add weather report for '%s'. City already exists.%n", cityName);
            return;
        }

        double temperature = readDoubleInput("Enter Temperature in °C (-90.0 to 65.0): ", -90.0, 65.0);
        double humidity = readDoubleInput("Enter Relative Humidity % (0.0 to 100.0): ", 0.0, 100.0);
        double windSpeed = readDoubleInput("Enter Wind Speed in km/h (>= 0.0): ", 0.0, 500.0);
        String condition = readWeatherCondition();

        try {
            WeatherReport newReport = new WeatherReport(cityName, temperature, humidity, windSpeed, condition);

            // Prepare proposed state list without mutating in-memory service first
            List<WeatherReport> proposedReports = new ArrayList<>(weatherService.getAllReports());
            proposedReports.add(newReport);

            // Attempt to persist proposed state to storage
            boolean saved = weatherStorage.saveReports(proposedReports);
            if (saved) {
                // Apply in-memory mutation only after successful persistence
                weatherService.addWeatherReport(newReport);
                System.out.printf("%n[SUCCESS] Weather report for '%s' recorded and successfully saved to disk!%n", cityName);
            } else {
                // In-memory state remains completely unchanged!
                System.out.printf("%n[ERROR] Failed to save weather report for '%s' to persistent storage.%n", cityName);
                System.out.println("        Operation aborted. In-memory records remain unchanged.");
            }
        } catch (InvalidWeatherDataException e) {
            System.out.printf("%n[!] Error creating weather report: %s%n", e.getMessage());
        }
    }

    /**
     * Handles Option 2: Fetching live weather data from the Open-Meteo REST API.
     * Transactional pattern: Persists proposed state to JSON first, and only mutates WeatherService after save succeeds.
     */
    private static void handleFetchLiveWeather() {
        System.out.println("\n--- Fetch Live Weather from Open-Meteo API ---");
        String cityName = readNonEmptyString("Enter City Name: ");

        // Check if city already exists before querying the API
        if (weatherService.cityExists(cityName)) {
            System.out.printf("%n[!] A weather report for '%s' already exists in the system.%n", cityName);
            System.out.println("    Please use Option 5 from the main menu to update existing city records.");
            return;
        }

        System.out.printf("%n[INFO] Querying Open-Meteo API for '%s'...%n", cityName);

        try {
            // Attempt to fetch live weather via WeatherApiClient
            WeatherReport liveReport = weatherApiClient.fetchCurrentWeather(cityName);

            // Display fetched report
            System.out.println("\n--------------------------------------------------");
            System.out.printf("  Live Weather Report Retrieved for '%s'%n", liveReport.getCityName());
            System.out.println("--------------------------------------------------");
            liveReport.displayReport();

            // Check if record already exists before proposing save
            if (weatherService.cityExists(liveReport.getCityName())) {
                System.out.printf("[!] Could not save report: A record for '%s' already exists.%n", liveReport.getCityName());
                return;
            }

            // Prepare proposed state list without mutating in-memory service first
            List<WeatherReport> proposedReports = new ArrayList<>(weatherService.getAllReports());
            proposedReports.add(liveReport);

            // Attempt to persist proposed state to storage
            boolean saved = weatherStorage.saveReports(proposedReports);
            if (saved) {
                // Apply in-memory mutation only after successful persistence
                weatherService.addWeatherReport(liveReport);
                System.out.printf("[SUCCESS] Live report for '%s' has been recorded and saved to disk.%n", liveReport.getCityName());
            } else {
                // In-memory state remains completely unchanged!
                System.out.printf("%n[ERROR] Failed to save live report for '%s' to persistent storage.%n", liveReport.getCityName());
                System.out.println("        Operation aborted. In-memory records remain unchanged.");
            }

        } catch (WeatherApiException e) {
            System.out.printf("%n[!] Live API Error: %s%n", e.getMessage());
            System.out.println("    Live weather data could not be retrieved.");

            // Offer fallback to manual entry
            System.out.print("\nWould you like to manually enter weather data for this city instead? (yes/no): ");
            String response = scanner.nextLine().trim();
            if (response.equalsIgnoreCase("yes") || response.equalsIgnoreCase("y")) {
                if (weatherService.cityExists(cityName)) {
                    System.out.printf("%n[!] A record for '%s' already exists. Use Option 5 to update.%n", cityName);
                } else {
                    System.out.printf("%n--- Manual Fallback Entry for '%s' ---%n", cityName);
                    promptAndSaveManualReport(cityName);
                }
            } else {
                System.out.println("[INFO] Manual fallback skipped. Returning to menu.");
            }
        } catch (InvalidWeatherDataException e) {
            System.out.printf("%n[!] Validation Error in API Data: %s%n", e.getMessage());
            System.out.print("\nWould you like to manually enter weather data for this city instead? (yes/no): ");
            String response = scanner.nextLine().trim();
            if (response.equalsIgnoreCase("yes") || response.equalsIgnoreCase("y")) {
                if (weatherService.cityExists(cityName)) {
                    System.out.printf("%n[!] A record for '%s' already exists. Use Option 5 to update.%n", cityName);
                } else {
                    System.out.printf("%n--- Manual Fallback Entry for '%s' ---%n", cityName);
                    promptAndSaveManualReport(cityName);
                }
            } else {
                System.out.println("[INFO] Manual fallback skipped. Returning to menu.");
            }
        }
    }

    /**
     * Handles Option 3: Displaying all stored weather reports.
     */
    private static void handleDisplayAllReports() {
        weatherService.displayAllReports();
    }

    /**
     * Handles Option 4: Searching for a specific city's weather.
     */
    private static void handleSearchCity() {
        System.out.println("\n--- Search Weather by City ---");
        String cityName = readNonEmptyString("Enter city name to search: ");

        WeatherReport report = weatherService.searchCity(cityName);
        if (report != null) {
            System.out.println("\n==================================================");
            System.out.println("               SEARCH RESULT FOUND                ");
            System.out.println("==================================================");
            report.displayReport();
        } else {
            System.out.printf("%n[!] No weather report found for '%s'.%n", cityName);
            System.out.println("    Please ensure the spelling is correct or add the city via Option 1 or 2.");
        }
    }

    /**
     * Handles Option 5: Updating existing weather information for a city.
     */
    private static void handleUpdateWeather() {
        System.out.println("\n--- Update Weather Information ---");
        String cityName = readNonEmptyString("Enter city name to update: ");

        WeatherReport existingReport = weatherService.searchCity(cityName);
        if (existingReport == null) {
            System.out.printf("%n[!] City '%s' is not registered in the system.%n", cityName);
            System.out.println("    Use Option 1 to add a new city report.");
            return;
        }

        System.out.printf("%nFound existing record for '%s':%n", existingReport.getCityName());
        existingReport.displayReport();

        System.out.println("\nEnter updated weather metrics:");
        double newTemp = readDoubleInput("Enter New Temperature in °C (-90.0 to 65.0): ", -90.0, 65.0);
        double newHum = readDoubleInput("Enter New Relative Humidity % (0.0 to 100.0): ", 0.0, 100.0);
        double newWind = readDoubleInput("Enter New Wind Speed in km/h (>= 0.0): ", 0.0, 500.0);
        String newCond = readWeatherCondition();

        try {
            // Prepare an updated report and proposed state list without mutating the existing report first
            String updatedTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss"));
            WeatherReport proposedReport = new WeatherReport(
                    existingReport.getCityName(),
                    newTemp,
                    newHum,
                    newWind,
                    newCond,
                    updatedTimestamp
            );

            List<WeatherReport> proposedReports = new ArrayList<>();
            for (WeatherReport report : weatherService.getAllReports()) {
                if (report.getCityName().equalsIgnoreCase(existingReport.getCityName())) {
                    proposedReports.add(proposedReport);
                } else {
                    proposedReports.add(report);
                }
            }

            // Persist the proposed state list first
            boolean saved = weatherStorage.saveReports(proposedReports);
            if (saved) {
                // Only apply the in-memory update after successful persistence
                weatherService.updateWeatherReport(existingReport.getCityName(), newTemp, newHum, newWind, newCond);
                existingReport.setLastUpdated(updatedTimestamp);
                System.out.printf("%n[SUCCESS] Weather information for '%s' updated successfully and saved to disk!%n", existingReport.getCityName());
                System.out.println("Updated Report:");
                existingReport.displayReport();
            } else {
                // Leave original in-memory state unchanged!
                System.out.printf("%n[ERROR] Failed to save updated weather report for '%s' to persistent storage.%n", existingReport.getCityName());
                System.out.println("        Update aborted. In-memory records remain unchanged.");
            }
        } catch (InvalidWeatherDataException e) {
            System.out.printf("%n[!] Update failed due to invalid data: %s%n", e.getMessage());
        }
    }

    /**
     * Handles Option 6: Displaying Temperature, Humidity, and Wind Speed only.
     */
    private static void handleDisplayKeyMetrics() {
        weatherService.displayKeyMetrics();
    }

    /**
     * Handles Option 7: Deleting saved weather reports from memory and persistent storage.
     */
    private static void handleDeleteSavedWeather() {
        System.out.println("\n--- DELETE SAVED WEATHER REPORTS ---");
        if (weatherStorage.isCorruptionDetected()) {
            System.err.printf("[!] NOTICE: Storage file '%s' has integrity issues or corruption.%n", weatherStorage.getFilePath());
            System.err.println("    Deletion operations will be refused to prevent data divergence.");
        }
        System.out.println("  A. Delete a specific city's saved weather report");
        System.out.println("  B. Delete all saved weather reports");
        System.out.println("  C. Cancel");
        System.out.println("------------------------------------");

        System.out.print("Select an operation (A/B/C or 1/2/3): ");
        String operation = scanner.nextLine().trim();

        if (operation.equalsIgnoreCase("A") || operation.equals("1")) {
            handleDeleteSpecificCity();
        } else if (operation.equalsIgnoreCase("B") || operation.equals("2")) {
            handleDeleteAllCities();
        } else if (operation.equalsIgnoreCase("C") || operation.equals("3")) {
            System.out.println("\n[INFO] Operation cancelled. Returning to main menu.");
        } else {
            System.out.println("\n[!] Invalid selection. Returning to main menu.");
        }
    }

    /**
     * Deletes a specific city's weather record after user confirmation.
     * Transactional pattern: Saves proposed list copy first, only mutates WeatherService after save succeeds.
     */
    private static void handleDeleteSpecificCity() {
        if (weatherStorage.isCorruptionDetected()) {
            System.err.printf("%n[!] Deletion Refused: Storage file '%s' has been flagged as corrupted or contains invalid/duplicate records.%n",
                    weatherStorage.getFilePath());
            System.err.println("    Deletion is disabled to prevent in-memory changes from diverging from the un-saveable file.");
            System.err.println("    Please inspect and repair the storage file manually before attempting deletion.");
            return;
        }

        System.out.println("\n--- Delete City Weather Report ---");
        String cityName = readNonEmptyString("Enter the city name to delete: ");

        WeatherReport existingReport = weatherService.searchCity(cityName);
        if (existingReport == null) {
            System.out.printf("%n[!] No weather report found for '%s'. Nothing to delete.%n", cityName);
            return;
        }

        System.out.printf("%nFound report for '%s':%n", existingReport.getCityName());
        existingReport.displayReport();

        System.out.printf("%nAre you sure you want to delete the weather report for '%s'? (yes/no): ", existingReport.getCityName());
        String confirm = scanner.nextLine().trim();

        if (!confirm.equalsIgnoreCase("yes") && !confirm.equalsIgnoreCase("y")) {
            System.out.println("\n[INFO] Deletion cancelled. No changes were made.");
            return;
        }

        // Prepare temporary copy of current reports, removing the selected report from the copy
        List<WeatherReport> proposedReports = new ArrayList<>();
        for (WeatherReport report : weatherService.getAllReports()) {
            if (!report.getCityName().equalsIgnoreCase(existingReport.getCityName())) {
                proposedReports.add(report);
            }
        }

        // Persist the proposed state first
        boolean saved = weatherStorage.saveReports(proposedReports);
        if (saved) {
            // Only apply in-memory deletion after save succeeds
            weatherService.deleteWeatherReport(existingReport.getCityName());
            System.out.printf("%n[SUCCESS] Weather report for '%s' was successfully deleted from memory and disk storage.%n",
                    existingReport.getCityName());
        } else {
            // Leave original in-memory state unchanged!
            System.out.printf("%n[ERROR] Failed to save deletion of '%s' to persistent storage.%n", existingReport.getCityName());
            System.out.println("        Deletion aborted. In-memory records remain unchanged.");
        }
    }

    /**
     * Deletes all weather records after explicit user confirmation.
     * Transactional pattern: Persists empty list to storage first, only clears WeatherService after save succeeds.
     */
    private static void handleDeleteAllCities() {
        if (weatherStorage.isCorruptionDetected()) {
            System.err.printf("%n[!] Deletion Refused: Storage file '%s' has been flagged as corrupted or contains invalid/duplicate records.%n",
                    weatherStorage.getFilePath());
            System.err.println("    Deletion is disabled to prevent in-memory changes from diverging from the un-saveable file.");
            System.err.println("    Please inspect and repair the storage file manually before attempting deletion.");
            return;
        }

        System.out.println("\n--- Delete ALL Weather Reports ---");
        int currentCount = weatherService.getTotalRecordCount();
        if (currentCount == 0) {
            System.out.println("\n[INFO] No weather reports available in the system. Nothing to delete.");
            return;
        }

        System.out.printf("%n[WARNING] This operation will delete all %d weather report(s) in the system!%n", currentCount);
        System.out.print("Are you sure you want to delete ALL weather reports? (yes/no): ");
        String confirm = scanner.nextLine().trim();

        if (!confirm.equalsIgnoreCase("yes") && !confirm.equalsIgnoreCase("y")) {
            System.out.println("\n[INFO] Deletion cancelled. No changes were made.");
            return;
        }

        // Prepare empty list and attempt to save to persistent storage first
        List<WeatherReport> emptyList = new ArrayList<>();
        boolean saved = weatherStorage.saveReports(emptyList);
        if (saved) {
            // Only clear WeatherService after the save succeeds
            weatherService.clearAllReports();
            System.out.println("\n[SUCCESS] All weather reports have been successfully deleted from memory and disk storage.");
        } else {
            // Leave original in-memory state unchanged!
            System.out.println("\n[ERROR] Failed to persist deletion to storage (storage is write-protected or unavailable).");
            System.out.println("        Operation aborted. In-memory records remain unchanged.");
        }
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
