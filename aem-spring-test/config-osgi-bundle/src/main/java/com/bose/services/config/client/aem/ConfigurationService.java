package com.bose.services.config.client.aem;

import java.util.Properties;

/**
 * Configuration Service interface for querying configuration on demand.
 * s
 */
public interface ConfigurationService {
    Properties getProperties(String name, String... additionalProfiles);
}
