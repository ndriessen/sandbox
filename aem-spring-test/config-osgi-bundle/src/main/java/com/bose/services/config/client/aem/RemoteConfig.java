package com.bose.services.config.client.aem;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RemoteConfig {
    private String name;
    private String label;
    private String version;
    private List<String> profiles;
    private List<PropertySource> propertySources;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<String> getProfiles() {
        return profiles;
    }

    public void setProfiles(List<String> profiles) {
        this.profiles = profiles;
    }

    public List<PropertySource> getPropertySources() {
        return propertySources;
    }

    public void setPropertySources(List<PropertySource> propertySources) {
        this.propertySources = propertySources;
    }
}

