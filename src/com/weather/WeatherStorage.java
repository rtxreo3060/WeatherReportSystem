package com.weather;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Handles persistent storage of weather reports to and from JSON files using Google Gson.
 *
 * Architecture & Features:
 *  - Stores data at a relative project path (default: data/weather-reports.json).
 *  - Formats output with Gson pretty-printing for human readability.
 *  - Performs atomic file writes using a temporary file and atomic move to prevent data corruption.
 *  - Automatically creates the parent data directory upon the first save.
 *  - Preserves all WeatherReport fields including original timestamps without recalculation.
 *  - Validates loaded records against domain constraints to ensure data integrity.
 *  - Duplicate Detection: Detects duplicate city names case-insensitively and ignoring whitespace.
 *    Valid unique records are preserved in memory, while corruption protection is activated.
 *  - Anti-corruption protection: Activates corruptionDetected = true if ANY record in a JSON array
 *    is invalid or null, if duplicate city records exist, if the existing file is empty/whitespace,
 *    if the JSON root is null, or if the file contains malformed JSON.
 *  - Overwrite protection: Never silently overwrites or empties a corrupted/duplicate-containing JSON file;
 *    alerts the user and disables saves for that session to preserve original file contents.
 *
 * Demonstrates:
 *  - Encapsulation (hides file I/O, NIO paths, and serialization details)
 *  - Robust Exception Handling (graceful recovery from IO and parsing errors)
 *  - Single Responsibility Principle (dedicated persistence class separate from UI and services)
 */
public class WeatherStorage {

    private static final String DEFAULT_FILE_PATH = "data/weather-reports.json";

    private final Path filePath;
    private final Gson gson;
    private boolean corruptionDetected;
    private boolean duplicateDetected;

    /**
     * Default constructor pointing to the standard relative data path: data/weather-reports.json.
     */
    public WeatherStorage() {
        this(Paths.get(DEFAULT_FILE_PATH));
    }

    /**
     * Parameterized constructor allowing a custom relative or absolute path.
     * Useful for automated testing and dependency injection.
     *
     * @param filePath Path where weather reports JSON should be stored.
     */
    public WeatherStorage(Path filePath) {
        if (filePath == null) {
            throw new IllegalArgumentException("File path cannot be null.");
        }
        this.filePath = filePath;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.corruptionDetected = false;
        this.duplicateDetected = false;
    }

    /**
     * Convenience constructor accepting a string path.
     *
     * @param pathString String path to JSON storage.
     * @throws IllegalArgumentException if pathString is null or contains only whitespace.
     */
    public WeatherStorage(String pathString) {
        this(validatePathString(pathString));
    }

    private static Path validatePathString(String pathString) {
        if (pathString == null || pathString.trim().isEmpty()) {
            throw new IllegalArgumentException("Path string cannot be null or empty.");
        }
        return Paths.get(pathString.trim());
    }

