package com.bose.services.config.client.aem;

/**
 * Main exception thrown by the configuration client.
 */
public class ConfigurationException extends RuntimeException {
    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Object... params) {
        super(String.format(message, params));
    }

    public ConfigurationException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public ConfigurationException(String message, Throwable throwable, Object... params) {
        super(String.format(message, params), throwable);
    }
}
