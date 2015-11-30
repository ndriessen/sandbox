package com.bose.aem.osgi;

import java.util.Map;

/**
 * Configuration Service interface for querying configuration on demand.
s */
public interface ConfigurationService {
    Map<String,String> getProperties();

    String getProperty(String name, String... profiles);

    String getRequiredProperty(String name, String... profiles);
}
