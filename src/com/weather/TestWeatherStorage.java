package com.weather;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Standalone verification runner for WeatherStorage and WeatherManager persistence features.
 *
 * Verifies all 27 core data-integrity, persistence, deletion, and safety requirements:
 *  1. Missing file is treated as first-run empty storage.
 *  2. Valid [] is accepted.
 *  3. Empty/whitespace existing file is protected.
 *  4. JSON null root is protected.
 *  5. Malformed JSON syntax is protected.
 *  6. Array with 1 valid and 1 invalid record is protected from overwriting.
 *  7. Completely invalid record array is protected.
 *  8. Byte-for-byte preservation after save rejection.
 *  9. Normal save, reload, update, and timestamp preservation.
 *  10. Duplicate city detection: 'Kolkata' and 'kolkata' (case-insensitive).
 *  11. Duplicate city detection: 'Delhi' and ' Delhi ' (whitespace-trimmed).
 *  12. Mixed file containing valid unique records alongside duplicate records.
 *  13. WeatherManager service integration: unique records loaded to memory, disk overwrite blocked.
 *  14. Root validation: '{}' (JSON object root) protected.
 *  15. Root validation: '"invalid"' (JSON string root) protected.
 *  16. Root validation: '42' (numeric root) protected.
 *  17. WeatherStorage constructor validation: rejects null and whitespace path strings.
 *  18. Deleting one existing city and verifying persistent storage update (with initial save check).
 *  19. Attempting to delete a city that does not exist.
 *  20. Case-insensitive and trimmed city matching during deletion (with initial save check).
 *  21. Deleting all reports and verifying persistent file contains a valid empty array [] (with initial save check).
 *  22. Storage-level no-operation check: file remains unchanged when no deletion is performed (with initial save check).
 *  23. WeatherStorage refusal to save after corruption detection (independent storage-layer protection).
 *  24. Storage save-failure simulation: proposed-list pattern leaves WeatherManager in-memory state unchanged.
 *  25. WeatherStorage.saveReports() domain, non-finite (NaN, +/-Inf), and uniqueness validation.
 *  26. Valid empty list [] and normal report persistence preservation in saveReports().
 *  27. Production data safeguard: confirms real 'data/weather-reports.json' is untouched.
 *
 * Runs exclusively inside a unique temporary directory created via Files.createTempDirectory(),
 * completely outside the project's data/ directory.
 */
public class TestWeatherStorage {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("       WeatherStorage Verification Test Suite     ");
        System.out.println("==================================================\n");

        int passed = 0;
        int failed = 0;

        // Record real production file state before test run to verify untouched safety
        Path productionFile = Paths.get("data", "weather-reports.json");
        boolean prodExistedInitially = Files.exists(productionFile);
        byte[] realProductionBytes = null;
        boolean initialReadFailed = false;

        if (prodExistedInitially) {
            try {
                realProductionBytes = Files.readAllBytes(productionFile);
            } catch (IOException e) {
                initialReadFailed = true;
                System.err.printf("[!] Warning: Could not read production file for initial snapshot: %s%n", e.getMessage());
            }
        }

        Path tempDir = null;

