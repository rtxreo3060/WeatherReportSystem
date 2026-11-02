# Weather Report System: Viva & Oral Examination Guide

This guide is designed for undergraduate engineering students preparing to present the **Weather Report System** during a college lab viva, project defense, or oral examination. It explains every design choice, OOP concept, algorithm, and class in plain, beginner-friendly English with references to the actual source code.

---

## Table of Contents
1. [Project Summary in 60 Seconds](#1-project-summary-in-60-seconds)
2. [Why Java Was Chosen](#2-why-java-was-chosen)
3. [Deep-Dive into Every Project Class](#3-deep-dive-into-every-project-class)
4. [How the Classes Communicate (Control Flow)](#4-how-the-classes-communicate-control-flow)
5. [Core OOP Concepts Demonstrated (With Actual Code Snippets)](#5-core-oop-concepts-demonstrated-with-actual-code-snippets)
6. [Collections Framework & `Collections.unmodifiableList()`](#6-collections-framework--collectionsunmodifiablelist)
7. [The `WeatherService` Interface & Interface-Based Design](#7-the-weatherservice-interface--interface-based-design)
8. [The `WeatherApiClient` Interface & Network Decoupling](#8-the-weatherapiclient-interface--network-decoupling)
9. [Inheritance Hierarchy: `BaseWeatherReport` & `WeatherReport`](#9-inheritance-hierarchy-baseweatherreport--weatherreport)
10. [Polymorphism in Action: Overriding vs. Overloading](#10-polymorphism-in-action-overriding-vs-overloading)
11. [Exception Handling Architecture & Checked Exceptions](#11-exception-handling-architecture--checked-exceptions)
12. [How Java `HttpClient` Works (Synchronous Execution)](#12-how-java-httpclient-works-synchronous-execution)
13. [JSON Deserialization with Google Gson](#13-json-deserialization-with-google-gson)
14. [The Open-Meteo REST API Workflow](#14-the-open-meteo-rest-api-workflow)
15. [Temperature Conversion Mathematics](#15-temperature-conversion-mathematics)
16. [Two-Tier Input Validation Strategy](#16-two-tier-input-validation-strategy)
17. [Why Program to an Interface, Not an Implementation?](#17-why-program-to-an-interface-not-an-implementation)
18. [Project Limitations & Future Scope](#18-project-limitations--future-scope)
19. [28 Likely Viva Questions & Answers](#19-28-likely-viva-questions--answers)

---

## 1. Project Summary in 60 Seconds

> **How to describe your project to the examiner:**
> 
> *"Our project is a console-based **Weather Report System** written in Java. It allows users to manage weather records across different cities. The application features seven interactive menu options, supporting both manual record entry and real-time live weather retrieval from the Open-Meteo API using Java's built-in `HttpClient` and Google `Gson` without requiring an API key. Users can display full weather report cards, view a side-by-side comparison table of key metrics, search for cities case-insensitively, and update existing records. If an API request fails, an automatic manual fallback allows the user to record metrics without retyping the city name. The project demonstrates core Object-Oriented Programming principles including Encapsulation, Inheritance, Abstraction, Polymorphism, and custom checked Exception handling."*

---

## 2. Why Java Was Chosen

1. **Object-Oriented Architecture**: Java natively supports class-based OOP, making it ideal for demonstrating inheritance, encapsulation, and interfaces.
2. **Standard HTTP Client (`java.net.http.HttpClient`)**: Since Java 11, Java includes a modern, native HTTP client that executes synchronous requests with configurable timeouts and redirection handling, eliminating external HTTP libraries.
3. **Platform Independence ("Write Once, Run Anywhere")**: The compiled bytecode runs on any operating system with a standard JRE.
4. **Strong Static Typing**: Catches type mismatches at compile time rather than during runtime evaluation.
5. **Robust Exception Handling**: Java differentiates between checked and unchecked exceptions, enforcing the 'handle or declare' rule for network failures and domain validation.

---

## 3. Deep-Dive into Every Project Class

The project is organized in the `com.weather` package across 10 source files:

| File Name | Class / Interface Type | Role and Purpose |
| :--- | :--- | :--- |
| **`BaseWeatherReport.java`** | `public abstract class` | Foundational domain entity. Encapsulates common attributes: `cityName`, `temperature` (°C), and `weatherCondition`. Declares the abstract method `displayReport()` and overrides `toString()`. |
| **`WeatherReport.java`** | `public class extends BaseWeatherReport` | Concrete domain model. Inherits base fields and adds `humidity`, `windSpeed`, and `lastUpdated`. Overrides `displayReport()`, overloads `displayReport(boolean)`, implements temperature conversion, and overrides `toString()`. |
| **`WeatherService.java`** | `public interface` | Service layer abstraction defining the contract for managing stored weather reports (add, retrieve, search, update, check duplicates). |
| **`WeatherManager.java`** | `public class implements WeatherService` | Service implementation. Stores and filters reports in memory using an encapsulated `ArrayList<WeatherReport>`. |
| **`WeatherApiClient.java`** | `public interface` | Remote service abstraction defining the contract for fetching live weather data from an external provider. |
| **`OpenMeteoApiClient.java`** | `public class implements WeatherApiClient` | API implementation. Communicates synchronously with Open-Meteo Geocoding and Forecast endpoints, deserializes JSON with Gson, and maps WMO codes. |
| **`InvalidWeatherDataException.java`** | `public class extends Exception` | Custom checked exception for physical domain violations (e.g. temperature out of Earth range, negative wind speed). |
| **`WeatherApiException.java`** | `public class extends Exception` | Custom checked exception for network errors, timeouts, HTTP non-200 responses, and unresolvable city queries. |
| **`Main.java`** | `public class` | Presentation controller. Drives the 7-option console menu, handles user input sanitization, provides manual fallback, and delegates tasks to services. |
| **`TestOpenMeteoClient.java`** | `public class` | Standalone test runner with a `main` method that executes programmatic verification against the API client without running the interactive menu. |

---

## 4. How the Classes Communicate (Control Flow)

The application follows a 3-tier architecture: **Presentation $\rightarrow$ Service / API $\rightarrow$ Domain Model**.

```
[User at Console]
       │
       ▼
    Main.java  (Presentation Controller)
       │
       ├──► Calls WeatherService (WeatherManager.java) ──► Manages ArrayList<WeatherReport>
       │
       └──► Calls WeatherApiClient (OpenMeteoApiClient.java)
                 │
                 ├── Sends Synchronous HTTP GET ──► Open-Meteo Geocoding API
                 ├── Sends Synchronous HTTP GET ──► Open-Meteo Forecast API
                 ├── Parses JSON ───────────────► Google Gson
                 └── Returns ───────────────────► WeatherReport (Domain Object)
                                                       │
                                                       ▼
                                            Passed to WeatherManager for Storage
```

1. `Main` interacts **only** with the interfaces `WeatherService` and `WeatherApiClient`.
2. When the user selects Option 2, `Main` calls `weatherApiClient.fetchCurrentWeather("Tokyo")`.
3. `OpenMeteoApiClient` contacts the web service synchronously, parses the response, constructs a `WeatherReport` object, and returns it to `Main`.
4. `Main` displays the report and passes that exact same `WeatherReport` object to `weatherService.addWeatherReport(report)`.
5. `WeatherManager` stores it in its internal `ArrayList`.

---

## 5. Core OOP Concepts Demonstrated (With Actual Code Snippets)

### A. Encapsulation (Data Hiding & Validation)
**What it is:** Keeping fields `private` and exposing them only through public getters and validated setters.

**Actual Code in `BaseWeatherReport.java` (lines 11–14, 56–64):**
```java
// Encapsulation: Private data fields
private String cityName;
private double temperature;       // In Celsius
private String weatherCondition;  // e.g., Sunny, Cloudy, Rainy, Stormy

public void setTemperature(double temperature) throws InvalidWeatherDataException {
    // Absolute Earth surface temperature limits: approximately -90°C to +60°C
    if (temperature < -90.0 || temperature > 65.0) {
        throw new InvalidWeatherDataException(
            "Temperature " + temperature + "°C is out of realistic earthly range (-90°C to 65°C)."
        );
    }
    this.temperature = temperature;
}
```
**Viva Explanation:** *"By making `temperature` private, no outside class can set an invalid temperature like $500^\circ\text{C}$. Any change must pass through `setTemperature()`, which validates the input."*

---

### B. Inheritance (Code Reuse & Specialization)
**What it is:** A subclass inherits properties and methods from a superclass using the `extends` keyword.

**Actual Code in `WeatherReport.java` (lines 15, 32–38):**
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
**Viva Explanation:** *"Instead of re-declaring `cityName`, `temperature`, and `weatherCondition` in `WeatherReport`, we inherit them from `BaseWeatherReport`. `super(...)` passes the shared fields up to the parent constructor for initialization."*

---

### C. Abstraction (Hiding Implementation Complexity)
**What it is:** Exposing what an object does while hiding how it does it.

1. **Abstract Class (`BaseWeatherReport.java`, line 32)**:
   ```java
   public abstract class BaseWeatherReport {
       // Forces subclasses to provide their own display logic
       public abstract void displayReport();
   }
   ```
2. **Interface Abstraction (`WeatherApiClient.java`, line 19)**:
   ```java
   public interface WeatherApiClient {
       WeatherReport fetchCurrentWeather(String cityName) 
               throws WeatherApiException, InvalidWeatherDataException;
   }
   ```
**Viva Explanation:** *"`Main` does not know whether weather data comes from Open-Meteo, another API, or a mock service. It simply calls `fetchCurrentWeather()` on the `WeatherApiClient` interface."*

---

### D. Polymorphism (Many Forms)
1. **Method Overriding (Runtime Polymorphism)**:
   In `BaseWeatherReport.java` (line 32):
   ```java
   public abstract void displayReport();
   ```
   In `WeatherReport.java` (lines 95–98):
   ```java
   @Override
   public void displayReport() {
       displayReport(true); // delegates to overloaded method
   }
   ```
   Also verified in `toString()` overriding:
   - `BaseWeatherReport.java` (lines 77–80):
     ```java
     @Override
     public String toString() {
         return "City: " + cityName + ", Temperature: " + temperature + "°C, Condition: " + weatherCondition;
     }
     ```
   - `WeatherReport.java` (lines 114–120):
     ```java
     @Override
     public String toString() {
         return String.format(
             "WeatherReport [City=%s, Temp=%.1f°C, Humidity=%.1f%%, Wind=%.1f km/h, Condition=%s, Updated=%s]",
             getCityName(), getTemperature(), humidity, windSpeed, getWeatherCondition(), lastUpdated
         );
     }
     ```
2. **Method Overloading (Compile-Time Polymorphism)**:
   In `WeatherReport.java`:
   ```java
   // Signature 1 (line 96): Takes no parameters
   public void displayReport() { displayReport(true); }

   // Signature 2 (line 101): Takes a boolean flag
   public void displayReport(boolean showDetails) { ... }
   ```
   Also demonstrated in overloaded constructors in `WeatherReport.java` (lines 24, 32, 41).

---

## 6. Collections Framework & `Collections.unmodifiableList()`

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
2. **Utility Methods**: Provides convenient methods like `.add()`, `.size()`, `.isEmpty()`, and clean iteration.

### Crucial Distinction: What `Collections.unmodifiableList()` Actually Does
`Collections.unmodifiableList(weatherReports)` provides an **unmodifiable wrapper view** of the collection:
- **Structural Modifications are Blocked**: Any attempt to modify the list structure through the returned reference (such as calling `.add()`, `.remove()`, or `.clear()`) throws an `UnsupportedOperationException`.
- **Objects Inside are NOT Immutable**: This is a shallow wrapper, not a deep copy. The underlying `WeatherReport` objects inside the list can still have their state altered if an external caller obtains a report reference and calls its setters (`setTemperature()`, etc.).

---

## 7. The `WeatherService` Interface & Interface-Based Design

```java
public interface WeatherService {
    boolean addWeatherReport(WeatherReport report) throws InvalidWeatherDataException;
    List<WeatherReport> getAllReports();
    WeatherReport searchCity(String cityName);
    boolean updateWeatherReport(String cityName, double temp, double hum, double wind, String cond) 
        throws InvalidWeatherDataException;
    boolean cityExists(String cityName);
    void displayAllReports();
    void displayKeyMetrics();
    int getTotalRecordCount();
}
```

### Why use an interface here?
- Decouples the presentation layer from the data management layer.
- If we replace `ArrayList` with an SQL database (e.g., SQLite or MySQL), we can create `DatabaseWeatherService implements WeatherService` without changing any menu logic in `Main.java`.

---

## 8. The `WeatherApiClient` Interface & Network Decoupling

```java
public interface WeatherApiClient {
    WeatherReport fetchCurrentWeather(String cityName) 
            throws WeatherApiException, InvalidWeatherDataException;
}
```

### Why separate `WeatherApiClient` from `WeatherService`?
- **Single Responsibility Principle (SRP)**: `WeatherService` manages and stores records. `WeatherApiClient` communicates over the internet to fetch data.
- **Pluggability**: We can substitute `OpenMeteoApiClient` with a mock client or another provider without altering storage or UI code.

---

## 9. Inheritance Hierarchy: `BaseWeatherReport` & `WeatherReport`

```
       +---------------------------------------------+
       |             BaseWeatherReport               |
       |  (Abstract: cityName, temp, condition)      |
       +---------------------------------------------+
                              ^
                              |  extends (inherits)
       +---------------------------------------------+
       |                WeatherReport                |
       |  (Concrete: humidity, windSpeed, timestamp, |
       |   Fahrenheit conversion, displayReport())   |
       +---------------------------------------------+
```

### What does `WeatherReport` inherit?
- Fields: `cityName`, `temperature`, `weatherCondition`.
- Methods: `getCityName()`, `setCityName()`, `getTemperature()`, `setTemperature()`, `getWeatherCondition()`, `setWeatherCondition()`, `getBasicSummary()`.
- Constructor behavior via `super(cityName, temperature, weatherCondition)`.

---

## 10. Polymorphism in Action: Overriding vs. Overloading

| Characteristic | Method Overloading (Compile-Time) | Method Overriding (Runtime) |
| :--- | :--- | :--- |
| **Where in project** | `WeatherReport.displayReport()` vs `displayReport(boolean)` | `WeatherReport.displayReport()` overriding `BaseWeatherReport.displayReport()` |
| **Parameters** | Must have different parameter lists | Must have the exact same signature |
| **Return type** | Can be the same or different | Must be the same (or covariant) |
| **Resolution time** | Resolved at compile time by compiler | Resolved at runtime using Dynamic Method Dispatch |
| **Annotation** | None | `@Override` |

---

## 11. Exception Handling Architecture & Checked Exceptions

```
                  java.lang.Throwable
                           │
                  java.lang.Exception (Checked)
                   ├── InvalidWeatherDataException
                   └── WeatherApiException
```

### The "Handle or Declare" Rule
Because both exceptions extend `java.lang.Exception`, they are **checked exceptions**. The Java compiler strictly enforces the **"handle or declare"** rule:
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
1. **`InvalidWeatherDataException` (Domain Validation)**: Thrown when data violates physical limits on Earth (e.g. temperature $< -90^\circ\text{C}$ or $> 65^\circ\text{C}$, relative humidity $< 0\%$ or $> 100\%$, negative wind speed, empty city name).
2. **`WeatherApiException` (Network & API Infrastructure)**: Thrown when external network communication fails (e.g. timeout, connection error, non-200 HTTP response, unresolvable city, or malformed JSON).

### Distinguishing Network Exceptions:
- **`java.io.IOException`**: General checked exception indicating network connection drops, unreachable hosts, or DNS failures.
- **`java.net.http.HttpTimeoutException`**: A specific subclass of `IOException` thrown by `HttpClient` when a configured connection or request timeout expires before the response is received.
- **`java.lang.InterruptedException`**: Thrown when another thread interrupts the thread that is blocked waiting in `httpClient.send()`. When caught, thread interruption status must be restored via `Thread.currentThread().interrupt()`.

---

## 12. How Java `HttpClient` Works (Synchronous Execution)

In `OpenMeteoApiClient.java` (lines 48–55, 150–186):
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

1. **Synchronous Execution**: The call to `httpClient.send()` is **blocking and synchronous**. The calling thread pauses and waits until the full response is received, a timeout expires, or an exception is thrown. (While `HttpClient` also supports asynchronous execution via `sendAsync()`, this project intentionally uses synchronous `send()` to maintain straightforward, sequential control flow suitable for a console application).
2. **`HttpRequest`**: Specifies the URI, sets the HTTP method to `GET`, specifies headers, and attaches a 10-second timeout.
3. **`HttpResponse<String>`**: `BodyHandlers.ofString()` converts incoming byte streams into a Java `String`.
4. **Status Code Check**: `if (response.statusCode() != 200)` throws `WeatherApiException`.

---

## 13. JSON Deserialization with Google Gson

### What is JSON?
JSON (JavaScript Object Notation) is a text format for transferring key-value structured data over HTTP.

Example Open-Meteo payload:
```json
{
  "current": {
    "temperature_2m": 26.2,
    "relative_humidity_2m": 90.0,
    "wind_speed_10m": 6.4,
    "weather_code": 61
  }
}
```

### How Gson Works in Our Code:
We define matching static inner Data Transfer Objects (DTOs):
```java
private static class ForecastApiResponse {
    CurrentWeather current;
}

static class CurrentWeather {
    Double temperature_2m;
    Double relative_humidity_2m;
    Double wind_speed_10m;
    Integer weather_code;
}
```
Gson maps JSON fields to Java variables automatically:
```java
Gson gson = new Gson();
ForecastApiResponse response = gson.fromJson(jsonResponse, ForecastApiResponse.class);
```
**Viva Explanation:** *"Gson uses Java Reflection to inspect the field names of our DTO class and matches them with keys in the JSON text, converting numbers and strings into strongly-typed Java objects."*

---

## 14. The Open-Meteo REST API Workflow

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
`URLEncoder.encode(cityName, StandardCharsets.UTF_8)` uses the `application/x-www-form-urlencoded` standard, which encodes spaces as `+` (plus signs). In URL query parameters, web servers decode `+` as a space.

### WMO Weather Interpretation Codes
Open-Meteo reports conditions using specific assigned integer values defined in WMO Code Table 4677 (not every integer between 0 and 99 represents a code). `mapWmoCodeToCondition(int code)` maps these specific values to descriptive text.

---

## 15. Temperature Conversion Mathematics

Stored temperatures are maintained in **Celsius (°C)**. Fahrenheit is calculated dynamically:

$$\text{°F} = \left(\text{°C} \times \frac{9}{5}\right) + 32$$

In `WeatherReport.java` (lines 90–92):
```java
public double getTemperatureInFahrenheit() {
    return (getTemperature() * 9.0 / 5.0) + 32.0;
}
```
*Note: We write `9.0 / 5.0` instead of `9 / 5`. In Java, integer division `9 / 5` evaluates to `1` due to truncation! Using floating-point literals `9.0 / 5.0` prevents integer division and preserves the fractional part of the conversion.*

---

## 16. Two-Tier Input Validation Strategy

The project employs two distinct validation layers:

1. **Syntactic & Type Validation (`Main.java`)**:
   - `readNonEmptyString()`: Loops until the user enters non-whitespace text.
   - `readIntegerInput()`: Catches `NumberFormatException` so typing letters (like `"abc"`) when asked for a menu choice does not crash the program.
   - `readDoubleInput()`: Ensures numeric inputs stay within prompted menu boundaries.
2. **Semantic & Physical Validation (`BaseWeatherReport.java` & `WeatherReport.java`)**:
   - Validates that values make physical sense on Earth.
   - Even if someone bypassed the console UI, the constructor and setters in the domain models will throw `InvalidWeatherDataException`.

---

## 17. Why Program to an Interface, Not an Implementation?

In `Main.java` (lines 16–17):
```java
private static final WeatherService weatherService = new WeatherManager();
private static final WeatherApiClient weatherApiClient = new OpenMeteoApiClient();
```

### Examiner Question: "Why didn't you declare `WeatherManager weatherService = new WeatherManager();`?"
**Your Answer:**
> *"The application uses interface-based programming to reduce coupling between the presentation layer and concrete service implementations. This supports the principles of dependency inversion, although the concrete implementations are currently instantiated inside `Main`. If we replace `WeatherManager` with a `DatabaseWeatherManager`, or replace `OpenMeteoApiClient` with a `MockApiClient`, none of the menu or dispatch logic in `Main.java` needs to change because the caller interacts only with the interface contract."*

> *Note for Viva: Programming to an interface is not automatically the same as full Dependency Injection (DI) or Inversion of Control (IoC). In this project, `Main` still instantiates the concrete objects directly using `new WeatherManager()` and `new OpenMeteoApiClient()`. A complete Inversion of Control architecture (such as with a framework or factory) would inject those dependencies from outside `Main`. However, adopting interfaces here provides the foundational abstraction required for that flexibility.*

---

## 18. Project Limitations & Future Scope

### Current Limitations
1. **In-Memory Storage**: Records are kept in RAM (`ArrayList`). When the application exits, records are cleared.
2. **Internet Dependency**: Option 2 requires an active network connection to contact Open-Meteo.
3. **Single Timeframe**: Only retrieves current weather, not 7-day or hourly forecasts.

### Realistic Future Improvements
1. **Persistence**: Save records to a local JSON file, CSV file, or SQLite database.
2. **Multi-Day Forecasts**: Expand `OpenMeteoApiClient` to fetch `daily=temperature_2m_max,temperature_2m_min`.
3. **Graphical User Interface**: Build a JavaFX or Swing UI with interactive weather icons and charts.

---

## 19. 28 Likely Viva Questions & Answers

### Group 1: Java Fundamentals & Architecture
#### Q1: What version of Java does this project require, and why?
**Answer:** The project requires **Java 11 or higher** (tested and compiled on JDK 27). This is because the application uses the modern `java.net.http.HttpClient` package, which was officially introduced in Java 11 to replace the older `HttpURLConnection`.

#### Q2: What is the purpose of packages in Java? Why is `package com.weather;` used?
**Answer:** Packages provide namespace management and modularity. They prevent naming collisions and group related classes (domain models, services, exceptions, and controllers) into a single logical container.

#### Q3: Why did you declare `scanner` as `private static final` in `Main`?
**Answer:** `private` encapsulates the scanner within `Main`. `static` allows all static helper methods (`readIntegerInput`, `handleAddWeather`, etc.) to share the same scanner instance. `final` ensures the scanner reference cannot be reassigned during runtime.

---

### Group 2: Object-Oriented Principles
#### Q4: How is Encapsulation demonstrated in `BaseWeatherReport`?
**Answer:** All attributes (`cityName`, `temperature`, `weatherCondition`) are declared `private`. They cannot be accessed or modified directly from outside the class. Public getters and setters provide controlled access, and setters enforce validation rules (like checking temperature boundaries).

#### Q5: What is the difference between an abstract class and an interface? How are both used here?
**Answer:** 
- An **abstract class** can have instance variables, constructors, and concrete method implementations along with abstract methods. `BaseWeatherReport` is an abstract class because it holds common state (`cityName`, `temperature`) and implements `getBasicSummary()`, while leaving `displayReport()` abstract.
- In this project, the interfaces define method contracts that implementing classes must fulfil. Java interfaces can also contain default, static, and private methods. `WeatherService` and `WeatherApiClient` declare service operations without dictating how data is stored or retrieved.

#### Q6: Can you instantiate an abstract class like `BaseWeatherReport`?
**Answer:** No, abstract classes cannot be directly instantiated with the `new` operator. They are meant to be extended by concrete subclasses (like `WeatherReport`) that implement all abstract methods.

#### Q7: What is the role of `super(...)` in `WeatherReport`?
**Answer:** `super(cityName, temperature, weatherCondition)` invokes the parameterized constructor of the parent class (`BaseWeatherReport`). This reuses parent constructor logic to initialize and validate the inherited fields.

#### Q8: Where does Method Overriding occur in this project?
**Answer:** `WeatherReport` overrides the abstract method `displayReport()` from `BaseWeatherReport`. In addition, both `BaseWeatherReport` (lines 77–80) and `WeatherReport` (lines 114–120) override `toString()` from `java.lang.Object`.

#### Q9: Where does Method Overloading occur in this project?
**Answer:** `WeatherReport` has overloaded constructors (default constructor, 5-parameter constructor, and 6-parameter constructor with timestamp). It also overloads `displayReport()` with `displayReport(boolean showDetails)`.

---

### Group 3: Collections & Data Management
#### Q10: Why did you choose `ArrayList` instead of a plain Java array?
**Answer:** Plain Java arrays have a fixed size that cannot change after creation. An `ArrayList` dynamically resizes as records are added or removed, provides convenient methods like `.add()` and `.size()`, and supports easy traversal with enhanced for-loops.

#### Q11: How does case-insensitive city search work in `WeatherManager`?
**Answer:** It iterates through the `ArrayList<WeatherReport>` and compares the stored city name with the search target using `report.getCityName().equalsIgnoreCase(searchTarget)`. This ensures that `"mumbai"`, `"MUMBAI"`, and `"Mumbai"` all match.

#### Q12: Does `Collections.unmodifiableList()` make the reports inside the list immutable?
**Answer:** No. `Collections.unmodifiableList()` provides an unmodifiable *wrapper view* of the list structure. Calling `.add()`, `.remove()`, or `.clear()` through that view throws an `UnsupportedOperationException`. However, the individual `WeatherReport` objects inside remain mutable; if an external caller obtains a report reference, it can still call setters on that object.

---

### Group 4: Exception Handling
#### Q13: What is the difference between checked and unchecked exceptions? How are checked exceptions handled?
**Answer:** 
- **Checked exceptions** inherit from `java.lang.Exception` and are checked at compile time. The compiler enforces the **handle or declare** rule: calling code must either catch the exception using `try-catch` OR propagate it using the `throws` keyword.
- **Unchecked exceptions** inherit from `RuntimeException` and occur during execution without requiring explicit `throws` clauses.
- We used checked exceptions (`InvalidWeatherDataException` and `WeatherApiException`) because invalid data and network failures are expected operational conditions that callers must handle.

#### Q14: Why do you have two separate custom exceptions instead of just one?
**Answer:** To maintain the **Separation of Concerns**. `InvalidWeatherDataException` represents business rule violations (e.g. impossible temperatures), while `WeatherApiException` represents external infrastructure failures (e.g. timeout, connection loss, or unresolvable city names).

#### Q15: How do you prevent the console application from crashing when a user types letters instead of numbers?
**Answer:** In `Main.java`, `readIntegerInput()` and `readDoubleInput()` read raw text using `scanner.nextLine()` and attempt to parse it inside a `try-catch (NumberFormatException)` block. If an invalid string is entered, it displays an error message and re-prompts the user instead of terminating.

---

### Group 5: Networking & Open-Meteo REST API
#### Q16: What is an API, and what is a REST API?
**Answer:** An API (Application Programming Interface) allows two software systems to communicate. A REST (Representational State Transfer) API is a web-based architectural style that uses standard HTTP methods (like `GET`) and URLs to request or send data, typically in JSON format.

#### Q17: What are the authentication requirements for Open-Meteo in this project?
**Answer:** Open-Meteo allows non-commercial educational use **without requiring an API key**. This simplifies project evaluation because students do not need to register API credentials, manage secret tokens, or worry about expired trial accounts during examination.

#### Q18: What is Geocoding, and why does Open-Meteo need it?
**Answer:** Meteorological models calculate weather based on physical grid coordinates (latitude and longitude), not textual city names. The Open-Meteo Geocoding API converts the user's city name (e.g. `"London"`) into numerical coordinates (`latitude: 51.5085, longitude: -0.1257`), which are then passed to the Forecast API.

#### Q19: How does `URLEncoder` encode spaces?
**Answer:** `URLEncoder.encode(cityName, StandardCharsets.UTF_8)` uses the `application/x-www-form-urlencoded` standard, which translates space characters into `+` (plus signs). In URL query parameters, web servers decode `+` as a space.

#### Q20: Are WMO weather codes every number between 0 and 99?
**Answer:** No. WMO Code Table 4677 defines **specific assigned integer values** for defined weather conditions (e.g., 0 for Clear sky, 1–3 for cloudy states, 45/48 for fog, 61/63/65 for rain, 71/73/75 for snow, 95 for thunderstorm). Unassigned numbers are not valid weather codes.

#### Q21: What is the difference between `IOException`, `HttpTimeoutException`, and `InterruptedException` in `OpenMeteoApiClient`?
**Answer:**
- `IOException` is a broad checked exception indicating low-level communication failure (e.g. no internet connection, DNS failure).
- `HttpTimeoutException` is a specific subclass of `IOException` thrown by `HttpClient` when a connection or request timeout expires.
- `InterruptedException` is thrown if another thread interrupts the thread that is blocked inside `httpClient.send()`. When caught, the thread's interrupt status must be restored via `Thread.currentThread().interrupt()`.

#### Q22: Why did you use `Locale.US` when formatting coordinates in `String.format`?
**Answer:** In some European locales, floating-point numbers format with commas instead of periods (e.g., `22,56` instead of `22.56`). `Locale.US` guarantees decimal periods in the URL string, preventing invalid URL syntax errors.

---

### Group 6: JSON Parsing & Google Gson
#### Q23: What is Gson, and why did you choose it over manual string parsing?
**Answer:** Google Gson is a lightweight JSON library for Java. It automatically deserializes JSON strings into strongly-typed Java objects (DTOs) through reflection. Writing custom string-splitting or regex logic is brittle and breaks easily if whitespace or field order changes.

#### Q24: What is a DTO (Data Transfer Object)?
**Answer:** A DTO is a simple Java class containing only fields, used exclusively to transport data between processes (in our case, from the raw JSON payload into memory). In `OpenMeteoApiClient`, `GeocodingApiResponse` and `ForecastApiResponse` are private static DTOs.

#### Q25: How did you configure Gson without Maven or Gradle?
**Answer:** We placed `gson-2.10.1.jar` inside a project-level `lib/` directory and referenced it using a relative module path (`$MODULE_DIR$/lib/gson-2.10.1.jar!/`) inside `WeatherReportSystem.iml`. For command-line execution, we supply it via the `-cp` (classpath) argument.

---

### Group 7: Code Quality & Testing
#### Q26: What is `TestOpenMeteoClient.java`? Is it a formal JUnit test suite?
**Answer:** `TestOpenMeteoClient` is a **standalone console test runner**, not a formal JUnit test suite. It contains a standard `public static void main` method that executes programmatic verification against valid cities (Kolkata, London), non-existent cities (AtlantisXYZ), empty inputs, and WMO code mappings, reporting pass/fail counts to the console.

#### Q27: How does your code prevent duplicate entries?
**Answer:** Before adding or fetching a report, `Main` calls `weatherService.cityExists(cityName)`. In addition, `WeatherManager.addWeatherReport()` checks if `cityExists()` is true. If a duplicate is detected, it cancels the addition and advises the user to update the existing record instead.

#### Q28: If you had more time, what is the single most important feature you would add?
**Answer:** File or database persistence. Currently, all weather reports are stored in memory (`ArrayList`) and are cleared when the program terminates. Storing them in a local JSON file or SQLite database would ensure data survives application restarts.
