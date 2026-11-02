package com.weather;

/**
 * Custom checked exception representing errors that occur while communicating
 * with or processing responses from external weather APIs.
 *
 * Demonstrates:
 *  - Exception Handling (Custom Checked Exception)
 *  - Inheritance (extends java.lang.Exception)
 *  - Separation of Concerns (distinguishes network/API failures from domain validation failures)
 */
public class WeatherApiException extends Exception {

    /**
     * Constructs a new WeatherApiException with a descriptive error message.
     *
     * @param message Human-readable description of the error.
     */
    public WeatherApiException(String message) {
        super(message);
    }

    /**
     * Constructs a new WeatherApiException with a descriptive error message
     * and the underlying cause (e.g., IOException, InterruptedException).
     *
     * @param message Human-readable description of the error.
     * @param cause   The underlying throwable that caused this exception.
     */
    public WeatherApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
