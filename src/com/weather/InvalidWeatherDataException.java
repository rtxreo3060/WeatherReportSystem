package com.weather;

/**
 * Custom Exception class for weather data validation.
 * Demonstrates: Exception Handling (Custom Checked Exception).
 */
public class InvalidWeatherDataException extends Exception {
    
    public InvalidWeatherDataException(String message) {
        super(message);
    }

    public InvalidWeatherDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