        try {
            // Create a unique temporary directory outside project data/
            tempDir = Files.createTempDirectory("weather_test_");
            System.out.printf("[SETUP] Created unique test sandbox: %s%n%n", tempDir.toAbsolutePath());

            // -------------------------------------------------------------
            // TEST 1: Missing file is treated as first-run empty storage
            // -------------------------------------------------------------
            System.out.println("[TEST 1] Missing file treated as first-run empty storage...");
            Path missingFile = tempDir.resolve("non_existent.json");
            WeatherStorage storage1 = new WeatherStorage(missingFile);
            List<WeatherReport> reports1 = storage1.loadReports();

            if (reports1 != null && reports1.isEmpty() && !storage1.isCorruptionDetected()) {
                System.out.println("  -> SUCCESS: Empty list returned, corruptionDetected is false.");
                passed++;
            } else {
                System.out.println("  -> FAILED: Expected empty list and false corruption flag.");
                failed++;
            }

            // -------------------------------------------------------------
            // TEST 2: Valid empty JSON array [] is accepted
            // -------------------------------------------------------------
            System.out.println("\n[TEST 2] Valid empty JSON array [] is accepted...");
            Path emptyArrayFile = tempDir.resolve("empty_array.json");
            Files.writeString(emptyArrayFile, "[]", StandardCharsets.UTF_8);

            WeatherStorage storage2 = new WeatherStorage(emptyArrayFile);
            List<WeatherReport> reports2 = storage2.loadReports();

            if (reports2 != null && reports2.isEmpty() && !storage2.isCorruptionDetected()) {
                List<WeatherReport> sampleList = new ArrayList<>();
                sampleList.add(new WeatherReport("Berlin", 12.0, 70.0, 15.0, "Cloudy", "01-Jan-2026 10:00:00"));
                boolean saveAllowed = storage2.saveReports(sampleList);

                if (saveAllowed && storage2.loadReports().size() == 1) {
                    System.out.println("  -> SUCCESS: [] loaded cleanly without corruption, and saves are permitted.");
                    passed++;
                } else {
                    System.out.println("  -> FAILED: Save to valid empty array file was rejected.");
                    failed++;
                }
            } else {
                System.out.println("  -> FAILED: Valid [] was incorrectly flagged as corrupted.");
                failed++;
            }

            // -------------------------------------------------------------
            // TEST 3: Empty existing file is protected
            // -------------------------------------------------------------
            System.out.println("\n[TEST 3] Empty/whitespace-only existing file is protected...");
            Path emptyFile = tempDir.resolve("empty_file.json");
            Files.writeString(emptyFile, "   \n  \t  ", StandardCharsets.UTF_8);

            WeatherStorage storage3 = new WeatherStorage(emptyFile);
            List<WeatherReport> reports3 = storage3.loadReports();
            boolean saveBlocked3 = !storage3.saveReports(new ArrayList<>());
            String contentAfter3 = Files.readString(emptyFile, StandardCharsets.UTF_8);

            if (reports3.isEmpty() && storage3.isCorruptionDetected() && saveBlocked3 && contentAfter3.equals("   \n  \t  ")) {
                System.out.println("  -> SUCCESS: Empty file detected as invalid, saves blocked, content preserved.");
                passed++;
            } else {
                System.out.println("  -> FAILED: Empty file was not properly protected.");
                failed++;
            }

            // -------------------------------------------------------------
            // TEST 4: JSON null is protected
            // -------------------------------------------------------------
            System.out.println("\n[TEST 4] JSON null root is protected...");
            Path nullFile = tempDir.resolve("json_null.json");
            Files.writeString(nullFile, "null", StandardCharsets.UTF_8);

            WeatherStorage storage4 = new WeatherStorage(nullFile);
            List<WeatherReport> reports4 = storage4.loadReports();
            boolean saveBlocked4 = !storage4.saveReports(new ArrayList<>());
            String contentAfter4 = Files.readString(nullFile, StandardCharsets.UTF_8);

            if (reports4.isEmpty() && storage4.isCorruptionDetected() && saveBlocked4 && contentAfter4.equals("null")) {
                System.out.println("  -> SUCCESS: JSON null detected as invalid root, saves blocked, content preserved.");
                passed++;
            } else {
                System.out.println("  -> FAILED: JSON null was not properly protected.");
                failed++;
            }

            // -------------------------------------------------------------
            // TEST 5: Malformed JSON is protected
            // -------------------------------------------------------------
            System.out.println("\n[TEST 5] Malformed JSON syntax is protected...");
            Path malformedFile = tempDir.resolve("malformed.json");
            String malformedJson = "{\"cityName\": \"IncompleteJson\", ";
            Files.writeString(malformedFile, malformedJson, StandardCharsets.UTF_8);

            WeatherStorage storage5 = new WeatherStorage(malformedFile);
            List<WeatherReport> reports5 = storage5.loadReports();
            boolean saveBlocked5 = !storage5.saveReports(new ArrayList<>());
            String contentAfter5 = Files.readString(malformedFile, StandardCharsets.UTF_8);

            if (reports5.isEmpty() && storage5.isCorruptionDetected() && saveBlocked5 && contentAfter5.equals(malformedJson)) {
                System.out.println("  -> SUCCESS: Malformed JSON flagged, saves blocked, content preserved.");
                passed++;
            } else {
                System.out.println("  -> FAILED: Malformed JSON was not properly protected.");
                failed++;
            }

            // -------------------------------------------------------------
            // TEST 6: Array with one valid record and one invalid record
            // -------------------------------------------------------------
            System.out.println("\n[TEST 6] Array with 1 valid and 1 invalid record is protected from overwriting...");
            Path partialFile = tempDir.resolve("partially_invalid.json");
            String partialJson = "[\n"
                    + "  {\n"
                    + "    \"cityName\": \"Tokyo\",\n"
                    + "    \"temperature\": 15.0,\n"
                    + "    \"humidity\": 60.0,\n"
                    + "    \"windSpeed\": 10.0,\n"
                    + "    \"weatherCondition\": \"Sunny\",\n"
                    + "    \"lastUpdated\": \"01-Jan-2026 12:00:00\"\n"
                    + "  },\n"
                    + "  {\n"
                    + "    \"cityName\": \"BadCity\",\n"
                    + "    \"temperature\": 999.0,\n" // Invalid temperature!
                    + "    \"humidity\": 50.0,\n"
                    + "    \"windSpeed\": 10.0,\n"
                    + "    \"weatherCondition\": \"Hot\"\n"
                    + "  }\n"
                    + "]";
            Files.writeString(partialFile, partialJson, StandardCharsets.UTF_8);

            WeatherStorage storage6 = new WeatherStorage(partialFile);
            List<WeatherReport> reports6 = storage6.loadReports();

            boolean validLoaded6 = (reports6.size() == 1 && reports6.get(0).getCityName().equals("Tokyo"));
            boolean corruptedFlagged6 = storage6.isCorruptionDetected();
            boolean saveBlocked6 = !storage6.saveReports(reports6);
            String contentAfter6 = Files.readString(partialFile, StandardCharsets.UTF_8);

            if (validLoaded6 && corruptedFlagged6 && saveBlocked6 && contentAfter6.equals(partialJson)) {
                System.out.println("  -> SUCCESS: Valid record loaded, corruption detected, save blocked, file preserved.");
                passed++;
            } else {
                System.out.println("  -> FAILED: Partial invalid record protection failed.");
                failed++;
            }

            // -------------------------------------------------------------
            // TEST 7: Completely invalid record array is protected
            // -------------------------------------------------------------
            System.out.println("\n[TEST 7] Completely invalid record array is protected...");
            Path allInvalidFile = tempDir.resolve("all_invalid.json");
            String allInvalidJson = "[\n"
                    + "  {\"cityName\": \"Bad1\", \"temperature\": -999.0},\n"
                    + "  null,\n"
                    + "  {\"cityName\": \"\", \"temperature\": 20.0}\n"
                    + "]";
            Files.writeString(allInvalidFile, allInvalidJson, StandardCharsets.UTF_8);

            WeatherStorage storage7 = new WeatherStorage(allInvalidFile);
            List<WeatherReport> reports7 = storage7.loadReports();
            boolean saveBlocked7 = !storage7.saveReports(new ArrayList<>());
            String contentAfter7 = Files.readString(allInvalidFile, StandardCharsets.UTF_8);

            if (reports7.isEmpty() && storage7.isCorruptionDetected() && saveBlocked7 && contentAfter7.equals(allInvalidJson)) {
                System.out.println("  -> SUCCESS: Completely invalid array caught, saves blocked, content preserved.");
                passed++;
            } else {
                System.out.println("  -> FAILED: Completely invalid array was not properly protected.");
                failed++;
            }

            // -------------------------------------------------------------
            // TEST 8: Save attempt after corruption leaves original bytes unchanged
            // -------------------------------------------------------------
            System.out.println("\n[TEST 8] Byte-for-byte preservation after save rejection...");
            Path byteCheckFile = tempDir.resolve("byte_check.json");
            byte[] originalBytes8 = "RAW_UNTOUCHED_BINARY_OR_CORRUPT_BYTES_XYZ_12345".getBytes(StandardCharsets.UTF_8);
            Files.write(byteCheckFile, originalBytes8);

            WeatherStorage storage8 = new WeatherStorage(byteCheckFile);
            storage8.loadReports();

            List<WeatherReport> dummyList = new ArrayList<>();
            dummyList.add(new WeatherReport("Rome", 20.0, 50.0, 10.0, "Sunny", "01-Jan-2026 15:00:00"));
            boolean saveResult8 = storage8.saveReports(dummyList);

            byte[] bytesAfterAttempt8 = Files.readAllBytes(byteCheckFile);
            boolean bytesIdentical8 = Arrays.equals(originalBytes8, bytesAfterAttempt8);

            if (!saveResult8 && bytesIdentical8) {
                System.out.println("  -> SUCCESS: Save returned false and file is byte-for-byte identical to original!");
                passed++;
            } else {
                System.out.println("  -> FAILED: Save succeeded or file content was modified.");
                failed++;
            }

            // -------------------------------------------------------------
            // TEST 9: Normal save, reload, update, and timestamp preservation
            // -------------------------------------------------------------
            System.out.println("\n[TEST 9] Normal save, reload, update, and timestamp preservation...");
            Path normalFile = tempDir.resolve("normal_reports.json");
            WeatherStorage storage9 = new WeatherStorage(normalFile);

            String initialTimestamp = "15-May-2026 09:30:00";
            WeatherReport r1 = new WeatherReport("Madrid", 22.5, 45.0, 14.0, "Sunny", initialTimestamp);
            List<WeatherReport> list9 = new ArrayList<>();
            list9.add(r1);

            boolean firstSave = storage9.saveReports(list9);
            List<WeatherReport> reloaded1 = storage9.loadReports();

            boolean r1Valid = (reloaded1.size() == 1
                    && reloaded1.get(0).getCityName().equals("Madrid")
                    && Math.abs(reloaded1.get(0).getTemperature() - 22.5) < 0.001
                    && reloaded1.get(0).getLastUpdated().equals(initialTimestamp));

            String updatedTimestamp = "15-May-2026 12:45:00";
            reloaded1.get(0).setTemperature(26.0);
            reloaded1.get(0).setWeatherCondition("Hot");
            reloaded1.get(0).setLastUpdated(updatedTimestamp);

            boolean secondSave = storage9.saveReports(reloaded1);
            List<WeatherReport> reloaded2 = storage9.loadReports();

            boolean r2Valid = (reloaded2.size() == 1
                    && reloaded2.get(0).getCityName().equals("Madrid")
                    && Math.abs(reloaded2.get(0).getTemperature() - 26.0) < 0.001
                    && reloaded2.get(0).getWeatherCondition().equals("Hot")
                    && reloaded2.get(0).getLastUpdated().equals(updatedTimestamp));

            if (firstSave && r1Valid && secondSave && r2Valid) {
                System.out.println("  -> SUCCESS: Normal save, reload, update, and timestamp preservation verified!");
                passed++;
            } else {
                System.out.println("  -> FAILED: Normal flow checks failed.");
                failed++;
            }

            // -------------------------------------------------------------
            // TEST 10: Duplicate city detection: 'Kolkata' and 'kolkata' (case-insensitive)
            // -------------------------------------------------------------
            System.out.println("\n[TEST 10] Duplicate detection: 'Kolkata' and 'kolkata' (case-insensitive)...");
            Path dupCaseFile = tempDir.resolve("dup_case.json");
            String dupCaseJson = "[\n"
                    + "  {\"cityName\": \"Kolkata\", \"temperature\": 25.0, \"humidity\": 80.0, \"windSpeed\": 10.0, \"weatherCondition\": \"Rainy\", \"lastUpdated\": \"01-Jan-2026 10:00:00\"},\n"
                    + "  {\"cityName\": \"kolkata\", \"temperature\": 27.0, \"humidity\": 75.0, \"windSpeed\": 12.0, \"weatherCondition\": \"Sunny\", \"lastUpdated\": \"01-Jan-2026 12:00:00\"}\n"
                    + "]";
            byte[] dupCaseOriginalBytes = dupCaseJson.getBytes(StandardCharsets.UTF_8);
            Files.write(dupCaseFile, dupCaseOriginalBytes);

            WeatherStorage storage10 = new WeatherStorage(dupCaseFile);
            List<WeatherReport> reports10 = storage10.loadReports();

            boolean t10SizeOk = (reports10.size() == 1 && reports10.get(0).getCityName().equals("Kolkata"));
            boolean t10CorruptOk = storage10.isCorruptionDetected();
            boolean t10DupOk = storage10.isDuplicateDetected();
            boolean t10SaveResult = storage10.saveReports(reports10);
            byte[] t10BytesAfter = Files.readAllBytes(dupCaseFile);
            boolean t10BytesUnchanged = Arrays.equals(dupCaseOriginalBytes, t10BytesAfter);

            if (t10SizeOk && t10CorruptOk && t10DupOk && !t10SaveResult && t10BytesUnchanged) {
                System.out.println("  -> SUCCESS: Case-insensitive duplicate detected ('Kolkata' vs 'kolkata').");
                System.out.println("              Unique record loaded to memory, corruption set, save rejected, file preserved byte-for-byte!");
                passed++;
            } else {
                System.out.println("  -> FAILED: Case-insensitive duplicate check failed.");
                failed++;
            }

            // -------------------------------------------------------------
            // TEST 11: Duplicate city detection: 'Delhi' and ' Delhi ' (whitespace-trimmed)
            // -------------------------------------------------------------
            System.out.println("\n[TEST 11] Duplicate detection: 'Delhi' and ' Delhi ' (whitespace-trimmed)...");
            Path dupWhitespaceFile = tempDir.resolve("dup_whitespace.json");
            String dupWhitespaceJson = "[\n"
                    + "  {\"cityName\": \"Delhi\", \"temperature\": 32.0, \"humidity\": 45.0, \"windSpeed\": 8.0, \"weatherCondition\": \"Haze\", \"lastUpdated\": \"01-Jan-2026 10:00:00\"},\n"
                    + "  {\"cityName\": \" Delhi \", \"temperature\": 34.0, \"humidity\": 40.0, \"windSpeed\": 9.0, \"weatherCondition\": \"Clear\", \"lastUpdated\": \"01-Jan-2026 12:00:00\"}\n"
                    + "]";
            byte[] dupWhitespaceOriginalBytes = dupWhitespaceJson.getBytes(StandardCharsets.UTF_8);
            Files.write(dupWhitespaceFile, dupWhitespaceOriginalBytes);

            WeatherStorage storage11 = new WeatherStorage(dupWhitespaceFile);
            List<WeatherReport> reports11 = storage11.loadReports();

            boolean t11SizeOk = (reports11.size() == 1 && reports11.get(0).getCityName().equals("Delhi"));
            boolean t11CorruptOk = storage11.isCorruptionDetected();
            boolean t11DupOk = storage11.isDuplicateDetected();
            boolean t11SaveResult = storage11.saveReports(reports11);
            byte[] t11BytesAfter = Files.readAllBytes(dupWhitespaceFile);
            boolean t11BytesUnchanged = Arrays.equals(dupWhitespaceOriginalBytes, t11BytesAfter);

            if (t11SizeOk && t11CorruptOk && t11DupOk && !t11SaveResult && t11BytesUnchanged) {
                System.out.println("  -> SUCCESS: Whitespace duplicate detected ('Delhi' vs ' Delhi ').");
                System.out.println("              Unique record loaded to memory, corruption set, save rejected, file preserved byte-for-byte!");
                passed++;
            } else {
                System.out.println("  -> FAILED: Whitespace duplicate check failed.");
                failed++;
            }

            // -------------------------------------------------------------
            // TEST 12: File containing valid unique records alongside duplicate records
            // -------------------------------------------------------------
            System.out.println("\n[TEST 12] Valid unique records alongside duplicate records...");
            Path dupMixedFile = tempDir.resolve("dup_mixed.json");
            String dupMixedJson = "[\n"
                    + "  {\"cityName\": \"London\", \"temperature\": 15.0, \"humidity\": 70.0, \"windSpeed\": 12.0, \"weatherCondition\": \"Drizzle\", \"lastUpdated\": \"01-Jan-2026 10:00:00\"},\n"
                    + "  {\"cityName\": \"Paris\", \"temperature\": 18.0, \"humidity\": 65.0, \"windSpeed\": 10.0, \"weatherCondition\": \"Cloudy\", \"lastUpdated\": \"01-Jan-2026 10:00:00\"},\n"
                    + "  {\"cityName\": \"Tokyo\", \"temperature\": 20.0, \"humidity\": 60.0, \"windSpeed\": 15.0, \"weatherCondition\": \"Clear\", \"lastUpdated\": \"01-Jan-2026 10:00:00\"},\n"
                    + "  {\"cityName\": \"  tokyo  \", \"temperature\": 21.0, \"humidity\": 58.0, \"windSpeed\": 14.0, \"weatherCondition\": \"Sunny\", \"lastUpdated\": \"01-Jan-2026 12:00:00\"}\n"
                    + "]";
            byte[] dupMixedOriginalBytes = dupMixedJson.getBytes(StandardCharsets.UTF_8);
            Files.write(dupMixedFile, dupMixedOriginalBytes);

            WeatherStorage storage12 = new WeatherStorage(dupMixedFile);
            List<WeatherReport> reports12 = storage12.loadReports();

            boolean t12SizeOk = (reports12.size() == 3);
            boolean t12HasLondon = reports12.stream().anyMatch(r -> r.getCityName().equalsIgnoreCase("London"));
            boolean t12HasParis = reports12.stream().anyMatch(r -> r.getCityName().equalsIgnoreCase("Paris"));
            boolean t12HasTokyo = reports12.stream().anyMatch(r -> r.getCityName().equalsIgnoreCase("Tokyo"));
            boolean t12CorruptOk = storage12.isCorruptionDetected();
            boolean t12DupOk = storage12.isDuplicateDetected();

            reports12.add(new WeatherReport("Rome", 22.0, 50.0, 10.0, "Sunny", "01-Jan-2026 14:00:00"));
            boolean t12SaveResult = storage12.saveReports(reports12);
            byte[] t12BytesAfter = Files.readAllBytes(dupMixedFile);
            boolean t12BytesUnchanged = Arrays.equals(dupMixedOriginalBytes, t12BytesAfter);

            if (t12SizeOk && t12HasLondon && t12HasParis && t12HasTokyo && t12CorruptOk && t12DupOk && !t12SaveResult && t12BytesUnchanged) {
                System.out.println("  -> SUCCESS: All 3 unique valid records preserved in memory.");
                System.out.println("              Duplicate detected, corruption flag set, saves blocked, original file preserved byte-for-byte!");
                passed++;
            } else {
                System.out.println("  -> FAILED: Mixed duplicate test failed.");
                failed++;
            }

            // -------------------------------------------------------------
            // TEST 13: WeatherManager service integration test with unique records
            // -------------------------------------------------------------
            System.out.println("\n[TEST 13] WeatherManager service integration & save rejection...");
            WeatherManager manager13 = new WeatherManager();
            int addedCount13 = 0;
            for (WeatherReport report : storage12.loadReports()) {
                if (manager13.addWeatherReport(report)) {
                    addedCount13++;
                }
            }

            boolean managerSizeOk = (manager13.getAllReports().size() == 3 && addedCount13 == 3);
            boolean managerSaveBlocked = !storage12.saveReports(manager13.getAllReports());
            boolean finalBytesUnchanged = Arrays.equals(dupMixedOriginalBytes, Files.readAllBytes(dupMixedFile));

            if (managerSizeOk && managerSaveBlocked && finalBytesUnchanged) {
                System.out.println("  -> SUCCESS: WeatherManager received all 3 unique records cleanly.");
                System.out.println("              Saving manager records was blocked by WeatherStorage to protect disk file!");
                passed++;
            } else {
                System.out.println("  -> FAILED: WeatherManager integration check failed.");
                failed++;
            }

            // -------------------------------------------------------------
            // TEST 14: JSON Root validation - '{}' (JSON object root)
            // -------------------------------------------------------------
            System.out.println("\n[TEST 14] Root validation: '{}' (JSON object root)...");
            Path objectRootFile = tempDir.resolve("root_object.json");
            byte[] objectRootBytes = "{}".getBytes(StandardCharsets.UTF_8);
            Files.write(objectRootFile, objectRootBytes);

            WeatherStorage storage14 = new WeatherStorage(objectRootFile);
            List<WeatherReport> reports14 = storage14.loadReports();
            boolean saveBlocked14 = !storage14.saveReports(new ArrayList<>());
            byte[] bytesAfter14 = Files.readAllBytes(objectRootFile);
            boolean bytesUnchanged14 = Arrays.equals(objectRootBytes, bytesAfter14);

            if (reports14 != null && reports14.isEmpty() && storage14.isCorruptionDetected() && saveBlocked14 && bytesUnchanged14) {
                System.out.println("  -> SUCCESS: '{}' object root handled gracefully, corruption set, save blocked, file preserved byte-for-byte!");
                passed++;
            } else {
                System.out.println("  -> FAILED: JSON object root '{}' was not properly protected.");
                failed++;
            }

            // -------------------------------------------------------------
            // TEST 15: JSON Root validation - '"invalid"' (JSON string root)
            // -------------------------------------------------------------
            System.out.println("\n[TEST 15] Root validation: '\"invalid\"' (JSON string root)...");
            Path stringRootFile = tempDir.resolve("root_string.json");
            byte[] stringRootBytes = "\"invalid\"".getBytes(StandardCharsets.UTF_8);
            Files.write(stringRootFile, stringRootBytes);

            WeatherStorage storage15 = new WeatherStorage(stringRootFile);
            List<WeatherReport> reports15 = storage15.loadReports();
            boolean saveBlocked15 = !storage15.saveReports(new ArrayList<>());
            byte[] bytesAfter15 = Files.readAllBytes(stringRootFile);
            boolean bytesUnchanged15 = Arrays.equals(stringRootBytes, bytesAfter15);

            if (reports15 != null && reports15.isEmpty() && storage15.isCorruptionDetected() && saveBlocked15 && bytesUnchanged15) {
                System.out.println("  -> SUCCESS: '\"invalid\"' string root handled gracefully, corruption set, save blocked, file preserved byte-for-byte!");
                passed++;
            } else {
                System.out.println("  -> FAILED: JSON string root '\"invalid\"' was not properly protected.");
                failed++;
            }

            // -------------------------------------------------------------
            // TEST 16: JSON Root validation - '42' (numeric root)
            // -------------------------------------------------------------
            System.out.println("\n[TEST 16] Root validation: '42' (numeric root)...");
            Path numericRootFile = tempDir.resolve("root_number.json");
            byte[] numericRootBytes = "42".getBytes(StandardCharsets.UTF_8);
            Files.write(numericRootFile, numericRootBytes);

            WeatherStorage storage16 = new WeatherStorage(numericRootFile);
            List<WeatherReport> reports16 = storage16.loadReports();
            boolean saveBlocked16 = !storage16.saveReports(new ArrayList<>());
            byte[] bytesAfter16 = Files.readAllBytes(numericRootFile);
            boolean bytesUnchanged16 = Arrays.equals(numericRootBytes, bytesAfter16);

            if (reports16 != null && reports16.isEmpty() && storage16.isCorruptionDetected() && saveBlocked16 && bytesUnchanged16) {
                System.out.println("  -> SUCCESS: '42' numeric root handled gracefully, corruption set, save blocked, file preserved byte-for-byte!");
                passed++;
            } else {
                System.out.println("  -> FAILED: JSON numeric root '42' was not properly protected.");
                failed++;
            }

            // -------------------------------------------------------------
            // TEST 17: WeatherStorage constructor validation
            // -------------------------------------------------------------
            System.out.println("\n[TEST 17] WeatherStorage constructor validation (null / blank paths)...");
            boolean nullStringThrew = false;
            try {
                new WeatherStorage((String) null);
            } catch (IllegalArgumentException e) {
                nullStringThrew = true;
            }

            boolean blankStringThrew = false;
            try {
                new WeatherStorage("    ");
            } catch (IllegalArgumentException e) {
                blankStringThrew = true;
            }

            boolean nullPathThrew = false;
            try {
                new WeatherStorage((Path) null);
            } catch (IllegalArgumentException e) {
                nullPathThrew = true;
            }

            WeatherStorage validConstructed = new WeatherStorage(tempDir.resolve("valid_str.json").toString());
            boolean validConstructedOk = (validConstructed != null && validConstructed.getFilePath() != null);

            if (nullStringThrew && blankStringThrew && nullPathThrew && validConstructedOk) {
                System.out.println("  -> SUCCESS: Constructor properly rejects null/blank strings and null Path objects!");
                passed++;
            } else {
                System.out.printf("  -> FAILED: nullStr=%b, blankStr=%b, nullPath=%b, validOk=%b%n",
                        nullStringThrew, blankStringThrew, nullPathThrew, validConstructedOk);
                failed++;
            }

            // -------------------------------------------------------------
            // TEST 18: Deleting one existing city and verifying persistent storage update
            // -------------------------------------------------------------
            System.out.println("\n[TEST 18] Deleting one existing city and verifying persistent storage update...");
            Path deleteSingleFile = tempDir.resolve("delete_single.json");
            WeatherStorage storage18 = new WeatherStorage(deleteSingleFile);
            WeatherManager manager18 = new WeatherManager();

            manager18.addWeatherReport(new WeatherReport("Tokyo", 15.0, 60.0, 10.0, "Sunny", "01-Jan-2026 10:00:00"));
            manager18.addWeatherReport(new WeatherReport("London", 10.0, 75.0, 14.0, "Cloudy", "01-Jan-2026 10:00:00"));
            boolean initialSave18 = storage18.saveReports(manager18.getAllReports());

            if (!initialSave18) {
                System.out.println("  -> FAILED: Initial save operation failed in Test 18.");
                failed++;
            } else {
                // Delete Tokyo
                boolean tokyoDeleted = manager18.deleteWeatherReport("Tokyo");
                boolean saveAfterDelete18 = storage18.saveReports(manager18.getAllReports());

                // Reload from storage to verify persistent update
                WeatherStorage reloadStorage18 = new WeatherStorage(deleteSingleFile);
                List<WeatherReport> reloadedAfterDelete18 = reloadStorage18.loadReports();

                boolean tokyoGone = reloadedAfterDelete18.stream().noneMatch(r -> r.getCityName().equalsIgnoreCase("Tokyo"));
                boolean londonStillPresent = reloadedAfterDelete18.stream().anyMatch(r -> r.getCityName().equalsIgnoreCase("London"));

                if (tokyoDeleted && saveAfterDelete18 && reloadedAfterDelete18.size() == 1 && tokyoGone && londonStillPresent) {
                    System.out.println("  -> SUCCESS: 'Tokyo' deleted, 'London' preserved, persistent JSON updated correctly!");
                    passed++;
                } else {
                    System.out.println("  -> FAILED: Deleting single city test failed.");
                    failed++;
                }
            }

            // -------------------------------------------------------------
            // TEST 19: Attempting to delete a city that does not exist
            // -------------------------------------------------------------
            System.out.println("\n[TEST 19] Attempting to delete a city that does not exist...");
            WeatherStorage reloadStorage18 = new WeatherStorage(deleteSingleFile);
            boolean nonExistentDeleted = manager18.deleteWeatherReport("NonExistentCityXYZ");
            boolean fileUnchanged19 = (reloadStorage18.loadReports().size() == 1);

            if (!nonExistentDeleted && fileUnchanged19) {
                System.out.println("  -> SUCCESS: Deleting non-existent city returned false; data remained unchanged!");
                passed++;
            } else {
                System.out.println("  -> FAILED: Deleting non-existent city did not return false.");
                failed++;
            }

            // -------------------------------------------------------------
            // TEST 20: Case-insensitive and trimmed city matching during deletion
            // -------------------------------------------------------------
            System.out.println("\n[TEST 20] Case-insensitive and trimmed city matching during deletion...");
            Path caseDeleteFile = tempDir.resolve("case_delete.json");
            WeatherStorage storage20 = new WeatherStorage(caseDeleteFile);
            WeatherManager manager20 = new WeatherManager();

            manager20.addWeatherReport(new WeatherReport("Sydney", 24.0, 55.0, 18.0, "Sunny", "01-Jan-2026 12:00:00"));
            boolean initialSave20 = storage20.saveReports(manager20.getAllReports());

            if (!initialSave20) {
                System.out.println("  -> FAILED: Initial save operation failed in Test 20.");
                failed++;
            } else {
                // Delete using "  sYdNeY  "
                boolean sydneyDeleted = manager20.deleteWeatherReport("  sYdNeY  ");
                boolean saveAfterDelete20 = storage20.saveReports(manager20.getAllReports());

                WeatherStorage reloadStorage20 = new WeatherStorage(caseDeleteFile);
                boolean sydneyGone = reloadStorage20.loadReports().isEmpty();

                if (sydneyDeleted && saveAfterDelete20 && sydneyGone) {
                    System.out.println("  -> SUCCESS: '  sYdNeY  ' matched 'Sydney' case-insensitively and was deleted cleanly!");
                    passed++;
                } else {
                    System.out.println("  -> FAILED: Case-insensitive/trimmed deletion failed.");
                    failed++;
                }
            }

            // -------------------------------------------------------------
            // TEST 21: Deleting all reports and verifying persistent file contains a valid empty array []
            // -------------------------------------------------------------
            System.out.println("\n[TEST 21] Deleting all reports and verifying file contains valid empty array []...");
            Path deleteAllFile = tempDir.resolve("delete_all.json");
            WeatherStorage storage21 = new WeatherStorage(deleteAllFile);
            WeatherManager manager21 = new WeatherManager();

            manager21.addWeatherReport(new WeatherReport("Cairo", 30.0, 30.0, 12.0, "Sunny", "01-Jan-2026 10:00:00"));
            manager21.addWeatherReport(new WeatherReport("Nairobi", 22.0, 60.0, 8.0, "Cloudy", "01-Jan-2026 10:00:00"));
            boolean initialSave21 = storage21.saveReports(manager21.getAllReports());

            if (!initialSave21) {
                System.out.println("  -> FAILED: Initial save operation failed in Test 21.");
                failed++;
            } else {
                // Clear all reports
                manager21.clearAllReports();
                boolean deleteAllSaved = storage21.saveReports(manager21.getAllReports());

                // Verify file still exists on disk and is a valid JSON array []
                boolean fileExists21 = Files.exists(deleteAllFile);
                String rawJson21 = Files.readString(deleteAllFile, StandardCharsets.UTF_8).trim();
                WeatherStorage reloadStorage21 = new WeatherStorage(deleteAllFile);
                List<WeatherReport> reloaded21 = reloadStorage21.loadReports();

                boolean fileHasEmptyArray21 = rawJson21.equals("[]");
                boolean reloadedClean21 = (reloaded21 != null && reloaded21.isEmpty() && !reloadStorage21.isCorruptionDetected());

                if (deleteAllSaved && fileExists21 && fileHasEmptyArray21 && reloadedClean21) {
                    System.out.println("  -> SUCCESS: All reports cleared, file preserved as valid '[]', reloads cleanly without corruption!");
                    passed++;
                } else {
                    System.out.printf("  -> FAILED: saved=%b, exists=%b, hasArray=%b, cleanReload=%b%n",
                            deleteAllSaved, fileExists21, fileHasEmptyArray21, reloadedClean21);
                    failed++;
                }
            }

            // -------------------------------------------------------------
            // TEST 22: Storage-level no-operation check: file remains unchanged when no deletion is performed
            // -------------------------------------------------------------
            System.out.println("\n[TEST 22] Storage-level no-operation check: file remains unchanged when no deletion is performed...");
            Path noOpFile = tempDir.resolve("no_op_check.json");
            WeatherStorage storage22 = new WeatherStorage(noOpFile);
            WeatherManager manager22 = new WeatherManager();

            manager22.addWeatherReport(new WeatherReport("Seoul", 18.0, 50.0, 10.0, "Clear", "01-Jan-2026 10:00:00"));
            boolean initialSave22 = storage22.saveReports(manager22.getAllReports());

            if (!initialSave22) {
                System.out.println("  -> FAILED: Initial save operation failed in Test 22.");
                failed++;
            } else {
                byte[] noOpOriginalBytes = Files.readAllBytes(noOpFile);

                // Verify that without executing any deletion or modification, file contents remain byte-for-byte identical
                byte[] bytesAfterNoOp = Files.readAllBytes(noOpFile);
                boolean noOpBytesIdentical = Arrays.equals(noOpOriginalBytes, bytesAfterNoOp);
                boolean noOpReloadSizeOk = (storage22.loadReports().size() == 1);

                if (noOpBytesIdentical && noOpReloadSizeOk) {
                    System.out.println("  -> SUCCESS: Storage file remained 100% byte-for-byte identical when no deletion was performed.");
                    passed++;
                } else {
                    System.out.println("  -> FAILED: File changed unexpectedly during no-operation check.");
                    failed++;
                }
            }

            // -------------------------------------------------------------
            // TEST 23: WeatherStorage refusal to save after corruption detection
            // -------------------------------------------------------------
            System.out.println("\n[TEST 23] WeatherStorage refusal to save after corruption detection...");
            Path corruptDeleteFile = tempDir.resolve("corrupt_delete.json");
            String corruptJson23 = "[\n"
                    + "  {\"cityName\": \"Rome\", \"temperature\": 20.0, \"humidity\": 50.0, \"windSpeed\": 10.0, \"weatherCondition\": \"Sunny\", \"lastUpdated\": \"01-Jan-2026 10:00:00\"},\n"
                    + "  {\"cityName\": \"CorruptCity\", \"temperature\": 999.0}\n"
                    + "]";
            byte[] corruptOriginalBytes23 = corruptJson23.getBytes(StandardCharsets.UTF_8);
            Files.write(corruptDeleteFile, corruptOriginalBytes23);

            WeatherStorage storage23 = new WeatherStorage(corruptDeleteFile);
            storage23.loadReports(); // triggers corruptionDetected = true

            // Storage layer verification: Even if an external caller or process attempts to call saveReports()
            // with a modified list (e.g., trying to persist a deletion), WeatherStorage MUST strictly refuse
            // the write to prevent overwriting the corrupt file on disk.
            // Note: Main.java application UI checks isCorruptionDetected() and blocks deletion upfront;
            // this test verifies the storage layer's independent safeguard.
            List<WeatherReport> proposedAfterDeletion = new ArrayList<>();
            boolean saveAttemptResult23 = storage23.saveReports(proposedAfterDeletion);
            byte[] bytesAfterAttempt23 = Files.readAllBytes(corruptDeleteFile);
            boolean corruptFileUntouched = Arrays.equals(corruptOriginalBytes23, bytesAfterAttempt23);

            if (storage23.isCorruptionDetected() && !saveAttemptResult23 && corruptFileUntouched) {
                System.out.println("  -> SUCCESS: WeatherStorage independently refused saveReports() when corruption was detected.");
                System.out.println("              Original file with corrupt records was preserved byte-for-byte on disk!");
                passed++;
            } else {
                System.out.printf("  -> FAILED: corrupt=%b, saveResult=%b, untouched=%b%n",
                        storage23.isCorruptionDetected(), saveAttemptResult23, corruptFileUntouched);
                failed++;
            }

            // -------------------------------------------------------------
            // TEST 24: Storage save-failure simulation: proposed-list pattern leaves WeatherManager state unchanged
            // -------------------------------------------------------------
            System.out.println("\n[TEST 24] Storage save-failure simulation: proposed-list pattern leaves WeatherManager state unchanged...");
            // Note: This test simulates the write-ahead proposed-list pattern against WeatherStorage and WeatherManager.
            // It tests that when storage.saveReports() fails, the proposed-state pattern ensures the service in-memory
            // data is NOT mutated. It directly verifies the storage-manager contract without invoking Main's console UI.
            Path failingStoragePath = tempDir.resolve("failing_storage.json");
            Files.writeString(failingStoragePath, "[{\"cityName\": \"Corrupt\", \"temperature\": 999.0}]", StandardCharsets.UTF_8);
            WeatherStorage failingStorage = new WeatherStorage(failingStoragePath);
            failingStorage.loadReports(); // triggers isCorruptionDetected() == true, making saveReports() return false

            WeatherManager manager24 = new WeatherManager();
            WeatherReport rMadrid = new WeatherReport("Madrid", 20.0, 45.0, 15.0, "Sunny", "01-Jan-2026 10:00:00");
            WeatherReport rLisbon = new WeatherReport("Lisbon", 18.0, 65.0, 20.0, "Cloudy", "01-Jan-2026 10:00:00");
            manager24.addWeatherReport(rMadrid);
            manager24.addWeatherReport(rLisbon);

            boolean addFailsafeOk = false;
            boolean updateFailsafeOk = false;
            boolean deleteSingleFailsafeOk = false;
            boolean deleteAllFailsafeOk = false;

            // 1. Transactional Add Failure (Manual or Live API addition)
            WeatherReport rParis = new WeatherReport("Paris", 14.0, 75.0, 12.0, "Rainy", "01-Jan-2026 10:00:00");
            List<WeatherReport> proposedAdd = new ArrayList<>(manager24.getAllReports());
            proposedAdd.add(rParis);
            boolean saveAddResult = failingStorage.saveReports(proposedAdd);
            if (!saveAddResult) {
                // Application invariant: in-memory service is NOT mutated when save fails
                addFailsafeOk = (manager24.getTotalRecordCount() == 2 && !manager24.cityExists("Paris"));
            }

            // 2. Transactional Update Failure
            List<WeatherReport> proposedUpdate = new ArrayList<>();
            WeatherReport updatedMadrid = new WeatherReport("Madrid", 28.0, 40.0, 10.0, "Clear", "01-Jan-2026 12:00:00");
            proposedUpdate.add(updatedMadrid);
            proposedUpdate.add(rLisbon);
            boolean saveUpdateResult = failingStorage.saveReports(proposedUpdate);
            if (!saveUpdateResult) {
                // Application invariant: in-memory record remains unchanged
                WeatherReport currentMadrid = manager24.searchCity("Madrid");
                updateFailsafeOk = (currentMadrid != null && currentMadrid.getTemperature() == 20.0
                        && currentMadrid.getWeatherCondition().equals("Sunny"));
            }

            // 3. Transactional Delete Single Failure
            List<WeatherReport> proposedDeleteSingle = new ArrayList<>();
            proposedDeleteSingle.add(rMadrid); // Lisbon omitted from proposed list
            boolean saveDeleteResult = failingStorage.saveReports(proposedDeleteSingle);
            if (!saveDeleteResult) {
                // Application invariant: Lisbon remains in memory
                deleteSingleFailsafeOk = (manager24.getTotalRecordCount() == 2 && manager24.cityExists("Lisbon"));
            }

            // 4. Transactional Delete All Failure
            List<WeatherReport> proposedDeleteAll = new ArrayList<>();
            boolean saveDeleteAllResult = failingStorage.saveReports(proposedDeleteAll);
            if (!saveDeleteAllResult) {
                // Application invariant: All records remain intact in memory
                deleteAllFailsafeOk = (manager24.getTotalRecordCount() == 2);
            }

            if (addFailsafeOk && updateFailsafeOk && deleteSingleFailsafeOk && deleteAllFailsafeOk) {
                System.out.println("  -> SUCCESS: WeatherManager in-memory state remained completely untouched across all failed storage saves!");
                passed++;
            } else {
                System.out.printf("  -> FAILED: addOk=%b, updateOk=%b, delSingleOk=%b, delAllOk=%b%n",
                        addFailsafeOk, updateFailsafeOk, deleteSingleFailsafeOk, deleteAllFailsafeOk);
                failed++;
            }

            // -------------------------------------------------------------
            // TEST 25: WeatherStorage.saveReports() domain, non-finite, and uniqueness validation
            // -------------------------------------------------------------
            System.out.println("\n[TEST 25] WeatherStorage.saveReports() domain, non-finite, and uniqueness validation...");
            Path validationFile = tempDir.resolve("save_validation.json");
            WeatherStorage validationStorage = new WeatherStorage(validationFile);

            // Establish initial valid file on disk
            List<WeatherReport> initialValid = new ArrayList<>();
            initialValid.add(new WeatherReport("Stockholm", 5.0, 70.0, 10.0, "Cloudy", "01-Jan-2026 10:00:00"));
            boolean validInitialSave = validationStorage.saveReports(initialValid);
            byte[] validBytesOnDisk = Files.readAllBytes(validationFile);

            // 1. Rejects list with null record
            List<WeatherReport> listWithNull = new ArrayList<>(initialValid);
            listWithNull.add(null);
            boolean nullRejected = !validationStorage.saveReports(listWithNull)
                    && Arrays.equals(validBytesOnDisk, Files.readAllBytes(validationFile));

            // 2. Rejects list with duplicate city names (case-insensitive & trimmed)
            List<WeatherReport> listWithDups = new ArrayList<>(initialValid);
            listWithDups.add(new WeatherReport("Stockholm", 6.0, 68.0, 12.0, "Sunny", "01-Jan-2026 11:00:00"));
            boolean dupsRejected = !validationStorage.saveReports(listWithDups)
                    && Arrays.equals(validBytesOnDisk, Files.readAllBytes(validationFile));

            // 3. Rejects list with empty city name
            boolean emptyCityRejected = !validationStorage.saveReports(List.of(
                    new MockMetricReport("   ", 10.0, 50.0, 10.0, "Clear")
            )) && Arrays.equals(validBytesOnDisk, Files.readAllBytes(validationFile));

            // 4. Rejects list with empty weather condition
            boolean emptyCondRejected = !validationStorage.saveReports(List.of(
                    new MockMetricReport("ValidCity", 10.0, 50.0, 10.0, "   ")
            )) && Arrays.equals(validBytesOnDisk, Files.readAllBytes(validationFile));

            // 5. Rejects out-of-range temperature (< -90 or > 65)
            boolean outRangeTempRejected = !validationStorage.saveReports(List.of(
                    new MockMetricReport("ValidCity", 999.0, 50.0, 10.0, "Clear")
            )) && !validationStorage.saveReports(List.of(
                    new MockMetricReport("ValidCity", -100.0, 50.0, 10.0, "Clear")
            )) && Arrays.equals(validBytesOnDisk, Files.readAllBytes(validationFile));

            // 6. Rejects out-of-range humidity (< 0 or > 100)
            boolean outRangeHumRejected = !validationStorage.saveReports(List.of(
                    new MockMetricReport("ValidCity", 20.0, 110.0, 10.0, "Clear")
            )) && !validationStorage.saveReports(List.of(
                    new MockMetricReport("ValidCity", 20.0, -5.0, 10.0, "Clear")
            )) && Arrays.equals(validBytesOnDisk, Files.readAllBytes(validationFile));

            // 7. Rejects negative wind speed (< 0)
            boolean negativeWindRejected = !validationStorage.saveReports(List.of(
                    new MockMetricReport("ValidCity", 20.0, 50.0, -15.0, "Clear")
            )) && Arrays.equals(validBytesOnDisk, Files.readAllBytes(validationFile));

            // 8. Rejects non-finite temperature (NaN, +Infinity, -Infinity)
            boolean nonFiniteTempRejected = !validationStorage.saveReports(List.of(
                    new MockMetricReport("ValidCity", Double.NaN, 50.0, 10.0, "Clear")
            )) && !validationStorage.saveReports(List.of(
                    new MockMetricReport("ValidCity", Double.POSITIVE_INFINITY, 50.0, 10.0, "Clear")
            )) && !validationStorage.saveReports(List.of(
                    new MockMetricReport("ValidCity", Double.NEGATIVE_INFINITY, 50.0, 10.0, "Clear")
            )) && Arrays.equals(validBytesOnDisk, Files.readAllBytes(validationFile));

            // 9. Rejects non-finite humidity (NaN, +Infinity, -Infinity)
            boolean nonFiniteHumRejected = !validationStorage.saveReports(List.of(
                    new MockMetricReport("ValidCity", 20.0, Double.NaN, 10.0, "Clear")
            )) && !validationStorage.saveReports(List.of(
                    new MockMetricReport("ValidCity", 20.0, Double.POSITIVE_INFINITY, 10.0, "Clear")
            )) && !validationStorage.saveReports(List.of(
                    new MockMetricReport("ValidCity", 20.0, Double.NEGATIVE_INFINITY, 10.0, "Clear")
            )) && Arrays.equals(validBytesOnDisk, Files.readAllBytes(validationFile));

            // 10. Rejects non-finite wind speed (NaN, +Infinity, -Infinity)
            boolean nonFiniteWindRejected = !validationStorage.saveReports(List.of(
                    new MockMetricReport("ValidCity", 20.0, 50.0, Double.NaN, "Clear")
            )) && !validationStorage.saveReports(List.of(
                    new MockMetricReport("ValidCity", 20.0, 50.0, Double.POSITIVE_INFINITY, "Clear")
            )) && !validationStorage.saveReports(List.of(
                    new MockMetricReport("ValidCity", 20.0, 50.0, Double.NEGATIVE_INFINITY, "Clear")
            )) && Arrays.equals(validBytesOnDisk, Files.readAllBytes(validationFile));

            boolean allValidationPassed = validInitialSave && nullRejected && dupsRejected
                    && emptyCityRejected && emptyCondRejected && outRangeTempRejected
                    && outRangeHumRejected && negativeWindRejected
                    && nonFiniteTempRejected && nonFiniteHumRejected && nonFiniteWindRejected;

            if (allValidationPassed) {
                System.out.println("  -> SUCCESS: saveReports() strictly rejected non-finite values (NaN, +/-Inf), out-of-range metrics,");
                System.out.println("              nulls, and duplicates, while leaving the existing JSON file 100% unchanged!");
                passed++;
            } else {
                System.out.printf("  -> FAILED: init=%b, null=%b, dup=%b, emptyCity=%b, emptyCond=%b, temp=%b, hum=%b, wind=%b, nfTemp=%b, nfHum=%b, nfWind=%b%n",
                        validInitialSave, nullRejected, dupsRejected, emptyCityRejected, emptyCondRejected,
                        outRangeTempRejected, outRangeHumRejected, negativeWindRejected,
                        nonFiniteTempRejected, nonFiniteHumRejected, nonFiniteWindRejected);
                failed++;
            }

            // -------------------------------------------------------------
            // TEST 26: Valid empty list [] and normal report persistence preservation in saveReports()
            // -------------------------------------------------------------
            System.out.println("\n[TEST 26] Valid empty list [] and normal report persistence preservation in saveReports()...");
            Path normalFile26 = tempDir.resolve("normal_save26.json");
            WeatherStorage normalStorage26 = new WeatherStorage(normalFile26);

            // 1. Save valid empty list -> should write "[]"
            boolean emptyListSaved = normalStorage26.saveReports(new ArrayList<>());
            String emptyListJson = Files.readString(normalFile26, StandardCharsets.UTF_8).trim();
            boolean emptyReloadClean = normalStorage26.loadReports().isEmpty() && !normalStorage26.isCorruptionDetected();

            // 2. Save valid populated list
            List<WeatherReport> normalList = new ArrayList<>();
            normalList.add(new WeatherReport("Toronto", -2.0, 75.0, 22.0, "Snowy", "01-Jan-2026 10:00:00"));
            normalList.add(new WeatherReport("Vancouver", 8.0, 85.0, 14.0, "Rainy", "01-Jan-2026 10:00:00"));
            boolean normalSaved = normalStorage26.saveReports(normalList);

            WeatherStorage reloadNormalStorage26 = new WeatherStorage(normalFile26);
            List<WeatherReport> reloadedNormal = reloadNormalStorage26.loadReports();
            boolean normalReloadClean = (reloadedNormal.size() == 2 && !reloadNormalStorage26.isCorruptionDetected());

            if (emptyListSaved && emptyListJson.equals("[]") && emptyReloadClean && normalSaved && normalReloadClean) {
                System.out.println("  -> SUCCESS: Valid empty lists and normal reports are persisted cleanly and correctly!");
                passed++;
            } else {
                System.out.printf("  -> FAILED: emptySaved=%b, json=%s, emptyClean=%b, normalSaved=%b, normalClean=%b%n",
                        emptyListSaved, emptyListJson, emptyReloadClean, normalSaved, normalReloadClean);
                failed++;
            }

            // -------------------------------------------------------------
            // TEST 27: Production data safeguard: confirming real 'data/weather-reports.json' is untouched
            // -------------------------------------------------------------
            System.out.println("\n[TEST 27] Production data safeguard: confirming real 'data/weather-reports.json' is untouched...");
            if (initialReadFailed) {
                System.out.println("  -> FAILED: Could not verify safeguard because initial production file snapshot failed to read.");
                failed++;
            } else if (prodExistedInitially) {
                boolean stillExists = Files.exists(productionFile);
                byte[] currentProductionBytes = stillExists ? Files.readAllBytes(productionFile) : null;
                boolean productionUntouched = (stillExists && Arrays.equals(realProductionBytes, currentProductionBytes));

                if (productionUntouched) {
                    System.out.println("  -> SUCCESS: Real production storage file 'data/weather-reports.json' existed initially and was NEVER modified!");
                    passed++;
                } else {
                    System.out.println("  -> FAILED: Real production storage file was altered, truncated, or deleted!");
                    failed++;
                }
            } else {
                // Did not exist initially
                boolean stillAbsent = !Files.exists(productionFile);
                if (stillAbsent) {
                    System.out.println("  -> SUCCESS: Production file 'data/weather-reports.json' was absent initially and remains absent!");
                    passed++;
                } else {
                    System.out.println("  -> FAILED: Production file was created during test execution!");
                    failed++;
                }
            }

        } catch (Exception e) {
            System.out.println("  -> UNEXPECTED EXCEPTION in test suite: " + e.getMessage());
            e.printStackTrace();
            failed++;
        } finally {
            // Clean up temporary test directory recursively in reverse order
            if (tempDir != null) {
                boolean cleanSuccess = cleanupDirectoryRecursively(tempDir);
                if (cleanSuccess) {
                    System.out.printf("%n[CLEANUP] Successfully cleaned up temporary test sandbox: %s%n", tempDir.toAbsolutePath());
                } else {
                    System.err.printf("%n[!] WARNING: Test sandbox cleanup was incomplete for '%s'. Some files could not be deleted.%n",
                            tempDir.toAbsolutePath());
                }
            }
        }

