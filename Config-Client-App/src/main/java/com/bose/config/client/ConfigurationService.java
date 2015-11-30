package com.bose.config.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class ConfigurationService {
    @Autowired
    private ConfigurableEnvironment env;

    @PostConstruct
    public void init() {
        env.setActiveProfiles("tax");
        env.setDefaultProfiles();
        String prop = env.getProperty("tax.server.url");
        System.out.println("prop = " + prop);
    }
}
