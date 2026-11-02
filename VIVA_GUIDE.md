# Weather Report System: Viva & Oral Examination Guide

This guide is designed for undergraduate engineering students preparing to present the **Weather Report System** during a college lab viva, project defense, or oral examination. It explains every design choice, OOP concept, algorithm, and class in plain, beginner-friendly English with references to the actual source code.

---

## Table of Contents
1. [Project Summary in 60 Seconds](#1-project-summary-in-60-seconds)
2. [Why Java Was Chosen](#2-why-java-was-chosen)
3. [Deep-Dive into Every Project Class (12 Files)](#3-deep-dive-into-every-project-class-12-files)
4. [How the Classes Communicate (3-Tier Architecture & Persistence Flow)](#4-how-the-classes-communicate-3-tier-architecture--persistence-flow)
5. [Core OOP Concepts Demonstrated (With Actual Code Snippets)](#5-core-oop-concepts-demonstrated-with-actual-code-snippets)
6. [Collections Framework & Defensive Unmodifiable Views](#6-collections-framework--defensive-unmodifiable-views)
7. [The `WeatherService` Interface & Interface-Based Design](#7-the-weatherservice-interface--interface-based-design)
8. [The `WeatherApiClient` Interface & Network Decoupling](#8-the-weatherapiclient-interface--network-decoupling)
9. [The `WeatherStorage` Persistence Architecture](#9-the-weatherstorage-persistence-architecture)
10. [Transactional Write-Ahead Consistency Pattern](#10-transactional-write-ahead-consistency-pattern)
11. [Inheritance Hierarchy: `BaseWeatherReport` & `WeatherReport`](#11-inheritance-hierarchy-baseweatherreport--weatherreport)
12. [Polymorphism in Action: Overriding vs. Overloading](#12-polymorphism-in-action-overriding-vs-overloading)
13. [Exception Handling Architecture & Checked Exceptions](#13-exception-handling-architecture--checked-exceptions)
14. [How Java `HttpClient` Works (Synchronous Execution)](#14-how-java-httpclient-works-synchronous-execution)
15. [JSON Serialization & Deserialization with Google Gson](#15-json-serialization--deserialization-with-google-gson)
16. [The Open-Meteo REST API Workflow](#16-the-open-meteo-rest-api-workflow)
17. [Temperature Conversion Mathematics](#17-temperature-conversion-mathematics)
18. [Two-Tier Input Validation & Floating-Point Sanitization (`Double.isFinite()`)](#18-two-tier-input-validation--floating-point-sanitization-doubleisfinite)
19. [Deletion Workflows (Single Delete vs. Full Clear)](#19-deletion-workflows-single-delete-vs-full-clear)
20. [Standalone Verification Suites (`TestWeatherStorage` & `TestOpenMeteoClient`)](#20-standalone-verification-suites-testweatherstorage--testopenmeteoclient)
21. [Why Program to an Interface, Not an Implementation?](#21-why-program-to-an-interface-not-an-implementation)
22. [Project Limitations & Future Scope](#22-project-limitations--future-scope)
23. [32 Likely Viva Questions & Answers](#23-32-likely-viva-questions--answers)

---

## 1. Project Summary in 60 Seconds

> **How to describe your project to the examiner:**
>
> *"Our project is a robust, console-based **Weather Report System** written in Java. It allows users to manage weather observations across multiple cities with full JSON file persistence. The application features eight interactive menu options, supporting manual record entry, real-time live weather retrieval from the Open-Meteo API using Java's native `HttpClient` and Google Gson without requiring an API key, detailed report displays, side-by-side metric tables, case-insensitive search, updates, and single or bulk record deletion.*
> 
> *A major strength of the system is its crash-resilient persistence layer (`WeatherStorage`): it uses safe atomic file replacement via temporary files and `StandardCopyOption.ATOMIC_MOVE`, strict anti-corruption protection that locks write access if disk data is tampered with, duplicate city detection during load, and a transactional write-ahead pattern ensuring that in-memory collections and disk storage never drift out of sync. The project demonstrates core Object-Oriented Programming principles including Encapsulation, Inheritance, Abstraction, Polymorphism, and custom checked Exception handling."*

---

## 2. Why Java Was Chosen

1. **Class-Based OOP Purity**: Java natively enforces class boundaries, making it ideal for demonstrating inheritance, encapsulation, abstract classes, and polymorphic interfaces.
2. **Standard HTTP Client (`java.net.http.HttpClient`)**: Introduced in Java 11, Java provides a modern, high-performance HTTP client that executes synchronous requests with configurable timeouts and redirection handling, eliminating legacy third-party dependencies.
3. **Platform Independence ("Write Once, Run Anywhere")**: The compiled bytecode runs identically across Windows, macOS, and Linux without OS-specific modifications.
4. **Strong Static Typing**: Catches type mismatches, missing fields, and contract violations at compile time rather than during runtime evaluation.
5. **Checked Exception Architecture**: Java differentiates between checked and unchecked exceptions, enforcing the 'handle or declare' rule for network failures, disk I/O, and domain validation.
6. **Modern File I/O (`java.nio.file`)**: Java NIO provides safe, atomic filesystem operations such as `Files.move()` with `ATOMIC_MOVE` and `REPLACE_EXISTING`, preventing corrupt partial writes on system power loss.

---

## 3. Deep-Dive into Every Project Class (12 Files)

The project is organized in the `com.weather` package across 12 source files:

| File Name | Class / Interface Type | Role and Purpose |
| :--- | :--- | :--- |
| **`BaseWeatherReport.java`** | `public abstract class` | Foundational domain entity. Encapsulates common attributes: `cityName`, `temperature` (°C), and `weatherCondition`. Declares the abstract method `displayReport()` and overrides `toString()`. Validates temperature using `Double.isFinite()`. |
| **`WeatherReport.java`** | `public class extends BaseWeatherReport` | Concrete domain model. Inherits base fields and adds `humidity`, `windSpeed`, and `lastUpdated`. Overrides `displayReport()`, overloads `displayReport(boolean)`, implements temperature conversion to Fahrenheit, and overrides `toString()`. Validates humidity and wind speed using `Double.isFinite()`. |
| **`WeatherService.java`** | `public interface` | Service layer abstraction defining the contract for managing stored weather reports (add, retrieve, search, update, delete, clear, check duplicates). |
| **`WeatherManager.java`** | `public class implements WeatherService` | Service implementation. Stores and filters reports in memory using an encapsulated `ArrayList<WeatherReport>`, returning defensive unmodifiable views. |
| **`WeatherStorage.java`** | `public class` | Persistence component. Loads and saves weather reports to `data/weather-reports.json`. Implements atomic temporary file replacement, corrupt file protection (`corruptionDetected`), duplicate city filtering, and schema validation. |
| **`WeatherApiClient.java`** | `public interface` | Remote service abstraction defining the contract for fetching live weather data from an external provider. |
| **`OpenMeteoApiClient.java`** | `public class implements WeatherApiClient` | API implementation. Communicates synchronously with Open-Meteo Geocoding and Forecast endpoints, deserializes JSON with Gson, and maps WMO codes. |
| **`InvalidWeatherDataException.java`** | `public class extends Exception` | Custom checked exception for physical domain violations (e.g. temperature out of Earth range, negative wind speed, non-finite values). |
| **`WeatherApiException.java`** | `public class extends Exception` | Custom checked exception for network errors, timeouts, HTTP non-200 responses, and unresolvable city queries. |
| **`Main.java`** | `public class` | Presentation controller. Drives the 8-option console menu, handles user input sanitization, coordinates transactional write-ahead updates with `WeatherStorage`, and provides manual fallback. |
| **`TestWeatherStorage.java`** | `public class` | Standalone test runner (27 tests) verifying JSON serialization, atomic writes, corruption detection, duplicate detection, and domain validation. |
| **`TestOpenMeteoClient.java`** | `public class` | Standalone test runner (6 tests) executing programmatic verification against the Open-Meteo API client and WMO mapping logic. |

---

## 4. How the Classes Communicate (3-Tier Architecture & Persistence Flow)

The application follows a clean 3-tier architecture: **Presentation $\rightarrow$ Service / API / Storage $\rightarrow$ Domain Model**.

```
                           [User at Console]
                                   │
                                   ▼
                             Main.java (UI Controller)
                                   │
      ┌────────────────────────────┼────────────────────────────┐
      │                            │                            │
      ▼                            ▼                            ▼
WeatherService               WeatherApiClient             WeatherStorage
(WeatherManager)           (OpenMeteoApiClient)           (JSON File I/O)
      │                            │                            │
      │ Manages in-memory          │ Synchronous HTTP GET       │ Atomic file replacement
      │ ArrayList<WeatherReport>   │ Geocoding & Forecast APIs  │ Corrupt file locking
      │ Defensive views            │ Google Gson parsing        │ Duplicate detection
      │                            │                            │
      └────────────────────────────┼────────────────────────────┘
                                   │
                                   ▼
                           WeatherReport Object
                     (Encapsulated Domain Entity)
                                   │
                                   ▼
                      data/weather-reports.json
```

### The Transactional Write-Ahead Flow:
When the user adds, updates, or deletes a report:
1. `Main` asks `WeatherService` for a copy of the current report list.
2. `Main` builds a **proposed list** reflecting the desired change (new report added, updated, or removed).
3. `Main` passes the proposed list to `WeatherStorage.saveReports(proposedList)`.
4. `WeatherStorage` validates every record, writes to a temporary file, and atomically replaces `data/weather-reports.json`.
5. **Only if the save succeeds**, `Main` instructs `WeatherService` to apply the mutation to its in-memory `ArrayList`.
6. If saving fails, in-memory state remains untouched, preventing memory-disk drift.

---

## 5. Core OOP Concepts Demonstrated (With Actual Code Snippets)

### A. Encapsulation (Data Hiding & Validation)
**What it is:** Keeping fields `private` and exposing them only through public getters and validated setters.

**Actual Code in `BaseWeatherReport.java`:**
```java
private String cityName;
private double temperature;       // In Celsius
private String weatherCondition;  // e.g., Sunny, Cloudy, Rainy, Stormy

public void setTemperature(double temperature) throws InvalidWeatherDataException {
    // Rejects non-finite values (NaN, +Infinity, -Infinity) and out-of-range temperatures
    if (!Double.isFinite(temperature) || temperature < -90.0 || temperature > 65.0) {
        throw new InvalidWeatherDataException(
            "Temperature " + temperature + "°C is out of realistic earthly range (-90°C to 65°C)."
        );
    }
    this.temperature = temperature;
}
```
**Actual Code in `WeatherReport.java`:**
```java
private double humidity;
private double windSpeed;
private String lastUpdated;

public void setHumidity(double humidity) throws InvalidWeatherDataException {
    if (!Double.isFinite(humidity) || humidity < 0.0 || humidity > 100.0) {
        throw new InvalidWeatherDataException(
            "Humidity " + humidity + "% is out of realistic earthly range (0% to 100%)."
        );
    }
    this.humidity = humidity;
}
```
**Viva Explanation:** *"By making fields private, outside classes cannot corrupt object state by assigning $500^\circ\text{C}$ or `Double.NaN`. All mutations pass through setters that validate physical Earth boundaries."*

---

### B. Inheritance (Code Reuse & Specialization)
**What it is:** A subclass inherits properties and methods from a superclass using the `extends` keyword.

**Actual Code in `WeatherReport.java`:**
```java
public class WeatherReport extends BaseWeatherReport {
    private double humidity;
    private double windSpeed;
    private String lastUpdated;

    public WeatherReport(String cityName, double temperature, double humidity, 
                         double windSpeed, String weatherCondition)
            throws InvalidWeatherDataException {
        super(cityName, temperature, weatherCondition); // Invoking superclass constructor
        setHumidity(humidity);
        setWindSpeed(windSpeed);
        this.lastUpdated = LocalDateTime.now().format(FORMATTER);
    }
}
```
**Viva Explanation:** *"Instead of repeating common fields like `cityName` and `temperature` in `WeatherReport`, we inherit them from `BaseWeatherReport`. `super(...)` forwards common arguments to the parent constructor for centralized validation."*

---

### C. Abstraction (Hiding Implementation Complexity)
**What it is:** Exposing what an object does while hiding how it does it.

1. **Abstract Class (`BaseWeatherReport.java`)**:
   ```java
   public abstract class BaseWeatherReport {
       // Forces all subclasses to define their own visual display logic
       public abstract void displayReport();
   }
   ```
2. **Interface Abstraction (`WeatherService.java` & `WeatherApiClient.java`)**:
   ```java
   public interface WeatherApiClient {
       WeatherReport fetchCurrentWeather(String cityName) 
               throws WeatherApiException, InvalidWeatherDataException;
   }
   ```
**Viva Explanation:** *"`Main` only interacts with the `WeatherApiClient` and `WeatherService` interfaces. It has no knowledge of HTTP sockets, Gson reflection, or whether records reside in RAM or a file."*

---

### D. Polymorphism (Many Forms)

1. **Method Overriding (Runtime Polymorphism / Dynamic Method Dispatch)**:
   In `BaseWeatherReport.java`:
   ```java
   public abstract void displayReport();
   ```
   In `WeatherReport.java`:
   ```java
   @Override
   public void displayReport() {
       displayReport(true); // delegates to overloaded method
   }
   ```
   Also demonstrated in `toString()`:
   - `BaseWeatherReport.java`:
     ```java
     @Override
     public String toString() {
         return "City: " + cityName + ", Temperature: " + temperature + "°C, Condition: " + weatherCondition;
     }
     ```
   - `WeatherReport.java`:
     ```java
     @Override
     public String toString() {
         return String.format(
             "WeatherReport [City=%s, Temp=%.1f°C, Humidity=%.1f%%, Wind=%.1f km/h, Condition=%s, Updated=%s]",
             getCityName(), getTemperature(), humidity, windSpeed, getWeatherCondition(), lastUpdated
         );
     }
     ```

2. **Method Overloading (Compile-Time Polymorphism / Static Polymorphism)**:
   In `WeatherReport.java`:
   ```java
   // Signature 1: Zero arguments
   public void displayReport() { displayReport(true); }

   // Signature 2: One boolean argument
   public void displayReport(boolean showDetails) { ... }
   ```
   Also demonstrated in overloaded constructors in `WeatherReport.java` (default constructor, 5-parameter constructor, and 6-parameter constructor with explicit timestamp).

---

## 6. Collections Framework & Defensive Unmodifiable Views

In `WeatherManager.java`:
```java
private final List<WeatherReport> weatherReports;

public WeatherManager() {
    this.weatherReports = new ArrayList<>();
}

@Override
public List<WeatherReport> getAllReports() {
    return Collections.unmodifiableList(weatherReports);
}
```

### Why use `ArrayList`?
1. **Dynamic Resizing**: Standard Java arrays (`WeatherReport[]`) have a fixed size. An `ArrayList` grows automatically as users add more cities.
2. **High-Performance Iteration**: Offers $O(1)$ random access and efficient sequential traversal for display and metric calculations.

### Crucial Distinction: What `Collections.unmodifiableList()` Actually Does
`Collections.unmodifiableList(weatherReports)` provides an **unmodifiable wrapper view** of the collection:
- **Structural Modifications are Blocked**: Any attempt to modify the list structure through the returned reference (such as calling `.add()`, `.remove()`, or `.clear()`) throws an `UnsupportedOperationException`.
- **Objects Inside are NOT Immutable**: This is a shallow wrapper, not a deep copy. The underlying `WeatherReport` objects inside the list can still have their state altered if an external caller obtains a report reference and calls its setters.

---

## 7. The `WeatherService` Interface & Interface-Based Design

```java
public interface WeatherService {
    boolean addWeatherReport(WeatherReport report) throws InvalidWeatherDataException;
    List<WeatherReport> getAllReports();
    WeatherReport searchCity(String cityName);
    boolean updateWeatherReport(String cityName, double temp, double hum, double wind, String cond) 
        throws InvalidWeatherDataException;
    boolean deleteWeatherReport(String cityName);
    void clearAllReports();
    boolean cityExists(String cityName);
    void displayAllReports();
    void displayKeyMetrics();
    int getTotalRecordCount();
}
```

### Why use an interface here?
- Decouples the presentation layer from the data management layer.
- If we replace `ArrayList` with an SQL database (e.g., PostgreSQL or SQLite), we can create `DatabaseWeatherService implements WeatherService` without changing any menu or UI logic in `Main.java`.

---

## 8. The `WeatherApiClient` Interface & Network Decoupling

```java
public interface WeatherApiClient {
    WeatherReport fetchCurrentWeather(String cityName) 
            throws WeatherApiException, InvalidWeatherDataException;
}
```

### Why separate `WeatherApiClient` from `WeatherService`?
- **Single Responsibility Principle (SRP)**: `WeatherService` manages and stores records. `WeatherApiClient` communicates over the network to fetch real-time data.
- **Pluggability**: We can substitute `OpenMeteoApiClient` with a mock client or another provider (e.g. NOAA or OpenWeatherMap) without altering storage or UI code.

---

## 9. The `WeatherStorage` Persistence Architecture

`WeatherStorage.java` provides crash-resilient JSON persistence at `data/weather-reports.json`.

```
                    ┌───────────────────────────┐
                    │ WeatherStorage.saveReports│
                    └─────────────┬─────────────┘
                                  │
                                  ▼
                   [Validate reports & check finite]
                                  │
                                  ▼
                   Write JSON to temp file:
                   data/weather-reports-1711...tmp
                                  │
                                  ▼
                   Flush & close file streams
                                  │
                                  ▼
                   Files.move(..., ATOMIC_MOVE, REPLACE_EXISTING)
                                  │
                                  ▼
                   Target file data/weather-reports.json
                   replaced cleanly without partial corruption!
```

### Key Architectural Features:
1. **Crash-Resilient Atomic Writes**:
   - Instead of writing directly to `data/weather-reports.json`, `saveReports()` creates a temporary file in the same directory (`data/weather-reports-*.tmp`).
   - It serializes the reports, flushes the writer, and closes the stream.
   - It uses `Files.move(tempFile, targetFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)`.
   - If power fails or the process is killed during writing, the original JSON file remains intact.
   - If the filesystem does not support atomic moves, it catches `AtomicMoveNotSupportedException` and falls back to standard `REPLACE_EXISTING`.
2. **Anti-Corruption Protection (`corruptionDetected` flag)**:
   - If the JSON file contains malformed syntax, a non-array root (`{}`, `"string"`, `42`), null records, missing fields, or out-of-range physical values, `WeatherStorage` sets `corruptionDetected = true`.
   - Once flagged, `saveReports()` **refuses to write** to disk. This prevents an invalid file from being accidentally overwritten and destroyed.
3. **Duplicate City Detection on Startup**:
   - `loadReports()` detects duplicate city names (comparing case-insensitively and ignoring whitespace).
   - If duplicates exist (e.g. `"Kolkata"` and `"kolkata"`), `corruptionDetected` is set to `true`, valid unique records are loaded into memory for read-only inspection, and writes are blocked until the file is repaired.

---

## 10. Transactional Write-Ahead Consistency Pattern

In simple console applications, a common defect is modifying in-memory data before saving to disk. If saving fails (e.g. disk full or corruption lock), memory and disk drift apart.

`WeatherReportSystem` implements a **Transactional Write-Ahead Pattern** across all mutating operations:

```java
// Example from Main.java (Manual Add):
List<WeatherReport> proposedList = new ArrayList<>(weatherService.getAllReports());
proposedList.add(newReport);

// 1. Attempt disk persistence first
boolean saved = weatherStorage.saveReports(proposedList);

// 2. Only mutate in-memory service if persistence succeeded
if (saved) {
    weatherService.addWeatherReport(newReport);
    System.out.println("Weather report saved successfully.");
} else {
    System.out.println("Error: Failed to persist report to disk. In-memory state unchanged.");
}
```

This guarantees:
- **No In-Memory Drift**: If saving is rejected by `WeatherStorage` (due to corruption protection or I/O failure), the in-memory `ArrayList` remains untouched.
- **Fail-Safe Deletion**: Deleting a record checks `isCorruptionDetected()` upfront. If corrupted, deletion is refused before any prompts or operations take place.

---

## 11. Inheritance Hierarchy: `BaseWeatherReport` & `WeatherReport`

```
       +---------------------------------------------+
       |             BaseWeatherReport               |
       |  (Abstract: cityName, temp, condition)      |
       |   setTemperature() with isFinite() check    |
       +---------------------------------------------+
                              ^
                              |  extends (inherits)
       +---------------------------------------------+
       |                WeatherReport                |
       |  (Concrete: humidity, windSpeed, timestamp, |
       |   Fahrenheit conversion, displayReport(),   |
       |   setHumidity() & setWindSpeed() checks)    |
       +---------------------------------------------+
```

### What does `WeatherReport` inherit?
- Fields: `cityName`, `temperature`, `weatherCondition`.
- Methods: `getCityName()`, `setCityName()`, `getTemperature()`, `setTemperature()`, `getWeatherCondition()`, `setWeatherCondition()`, `getBasicSummary()`.
- Constructor behavior via `super(cityName, temperature, weatherCondition)`.

---

## 12. Polymorphism in Action: Overriding vs. Overloading

| Characteristic | Method Overloading (Compile-Time) | Method Overriding (Runtime) |
| :--- | :--- | :--- |
| **Where in project** | `WeatherReport.displayReport()` vs `displayReport(boolean)` | `WeatherReport.displayReport()` overriding `BaseWeatherReport.displayReport()` |
| **Parameters** | Must have different parameter lists | Must have the exact same signature |
| **Return type** | Can be the same or different | Must be the same (or covariant) |
| **Resolution time** | Resolved at compile time by compiler | Resolved at runtime using Dynamic Method Dispatch |
| **Annotation** | None | `@Override` |

---

## 13. Exception Handling Architecture & Checked Exceptions

```
                  java.lang.Throwable
                           │
                  java.lang.Exception (Checked)
                   ├── InvalidWeatherDataException
                   └── WeatherApiException
```

### The "Handle or Declare" Rule
Because both custom exceptions extend `java.lang.Exception`, they are **checked exceptions**. The Java compiler strictly enforces the **"handle or declare"** rule:
- Calling code must either catch the exception using a `try-catch` block:
  ```java
  try {
      weatherApiClient.fetchCurrentWeather(cityName);
  } catch (WeatherApiException e) { ... }
  ```
- **OR** declare that it propagates the exception up the call stack using the `throws` keyword:
  ```java
  public WeatherReport fetchCurrentWeather(String cityName) 
          throws WeatherApiException, InvalidWeatherDataException { ... }
  ```

### Two Distinct Custom Exceptions:
1. **`InvalidWeatherDataException` (Domain Validation)**: Thrown when data violates physical limits on Earth (e.g. temperature $< -90^\circ\text{C}$ or $> 65^\circ\text{C}$, relative humidity $< 0\%$ or $> 100\%$, negative wind speed, non-finite values like `NaN` or `Infinity`, empty city name).
2. **`WeatherApiException` (Network & API Infrastructure)**: Thrown when external network communication fails (e.g. timeout, connection error, non-200 HTTP response, unresolvable city, or malformed API JSON).

---

## 14. How Java `HttpClient` Works (Synchronous Execution)

In `OpenMeteoApiClient.java`:
```java
HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .timeout(Duration.ofSeconds(10))
        .header("Accept", "application/json")
        .GET()
        .build();

// Synchronous (blocking) execution
HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
```

1. **Synchronous Execution**: The call to `httpClient.send()` is **blocking and synchronous**. The calling thread pauses and waits until the response is received, a timeout expires, or an exception is thrown.
2. **`HttpRequest`**: Specifies URI, HTTP method (`GET`), request headers, and a 10-second timeout.
3. **`HttpResponse<String>`**: `BodyHandlers.ofString()` converts incoming byte streams into a Java `String`.
4. **Status Code Check**: `if (response.statusCode() != 200)` throws `WeatherApiException`.

---

## 15. JSON Serialization & Deserialization with Google Gson

### What is Gson?
Google Gson is an open-source Java library that converts Java Objects into JSON representation (serialization) and JSON text into equivalent Java Objects (deserialization).

### How Gson is Used in `OpenMeteoApiClient`:
Uses typed static Data Transfer Objects (DTOs) with `gson.fromJson(jsonResponse, ForecastApiResponse.class)`.

### How Gson is Used in `WeatherStorage`:
1. **Serialization (`saveReports`)**:
   ```java
   Gson gson = new GsonBuilder().setPrettyPrinting().create();
   String json = gson.toJson(reports);
   ```
   `setPrettyPrinting()` produces human-readable, indented JSON with line breaks.
2. **Deserialization (`loadReports`)**:
   ```java
   Type reportListType = new TypeToken<List<WeatherReport>>() {}.getType();
   List<WeatherReport> rawReports = gson.fromJson(jsonElement, reportListType);
   ```
   **Why `TypeToken`?** In Java, generic type information like `List<WeatherReport>` is erased at runtime due to **Type Erasure**. `new TypeToken<List<WeatherReport>>() {}.getType()` uses an anonymous inner class to capture the full generic type parameter at compile time so Gson knows to instantiate `WeatherReport` elements inside the list.

---

## 16. The Open-Meteo REST API Workflow

Open-Meteo requires a two-step query sequence:

```
Step 1: Geocoding
User types "Kolkata" 
   ──► GET https://geocoding-api.open-meteo.com/v1/search?name=Kolkata&count=5&format=json
   ──► Returns JSON array of matching cities
   ──► Client selects best match: Latitude 22.5626, Longitude 88.3630

Step 2: Forecast
Client takes coordinates 
   ──► GET https://api.open-meteo.com/v1/forecast?latitude=22.5626&longitude=88.3630&current=temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code&wind_speed_unit=kmh
   ──► Returns current weather measurements
   ──► Client translates WMO code (e.g., 61 -> "Slight Rain")
   ──► Constructs domain WeatherReport object
```

### URL Encoding Details
`URLEncoder.encode(cityName, StandardCharsets.UTF_8)` encodes spaces as `+` according to standard `x-www-form-urlencoded` rules, preventing malformed URL queries.

### WMO Weather Interpretation Codes
Open-Meteo reports conditions using specific integer values defined in WMO Code Table 4677 (e.g. 0 for Clear Sky, 61 for Slight Rain, 95 for Thunderstorm). `mapWmoCodeToCondition(int code)` maps these specific values to descriptive text.

---

## 17. Temperature Conversion Mathematics

Stored temperatures are maintained in **Celsius (°C)**. Fahrenheit is calculated dynamically:

$$\text{°F} = \left(\text{°C} \times \frac{9}{5}\right) + 32$$

In `WeatherReport.java`:
```java
public double getTemperatureInFahrenheit() {
    return (getTemperature() * 9.0 / 5.0) + 32.0;
}
```
*Note: We write `9.0 / 5.0` instead of `9 / 5`. In Java, integer division `9 / 5` evaluates to `1` due to truncation! Using floating-point literals `9.0 / 5.0` prevents truncation and ensures mathematical precision.*

---

## 18. Two-Tier Input Validation & Floating-Point Sanitization (`Double.isFinite()`)

The project employs two distinct validation layers:

1. **Syntactic & Type Validation (`Main.java`)**:
   - `readNonEmptyString()`: Loops until the user enters non-whitespace text.
   - `readIntegerInput()`: Catches `NumberFormatException` so typing letters (like `"abc"`) when asked for a menu choice does not crash the program.
   - `readDoubleInput()`: Ensures numeric inputs stay within prompted menu boundaries.
2. **Semantic & Physical Domain Validation (`BaseWeatherReport.java`, `WeatherReport.java`, and `WeatherStorage.java`)**:
   - In Java, `Double.NaN < -90.0` and `Double.NaN > 65.0` both evaluate to `false`! Therefore, a simple range check `if (temp < -90 || temp > 65)` allows `Double.NaN` to slip through undetected.
   - To eliminate this IEEE 754 vulnerability, domain setters and `WeatherStorage.saveReports()` explicitly check `!Double.isFinite(value)`:
     ```java
     if (!Double.isFinite(temperature) || temperature < -90.0 || temperature > 65.0) {
         throw new InvalidWeatherDataException(...);
     }
     ```
   - This cleanly rejects `NaN`, `Double.POSITIVE_INFINITY`, and `Double.NEGATIVE_INFINITY`.

---

## 19. Deletion Workflows (Single Delete vs. Full Clear)

Menu Option 7 provides safe deletion of weather records:

1. **Sub-Menu Options**:
   - `1`: Delete a single city's weather report.
   - `2`: Delete all weather reports (bulk clear).
   - `3`: Return to main menu.
2. **Corruption Guard**:
   - Before prompting the user, `Main` checks `weatherStorage.isCorruptionDetected()`. If true, deletion is refused with an explanation that the underlying file is corrupted and must be inspected manually.
3. **Safety Confirmations**:
   - Sub-option 1 prompts for city name, searches case-insensitively, displays the report, and asks `Are you sure you want to delete this report? (y/n)`.
   - Sub-option 2 warns of permanent loss and requires explicit `y/n` confirmation.
4. **Write-Ahead Persistence**:
   - Sub-option 1 prepares a filtered list excluding the target city, persists it via `weatherStorage.saveReports()`, and only removes it from `WeatherService` upon success.
   - Sub-option 2 writes an empty JSON array `[]` via `weatherStorage.saveReports()`, and only calls `weatherService.clearAllReports()` upon success.

---

## 20. Standalone Verification Suites (`TestWeatherStorage` & `TestOpenMeteoClient`)

Rather than relying on heavyweight external testing frameworks, the project contains two self-contained test runners:

1. **`TestWeatherStorage.java` (27 Programmatic Tests)**:
   - Uses temporary isolated files created via `Files.createTempFile()` and cleans them up in `finally` blocks.
   - Covers: clean initialization, save/load roundtrips, atomic writes, corruption flags for non-array roots (`{}`, `"text"`, `42`), truncated JSON, missing required fields, out-of-range values, `NaN` and `Infinity` rejection, case-insensitive duplicate city detection, and save-blocking on corrupted files.
2. **`TestOpenMeteoClient.java` (6 Programmatic Tests)**:
   - Tests live geocoding and forecast retrieval for valid cities (`Kolkata`, `London`).
   - Tests error handling for non-existent cities (`AtlantisXYZ12345`), blank inputs, and WMO code translations.

---

## 21. Why Program to an Interface, Not an Implementation?

In `Main.java`:
```java
private static final WeatherService weatherService = new WeatherManager();
private static final WeatherApiClient weatherApiClient = new OpenMeteoApiClient();
```

### Examiner Question: "Why didn't you declare `WeatherManager weatherService = new WeatherManager();`?"
**Your Answer:**
> *"The application uses interface-based programming to reduce coupling between the presentation layer and concrete service implementations. This supports the principles of dependency inversion, although the concrete implementations are currently instantiated inside `Main`. If we replace `WeatherManager` with a `DatabaseWeatherManager`, or replace `OpenMeteoApiClient` with a `MockApiClient`, none of the menu or dispatch logic in `Main.java` needs to change because the caller interacts only with the interface contract."*

> *Note for Viva: Programming to an interface is not automatically the same as full Dependency Injection (DI) or Inversion of Control (IoC). In this project, `Main` still instantiates concrete objects directly. A full Inversion of Control architecture (such as with Spring or a Factory) would inject those dependencies from outside `Main`. However, adopting interfaces here provides the foundational abstraction required for that architectural flexibility.*

---

## 22. Project Limitations & Future Scope

### Current Limitations
1. **Console-Only Interface**: Interaction is strictly text-based via the terminal; no graphical user interface.
2. **Single Provider**: Remote live weather retrieval depends entirely on the Open-Meteo API.
3. **Single Timeframe**: Fetches only current weather conditions, not multi-day or hourly forecasts.
4. **Local File Persistence**: Data is saved to a local JSON file (`data/weather-reports.json`) rather than a multi-user relational database.

### Realistic Future Improvements
1. **Relational Database Storage**: Replace or supplement JSON storage with SQLite or PostgreSQL via JDBC.
2. **Graphical User Interface**: Build an interactive JavaFX or Swing desktop dashboard with visual weather icons and temperature charts.
3. **Multi-Day Forecasting**: Extend `OpenMeteoApiClient` to fetch 7-day daily forecasts (`temperature_2m_max`, `temperature_2m_min`).
4. **Multiple Weather Providers**: Implement a secondary provider (such as OpenWeatherMap or WeatherAPI) using the existing `WeatherApiClient` interface as an automated fallback.

---

## 23. 32 Likely Viva Questions & Answers

### Group 1: Java Fundamentals & Architecture
#### Q1: What version of Java does this project require, and why?
**Answer:** The project requires **Java 11 or higher** (compiled and tested on JDK 27). This is because the application uses `java.net.http.HttpClient` (introduced in Java 11) and modern NIO file APIs like `Files.writeString()` and `StandardCopyOption.ATOMIC_MOVE`.

#### Q2: What is the purpose of packages in Java? Why is `package com.weather;` used?
**Answer:** Packages provide namespace management and modularity. They prevent class naming collisions and group related classes (domain models, services, storage, exceptions, and controllers) into a single logical container.

#### Q3: Why did you declare `scanner` as `private static final` in `Main`?
**Answer:** `private` encapsulates the scanner within `Main`. `static` allows all static helper methods (`readIntegerInput`, `handleAddWeather`, etc.) to share the same scanner instance without passing it as an argument. `final` ensures the scanner reference cannot be reassigned during runtime.

---

### Group 2: Object-Oriented Principles
#### Q4: How is Encapsulation demonstrated in `BaseWeatherReport` and `WeatherReport`?
**Answer:** All attributes (`cityName`, `temperature`, `humidity`, `windSpeed`, `weatherCondition`, `lastUpdated`) are declared `private`. They cannot be accessed or modified directly from outside the class. Public getters and setters provide controlled access, and setters enforce validation rules (like checking temperature boundaries and `Double.isFinite()`).

#### Q5: What is the difference between an abstract class and an interface? How are both used here?
**Answer:** 
- An **abstract class** can hold instance variables, constructors, and concrete method implementations along with abstract methods. `BaseWeatherReport` is an abstract class because it holds common state (`cityName`, `temperature`) and implements `getBasicSummary()`, while leaving `displayReport()` abstract.
- An **interface** defines a contract containing method signatures that implementing classes must fulfil. `WeatherService` and `WeatherApiClient` declare operations without dictating how data is stored or retrieved.

#### Q6: Can you instantiate an abstract class like `BaseWeatherReport`?
**Answer:** No, abstract classes cannot be directly instantiated with the `new` operator. They are meant to be extended by concrete subclasses (like `WeatherReport`) that implement all abstract methods.

#### Q7: What is the role of `super(...)` in `WeatherReport`?
**Answer:** `super(cityName, temperature, weatherCondition)` invokes the parameterized constructor of the parent class (`BaseWeatherReport`). This reuses parent constructor logic to initialize and validate inherited fields.

#### Q8: Where does Method Overriding occur in this project?
**Answer:** `WeatherReport` overrides the abstract method `displayReport()` from `BaseWeatherReport`. In addition, both `BaseWeatherReport` and `WeatherReport` override `toString()` from `java.lang.Object`.

#### Q9: Where does Method Overloading occur in this project?
**Answer:** `WeatherReport` has overloaded constructors (default, 5-parameter, and 6-parameter with timestamp). It also overloads `displayReport()` with `displayReport(boolean showDetails)`.

---

### Group 3: Collections & Data Management
#### Q10: Why did you choose `ArrayList` instead of a plain Java array?
**Answer:** Plain Java arrays have a fixed size that cannot change after creation. An `ArrayList` dynamically resizes as records are added or removed, provides convenient methods like `.add()`, `.remove()`, and `.size()`, and supports easy traversal with enhanced for-loops.

#### Q11: How does case-insensitive city search work in `WeatherManager`?
**Answer:** It iterates through the `ArrayList<WeatherReport>` and compares the stored city name with the search target using `report.getCityName().equalsIgnoreCase(searchTarget)`. This ensures that `"mumbai"`, `"MUMBAI"`, and `"Mumbai"` all match.

#### Q12: Does `Collections.unmodifiableList()` make the reports inside the list immutable?
**Answer:** No. `Collections.unmodifiableList()` provides an unmodifiable *wrapper view* of the list structure. Calling `.add()`, `.remove()`, or `.clear()` through that view throws an `UnsupportedOperationException`. However, the individual `WeatherReport` objects inside remain mutable; if an external caller obtains a report reference, it can still call setters on that object.

---

### Group 4: JSON Persistence & Crash Resilience
#### Q13: How does `WeatherStorage` prevent data loss during power outages or system crashes?
**Answer:** It uses **atomic temporary file replacement**. Instead of writing directly to `weather-reports.json`, it writes the serialized data to a temporary file (`weather-reports-*.tmp`), flushes and closes it, and then uses `Files.move(tempFile, targetFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)`. On modern filesystems, `ATOMIC_MOVE` updates the filesystem pointer instantly, so the target file is never left half-written or empty.

#### Q14: What is the Anti-Corruption Protection mechanism in `WeatherStorage`?
**Answer:** If `WeatherStorage` detects that `weather-reports.json` is corrupted (e.g. invalid JSON, non-array root, malformed fields, out-of-range temperatures, or duplicate city names), it sets an internal boolean flag `corruptionDetected = true`. When this flag is true, subsequent calls to `saveReports()` immediately return `false` without modifying disk storage. This prevents valid disk files from being overwritten or destroyed by a corrupted runtime state.

#### Q15: How are duplicate city names handled when loading JSON records?
**Answer:** During `loadReports()`, city names are normalized by trimming whitespace and converting to lower case. If a duplicate is encountered (e.g. `"Kolkata"` and `"kolkata"`), `WeatherStorage` sets `corruptionDetected = true` and logs an error. It preserves valid unique records in memory for inspection but locks saving until the user repairs the file.

#### Q16: What is the Transactional Write-Ahead consistency pattern used in `Main`?
**Answer:** For all additions, updates, and deletions, `Main` creates a proposed list and calls `weatherStorage.saveReports(proposedList)` *before* modifying in-memory data. Only if the disk save succeeds is the in-memory mutation applied to `WeatherService`. If the save fails, in-memory state remains untouched, preventing disk-memory divergence.

#### Q17: Why did you use `TypeToken` when deserializing JSON with Gson?
**Answer:** Due to Java's **Type Erasure**, generic type arguments like `List<WeatherReport>` are removed at runtime, leaving only raw `List`. `TypeToken<List<WeatherReport>>` captures the full generic type parameter at compile time via an anonymous class, enabling Gson to instantiate `WeatherReport` instances rather than generic Maps.

---

### Group 5: Exception Handling & Validation
#### Q18: What is the difference between checked and unchecked exceptions?
**Answer:** 
- **Checked exceptions** inherit from `java.lang.Exception` and are checked at compile time. The compiler enforces the **handle or declare** rule: calling code must either catch the exception using `try-catch` OR propagate it using `throws`.
- **Unchecked exceptions** inherit from `RuntimeException` and occur during execution without requiring explicit declarations.
- We used checked exceptions (`InvalidWeatherDataException` and `WeatherApiException`) because invalid input and network failures are expected operational conditions that callers must handle.

#### Q19: Why do you have two separate custom exceptions instead of just one?
**Answer:** To maintain the **Separation of Concerns**. `InvalidWeatherDataException` represents business rule violations (e.g. impossible temperatures or non-finite numbers), while `WeatherApiException` represents external infrastructure failures (e.g. network timeouts or unresolvable city names).

#### Q20: Why did you add `Double.isFinite()` to your domain setters?
**Answer:** In Java's IEEE 754 floating-point standard, `Double.NaN < -90.0` and `Double.NaN > 65.0` both evaluate to `false`. Without `Double.isFinite()`, passing `Double.NaN` bypasses standard range checks. `Double.isFinite()` guarantees that `NaN`, `+Infinity`, and `-Infinity` are rejected with `InvalidWeatherDataException`.

#### Q21: How do you prevent console crashes when a user enters letters instead of numbers?
**Answer:** In `Main.java`, `readIntegerInput()` and `readDoubleInput()` read raw text using `scanner.nextLine()` and attempt to parse it inside a `try-catch (NumberFormatException)` block. If an invalid string is entered, it displays a friendly error message and loops until valid input is received.

---

### Group 6: Networking & Open-Meteo REST API
#### Q22: What is an API, and what is a REST API?
**Answer:** An API (Application Programming Interface) allows two software systems to communicate. A REST (Representational State Transfer) API is a web-based architectural style that uses standard HTTP methods (like `GET`) and URLs to request or send data, typically in JSON format.

#### Q23: What are the authentication requirements for Open-Meteo in this project?
**Answer:** Open-Meteo allows non-commercial educational use **without requiring an API key**. This simplifies project evaluation because students do not need to register API credentials, manage secret tokens, or worry about expired trial accounts during examination.

#### Q24: What is Geocoding, and why does Open-Meteo need it?
**Answer:** Meteorological models calculate weather based on physical grid coordinates (latitude and longitude), not textual city names. The Open-Meteo Geocoding API converts the user's city name (e.g. `"London"`) into numerical coordinates (`latitude: 51.5085, longitude: -0.1257`), which are then passed to the Forecast API.

#### Q25: How does `URLEncoder` encode spaces?
**Answer:** `URLEncoder.encode(cityName, StandardCharsets.UTF_8)` uses the `application/x-www-form-urlencoded` standard, which translates space characters into `+` (plus signs). In URL query parameters, web servers decode `+` as a space.

#### Q26: Are WMO weather codes every number between 0 and 99?
**Answer:** No. WMO Code Table 4677 defines **specific assigned integer values** for defined weather conditions (e.g., 0 for Clear Sky, 1–3 for cloudy states, 45/48 for fog, 61/63/65 for rain, 71/73/75 for snow, 95 for thunderstorm). Unassigned numbers are not valid weather codes.

#### Q27: What is the difference between `IOException`, `HttpTimeoutException`, and `InterruptedException` in `OpenMeteoApiClient`?
**Answer:**
- `IOException` is a broad checked exception indicating low-level communication failure (e.g. no internet connection, DNS failure).
- `HttpTimeoutException` is a specific subclass of `IOException` thrown by `HttpClient` when a connection or request timeout expires.
- `InterruptedException` is thrown if another thread interrupts the thread that is blocked inside `httpClient.send()`. When caught, the thread's interrupt status must be restored via `Thread.currentThread().interrupt()`.

#### Q28: Why did you use `Locale.US` when formatting coordinates in `String.format`?
**Answer:** In some European locales, floating-point numbers format with commas instead of periods (e.g., `22,56` instead of `22.56`). `Locale.US` guarantees decimal periods in the URL string, preventing invalid URL syntax errors.

---

### Group 7: Testing & Verification
#### Q29: What is `TestWeatherStorage.java`? How does it test without damaging production data?
**Answer:** `TestWeatherStorage` is a standalone console test runner containing 27 programmatic tests. It creates temporary files using `Files.createTempFile("test-weather", ".json")` and guarantees cleanup inside `finally` blocks. It never touches the real production file (`data/weather-reports.json`).

#### Q30: What is `TestOpenMeteoClient.java`?
**Answer:** `TestOpenMeteoClient` is a standalone console test runner containing 6 tests that exercise the live Open-Meteo API client against valid queries, invalid queries, blank inputs, and WMO code translations.

#### Q31: How does your code prevent duplicate entries during manual or live entry?
**Answer:** `Main` checks `weatherService.cityExists(cityName)` before fetching or adding a report. In addition, `WeatherManager.addWeatherReport()` checks `cityExists()`, rejecting duplicates and advising the user to use the update option instead.

#### Q32: What happens if the user tries to delete a report when the JSON file is corrupted?
**Answer:** In Option 7, `Main` checks `weatherStorage.isCorruptionDetected()` before prompting the user. If corruption is detected, the deletion operation is immediately refused, warning the user that deleting or saving would risk overwriting damaged data, and instructing them to inspect `data/weather-reports.json`.
