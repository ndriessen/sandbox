package com.bose.aem.spring;

import com.bose.aem.spring.mvc.MvcConfiguration;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.api.resource.ResourceResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;

@ComponentScan
@Import({MvcConfiguration.class})
public class SpringConfiguration {
    @Autowired
    private ConfigurableEnvironment environment;

    @Reference
    private ResourceResolver resourceResolver;

    @Bean
    public ResourceResolver resourceResolver(){
        return this.resourceResolver;
    }


    @Bean
    public String greetingMessage() {
        return "Hello";
    }
}