    /**
     * Loads and validates existing weather reports from the JSON file.
     * Checks for invalid data, malformed syntax, and duplicate city names (case-insensitive, trimmed).
     *
     * @return List of validated unique WeatherReport objects. Returns an empty list if the file
     *         does not exist or is empty.
     */
    public List<WeatherReport> loadReports() {
        List<WeatherReport> reports = new ArrayList<>();

        if (!Files.exists(filePath)) {
            // First run: data file does not exist yet. This is expected and valid.
            return reports;
        }

        String jsonContent;
        try {
            jsonContent = Files.readString(filePath, StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            System.err.printf("%n[!] Error: Unable to read storage file '%s': %s%n", filePath, e.getMessage());
            this.corruptionDetected = true;
            return reports;
        }

        // Existing file that is empty or whitespace-only is invalid and must be protected
        if (jsonContent.isEmpty()) {
            System.err.printf("%n[!] ERROR: Storage file '%s' exists but is empty or contains only whitespace.%n", filePath);
            System.err.println("    Safety Action: The file is preserved and will NOT be overwritten.");
            this.corruptionDetected = true;
            return reports;
        }

        Type listType = new TypeToken<List<WeatherReport>>() {}.getType();
        List<WeatherReport> rawReports;

        try {
            rawReports = gson.fromJson(jsonContent, listType);
        } catch (JsonSyntaxException e) {
            System.err.printf("%n[!] ERROR: Failed to parse storage file '%s'.%n", filePath);
            System.err.println("    Reason: The file contains malformed or corrupted JSON.");
            System.err.println("    Safety Action: The corrupted file is preserved and will NOT be overwritten.");
            this.corruptionDetected = true;
            return reports;
        }

        // JSON null or non-array root
        if (rawReports == null) {
            System.err.printf("%n[!] ERROR: Storage file '%s' contains an invalid JSON root (null or not an array).%n", filePath);
            System.err.println("    Safety Action: The file is preserved and will NOT be overwritten.");
            this.corruptionDetected = true;
            return reports;
        }

        // Validate each loaded record, detect duplicates (case-insensitive & trimmed), and preserve timestamps
        Set<String> seenCities = new HashSet<>();
        int invalidCount = 0;
        int duplicateCount = 0;

        for (WeatherReport raw : rawReports) {
            if (raw == null) {
                invalidCount++;
                System.err.printf("[!] Warning: Skipped null record in storage file '%s'.%n", filePath);
                continue;
            }

            try {
                // Re-validate using overloaded constructor that preserves original timestamp
                WeatherReport validated = new WeatherReport(
                        raw.getCityName(),
                        raw.getTemperature(),
                        raw.getHumidity(),
                        raw.getWindSpeed(),
                        raw.getWeatherCondition(),
                        raw.getLastUpdated() // Preserves existing timestamp exactly
                );

                // Case-insensitive and whitespace-trimmed duplicate detection
                String normalizedCity = validated.getCityName().trim().toLowerCase(Locale.ROOT);
                if (seenCities.contains(normalizedCity)) {
                    duplicateCount++;
                    System.err.printf("[!] Warning: Detected duplicate city record in storage file (%s).%n", validated.getCityName());
                } else {
                    seenCities.add(normalizedCity);
                    reports.add(validated);
                }
            } catch (InvalidWeatherDataException e) {
                invalidCount++;
                System.err.printf("[!] Warning: Skipped invalid record in storage file (%s): %s%n",
                        raw.getCityName() != null ? raw.getCityName() : "unknown", e.getMessage());
            }
        }

        // Protect partially or completely invalid arrays: ANY invalid record triggers corruption protection
        if (invalidCount > 0) {
            this.corruptionDetected = true;
            System.err.printf("%n[!] Warning: Detected %d invalid or corrupted record(s) in '%s'.%n", invalidCount, filePath);
            System.err.println("    Safety Action: File is marked as corrupted to protect remaining data from being overwritten.");
        }

        // Protect against duplicate records: ANY duplicate city triggers corruption protection
        if (duplicateCount > 0) {
            this.corruptionDetected = true;
            this.duplicateDetected = true;
            System.err.printf("%n[!] Warning: Detected %d duplicate city record(s) in '%s'.%n", duplicateCount, filePath);
            System.err.println("    Safety Action: File is marked as corrupted to protect conflicting data from being overwritten.");
        }

        return reports;
    }

    /**
     * Persists the provided weather reports to the JSON file.
     * Uses an atomic write pattern (writes to a temporary file, then replaces the target)
     * to prevent partial or corrupted file writes.
     *
     * @param reports The list of weather reports to save.
     * @return true if successfully saved, false otherwise.
     */
    public boolean saveReports(List<WeatherReport> reports) {
        if (corruptionDetected) {
            System.err.printf("%n[!] Save Aborted: The storage file '%s' was corrupted or invalid on startup.%n", filePath);
            System.err.println("    Saving is disabled to protect the original file from being overwritten.");
            System.err.println("    Please inspect or restore the file manually.");
            return false;
        }

        if (reports == null) {
            reports = new ArrayList<>();
        }

        // Validate that the report list to save contains only valid and unique records
        Set<String> seenCities = new HashSet<>();
        for (WeatherReport report : reports) {
            if (report == null) {
                System.err.printf("%n[!] Save Aborted: Report list for '%s' contains a null record.%n", filePath);
                return false;
            }
            if (report.getCityName() == null || report.getCityName().trim().isEmpty()) {
                System.err.printf("%n[!] Save Aborted: Report list for '%s' contains a record with an empty city name.%n", filePath);
                return false;
            }
            if (report.getWeatherCondition() == null || report.getWeatherCondition().trim().isEmpty()) {
                System.err.printf("%n[!] Save Aborted: Report list for '%s' contains a record with an empty weather condition.%n", filePath);
                return false;
            }

            // Reject NaN and positive/negative infinity before range validation
            if (!Double.isFinite(report.getTemperature())) {
                System.err.printf("%n[!] Save Aborted: Non-finite temperature (%s) for city '%s'.%n",
                        report.getTemperature(), report.getCityName());
                return false;
            }
            if (!Double.isFinite(report.getHumidity())) {
                System.err.printf("%n[!] Save Aborted: Non-finite humidity (%s) for city '%s'.%n",
                        report.getHumidity(), report.getCityName());
                return false;
            }
            if (!Double.isFinite(report.getWindSpeed())) {
                System.err.printf("%n[!] Save Aborted: Non-finite wind speed (%s) for city '%s'.%n",
                        report.getWindSpeed(), report.getCityName());
                return false;
            }

            // Physical domain validation limits
            if (report.getTemperature() < -90.0 || report.getTemperature() > 65.0) {
                System.err.printf("%n[!] Save Aborted: Out-of-range temperature (%.1f°C) for city '%s'.%n",
                        report.getTemperature(), report.getCityName());
                return false;
            }
            if (report.getHumidity() < 0.0 || report.getHumidity() > 100.0) {
                System.err.printf("%n[!] Save Aborted: Out-of-range humidity (%.1f%%) for city '%s'.%n",
                        report.getHumidity(), report.getCityName());
                return false;
            }
            if (report.getWindSpeed() < 0.0) {
                System.err.printf("%n[!] Save Aborted: Negative wind speed (%.1f km/h) for city '%s'.%n",
                        report.getWindSpeed(), report.getCityName());
                return false;
            }
            // Duplicate city detection (case-insensitive and trimmed)
            String normalized = report.getCityName().trim().toLowerCase(Locale.ROOT);
            if (!seenCities.add(normalized)) {
                System.err.printf("%n[!] Save Aborted: Duplicate city '%s' detected in list to be saved to '%s'.%n",
                        report.getCityName(), filePath);
                return false;
            }
        }

        Path parentDir = filePath.getParent();
        Path tempFile = null;

        try {
            // Ensure parent directory (e.g., 'data/') exists
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            // Create temporary file in the same directory for atomic rename/move
            Path tempDir = (parentDir != null) ? parentDir : Paths.get(".");
            tempFile = Files.createTempFile(tempDir, "weather-reports-", ".tmp");

            // Write JSON to temporary file
            try (BufferedWriter writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
                gson.toJson(reports, writer);
            }

            // Atomically replace existing target file
            try {
                Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                // Fallback to standard replace if filesystem does not support atomic move
                Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            return true;

        } catch (IOException e) {
            System.err.printf("%n[!] Error saving weather reports to '%s': %s%n", filePath, e.getMessage());
            return false;
        } finally {
            // Clean up temporary file if it still exists (e.g., if move failed)
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * Returns whether corruption or data-integrity issue was detected during file loading.
     */
    public boolean isCorruptionDetected() {
        return corruptionDetected;
    }

    /**
     * Returns whether duplicate city records were detected during file loading.
     */
    public boolean isDuplicateDetected() {
        return duplicateDetected;
    }

    /**
     * Returns the target file path.
     */
    public Path getFilePath() {
        return filePath;
    }
}
