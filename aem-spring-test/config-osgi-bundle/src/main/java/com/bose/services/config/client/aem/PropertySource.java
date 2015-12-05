package com.bose.services.config.client.aem;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class PropertySource {

    private String name;

    private Map<?, ?> source;

    public PropertySource() {
    }

    @JsonCreator
    public PropertySource(@JsonProperty("name") String name,
                          @JsonProperty("source") Map<?, ?> source) {
        this.name = name;
        this.source = source;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSource(Map<?, ?> source) {
        this.source = source;
    }

    public String getName() {
        return name;
    }

    public Map<?, ?> getSource() {
        return source;
    }

    @Override
    public String toString() {
        return "PropertySource [name=" + name + "]";
    }

}