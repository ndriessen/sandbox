package com.bose.config.restclient;

import com.bose.config.client.ConfigResponse;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple configuration client for Spring cloud config.
 *
 * It doesn't need to run in a Spring application (it uses RestTemplate as a classpath dependency though)
 * Simply calls the REST endpoint to fetch the config, allows to pass profiles with each call.
 *
 * It will take all property sources returned from spring cloud config, and convert them into a single Map with properties.
 * Taking into account the profile order, it will ensure the correct value for each key is stored in the single map.
 * It uses the reverse order of the profiles as hierarchy (meaning the last profile passed in is the highest in priority)
 */
public class ConfigurationClient {
    public static final String SERVICE_URL = "http://localhost:8888/%s/%s";
    private RestTemplate restTemplate;
    private String name;
    private String label;
    private String[] profiles;
    private String prefix;

    public ConfigurationClient(String name, String... profiles) {
        this.restTemplate = new RestTemplate();
        this.name = name;
        if(this.profiles == null) {
            this.profiles = new String[]{"default"};
        } else {
            this.profiles = profiles;
        }
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public String[] getProfiles() {
        return profiles;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public Map<String, String> getConfiguration() {
        return getConfiguration(this.profiles);
    }

    public Map<String, String> getConfiguration(String... profiles) {
        RestTemplate restTemplate = new RestTemplate();
        ConfigResponse response = restTemplate.getForObject(
                String.format(SERVICE_URL, this.name, StringUtils.arrayToCommaDelimitedString(profiles)),
                ConfigResponse.class);

        Map<String, String> properties = new HashMap<>();
        for (PropertySource source : response.getPropertySources()) {
            @SuppressWarnings("unchecked")
            Map<String, String> map = (Map<String, String>) source
                    .getSource();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                if (!properties.containsKey(entry.getKey())) {
                    properties.putIfAbsent(entry.getKey(), entry.getValue());
                }
            }

        }
        return properties;
    }
}
