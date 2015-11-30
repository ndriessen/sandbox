package com.bose.aem.spring.mvc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ConfigurationController {
    @Autowired
    private ConfigurableEnvironment env;

    @RequestMapping("/config/info")
    @ResponseBody
    public String info(){
        String profiles = StringUtils.arrayToCommaDelimitedString(env.getActiveProfiles());
        String sources = "";
        for (PropertySource<?> propertySource : env.getPropertySources()) {
            sources = sources.concat(propertySource.getName()).concat("<br/>");
        }


        return String.format("Active Profiles: %s<br/>Properties: %s", profiles, sources);
    }
}
