# Weather Report System

A functional, modular, and object-oriented Java console application designed for undergraduate engineering coursework. This application enables users to manage, retrieve, analyze, and display meteorological data across multiple cities while adhering to core Object-Oriented Programming (OOP) principles.

---

## 1. Project Overview & Objectives

### Overview
The **Weather Report System** provides a structured way to record, search, modify, and monitor key atmospheric parameters:
- **Temperature** (in °C and converted to °F)
- **Relative Humidity** (0.0% to 100.0%)
- **Wind Speed** (in km/h, non-negative)
- **Atmospheric Condition** (e.g., Sunny, Cloudy, Rainy, Stormy, Foggy)
- **Timestamp** of the last data capture

### Objectives
1. **Academic Demonstration of OOP**: Illustrate Encapsulation, Inheritance, Polymorphism, Abstraction, and Custom Exception Handling in a clean, real-world context.
2. **Robust Input Validation**: Ensure bad data (empty city names, out-of-range temperatures, negative wind speeds, or negative/exceeding humidity) is caught gracefully without program crashes.
3. **Extensibility**: Design with interfaces (`WeatherService`) so that a real-time REST API client can replace the local in-memory storage manager in the future without modifying user interface code.

---

## 2. System Architecture & Design

The application follows a clean 3-tier modular architecture:
- **Presentation Layer (`Main`)**: Menu rendering, console interaction, user input sanitization, and graceful error feedback.
- **Service Layer (`WeatherService`, `WeatherManager`)**: Business rules, duplicate validation, search filters, and collection management.
- **Domain/Model Layer (`BaseWeatherReport`, `WeatherReport`)**: Data representations, property encapsulation, validation logic, and formatted output generation.
- **Exception Layer (`InvalidWeatherDataException`)**: Custom checked exceptions for validation failures.

```
       +---------------------------------------------+
       |             Main (Console UI)               |
       +---------------------------------------------+
                              |
                              v  (calls interface)
       +---------------------------------------------+
       |         <<interface>> WeatherService        |
       +---------------------------------------------+
                              ^
                              |  (implements)
       +---------------------------------------------+
       |               WeatherManager                |
       |     (ArrayList<WeatherReport> storage)      |
       +---------------------------------------------+
                              |
                              v  (manages)
       +---------------------------------------------+
       |             BaseWeatherReport               |
       |   (Abstract Class: City, Temp, Condition)   |
       +---------------------------------------------+
                              ^
                              |  (extends / inherits)
       +---------------------------------------------+
       |                WeatherReport                |
       |  (Humidity, Wind Speed, Timestamp, Overrides|
       +---------------------------------------------+
```

---

## 3. OOP Concepts Mapping

| OOP Concept | Where It Is Used | Concrete Explanation |
| :--- | :--- | :--- |
| **Classes & Objects** | All files | `WeatherReport` and `WeatherManager` are instantiated as objects to manage real data. |
| **Encapsulation** | `BaseWeatherReport`, `WeatherReport`, `WeatherManager` | All fields (`cityName`, `temperature`, `humidity`, `windSpeed`, `weatherReports`) are `private`. Access is granted exclusively through validated getters and setters. |
| **Inheritance** | `WeatherReport extends BaseWeatherReport` | `BaseWeatherReport` defines shared attributes (`cityName`, `temperature`, `condition`). `WeatherReport` reuses these and adds domain-specific metrics (`humidity`, `windSpeed`). |
| **Abstraction** | `BaseWeatherReport`, `WeatherService` | `BaseWeatherReport` is an abstract class with the abstract method `displayReport()`. `WeatherService` is an interface declaring the service contract without tying it to storage mechanism. |
| **Polymorphism (Overriding)** | `WeatherReport.java` | Overrides `displayReport()` from `BaseWeatherReport` and `toString()` from `java.lang.Object`. `WeatherManager` overrides interface methods. |
| **Polymorphism (Overloading)**| `WeatherReport.java` | Overloaded method `displayReport(boolean showDetails)` and overloaded constructors provide compile-time polymorphism. |
| **Collections Framework** | `WeatherManager.java` | Employs `ArrayList<WeatherReport>` to dynamically store, traverse, and filter weather objects. |
| **Exception Handling** | `InvalidWeatherDataException.java`, `Main.java` | Custom checked exception thrown when physical metric constraints are breached; caught in UI layer to provide user-friendly warnings. |

---

## 4. Project Directory Structure

```text
WeatherReportSystem/
├── src/
│   └── com/
│       └── weather/
│           ├── BaseWeatherReport.java           # Abstract base entity
│           ├── InvalidWeatherDataException.java # Custom checked exception
│           ├── Main.java                        # Menu-driven console UI
│           ├── WeatherManager.java              # Collection-based service implementation
│           ├── WeatherReport.java               # Concrete report model
│           └── WeatherService.java              # Service abstraction interface
├── bin/                                         # Compiled bytecode (.class files)
└── README.md                                    # Project documentation
```

---

## 5. How to Build and Run

### Option A: From Command Line (Windows / Linux / macOS)

1. Open a terminal / command prompt and navigate to the project directory:
   ```bash
   cd WeatherReportSystem
   ```
2. Compile all source files into the `bin/` directory:
   ```bash
   javac -d bin src/com/weather/*.java
   ```
3. Run the application:
   ```bash
   java -cp bin com.weather.Main
   ```

### Option B: Using IntelliJ IDEA

1. Launch IntelliJ IDEA and select **Open** (or **File > Open**).
2. Browse to and select the `WeatherReportSystem` folder.
3. If IntelliJ does not automatically mark `src` as the source root:
   - Right-click the `src` folder in the Project view.
   - Select **Mark Directory as > Sources Root**.
4. Navigate to `src/com/weather/Main.java`.
5. Click the green **Run** triangle icon next to `public static void main(String[] args)`.

---

## 6. Functional Verification

The application fully satisfies all five core case study requirements:

1. **Add Weather Information for a City**: Prompts for city, temperature, humidity, wind speed, and condition. Disallows duplicate city names (case-insensitive check).
2. **Display Weather Reports for All Cities**: Shows full report cards for every stored city, including time of entry, temperature in Celsius and Fahrenheit, humidity, wind, and conditions.
3. **Search Weather by City Name**: Finds records using case-insensitive comparison (`"kolkata"`, `"KOLKATA"`, `"Kolkata"` all match). Displays report if found or clear message if not.
4. **Update Existing Weather Information**: Modifies temperature, humidity, wind speed, and condition while preserving the original city name.
5. **Display Temperature, Humidity and Wind Speed**: Dedicated option generating a clean, structured table comparing these three critical metrics across all stored cities.