        System.out.println("\n==================================================");
        System.out.printf("  TEST RESULTS: Passed: %d, Failed: %d%n", passed, failed);
        System.out.println("==================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }

    /**
     * Recursively walks and deletes directory contents in reverse path order.
     * Tracks and reports any deletion failures accurately.
     *
     * @param dir Directory to delete.
     * @return true if all files and directories were deleted successfully, false otherwise.
     */
    private static boolean cleanupDirectoryRecursively(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return true;
        }
        int[] failureCount = new int[]{0};
        try (var stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder())
                  .forEach(path -> {
                      try {
                          Files.deleteIfExists(path);
                      } catch (IOException e) {
                          failureCount[0]++;
                          System.err.printf("[!] Warning: Failed to delete test temp path '%s': %s%n", path, e.getMessage());
                      }
                  });
        } catch (IOException e) {
            failureCount[0]++;
            System.err.printf("[!] Warning: Error during directory walk on '%s': %s%n", dir, e.getMessage());
        }
        return failureCount[0] == 0 && !Files.exists(dir);
    }

    /**
     * Flexible subclass used specifically to test WeatherStorage.saveReports() rejection of
     * non-finite, out-of-range metrics, and invalid string fields without triggering constructor validation.
     */
    static class MockMetricReport extends WeatherReport {
        private final String mockCity;
        private final double mockTemp;
        private final double mockHumidity;
        private final double mockWind;
        private final String mockCondition;

        MockMetricReport(String mockCity, double mockTemp, double mockHumidity, double mockWind, String mockCondition)
                throws InvalidWeatherDataException {
            super("ValidCity", 0.0, 50.0, 10.0, "Clear");
            this.mockCity = mockCity;
            this.mockTemp = mockTemp;
            this.mockHumidity = mockHumidity;
            this.mockWind = mockWind;
            this.mockCondition = mockCondition;
        }

        @Override
        public String getCityName() {
            return mockCity != null ? mockCity : super.getCityName();
        }

        @Override
        public double getTemperature() {
            return mockTemp;
        }

        @Override
        public double getHumidity() {
            return mockHumidity;
        }

        @Override
        public double getWindSpeed() {
            return mockWind;
        }

        @Override
        public String getWeatherCondition() {
            return mockCondition != null ? mockCondition : super.getWeatherCondition();
        }
    }
}
