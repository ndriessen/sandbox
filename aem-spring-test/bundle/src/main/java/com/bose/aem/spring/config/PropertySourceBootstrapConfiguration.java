package com.bose.aem.spring.config;

import org.apache.sling.api.resource.ResourceResolver;
import org.eclipse.gemini.blueprint.context.event.OsgiBundleApplicationContextEvent;
import org.eclipse.gemini.blueprint.context.event.OsgiBundleApplicationContextListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.*;
import org.springframework.stereotype.Component;
import org.springframework.core.env.PropertySource;

import java.util.*;

/**
 * @author Dave Syer
 */
//@EnableConfigurationProperties(PropertySourceBootstrapProperties.class)
@Component
public class PropertySourceBootstrapConfiguration
        implements ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered, ApplicationListener<ContextRefreshedEvent> {

    private static final String BOOTSTRAP_PROPERTY_SOURCE_NAME = "ConfigurationService";

    private int order = Ordered.HIGHEST_PRECEDENCE + 10;

    @Autowired
    private ResourceResolver resourceResolver;


    private List<PropertySourceLocator> propertySourceLocators = new ArrayList<>();

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        ConfigurableApplicationContext applicationContext = (ConfigurableApplicationContext) event.getApplicationContext();
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        environment.setActiveProfiles("dev","us");
        Map<String,Object> clientProps = new HashMap<>();
        clientProps.putIfAbsent("spring.application.name", "tax");
        clientProps.putIfAbsent("spring.cloud.config.profile", "dev,us");
        environment.getPropertySources().addFirst(new MapPropertySource("config-client-props", clientProps));
        this.propertySourceLocators = new ArrayList<>();
        this.propertySourceLocators.add(new ConfigServicePropertySourceLocator(environment));
        initialize((ConfigurableApplicationContext) event.getApplicationContext());
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    public void setPropertySourceLocators(
            Collection<PropertySourceLocator> propertySourceLocators) {
        this.propertySourceLocators = new ArrayList<>(propertySourceLocators);
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        CompositePropertySource composite = new CompositePropertySource(
                BOOTSTRAP_PROPERTY_SOURCE_NAME);
        AnnotationAwareOrderComparator.sort(this.propertySourceLocators);
        boolean empty = true;
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        for (PropertySourceLocator locator : this.propertySourceLocators) {
            PropertySource<?> source = null;
            source = locator.locate(environment);
            if (source == null) {
                continue;
            }
//            logger.info("Located property source: " + source);
            composite.addPropertySource(source);
            empty = false;
        }
        if (!empty) {
            MutablePropertySources propertySources = environment.getPropertySources();
            if (propertySources.contains(BOOTSTRAP_PROPERTY_SOURCE_NAME)) {
                propertySources.remove(BOOTSTRAP_PROPERTY_SOURCE_NAME);
            }
            insertPropertySources(propertySources, composite);
        }
    }


    private void insertPropertySources(MutablePropertySources propertySources,
                                       CompositePropertySource composite) {
        MutablePropertySources incoming = new MutablePropertySources();
        incoming.addFirst(composite);
        PropertySourceBootstrapProperties remoteProperties = new PropertySourceBootstrapProperties();
//        new RelaxedDataBinder(remoteProperties, "spring.cloud.config")
//                .bind(new PropertySourcesPropertyValues(incoming));
        if (!remoteProperties.isAllowOverride() || (!remoteProperties.isOverrideNone()
                && remoteProperties.isOverrideSystemProperties())) {
            propertySources.addFirst(composite);
            return;
        }
        if (remoteProperties.isOverrideNone()) {
            propertySources.addLast(composite);
            return;
        }
        if (propertySources
                .contains(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)) {
            if (!remoteProperties.isOverrideSystemProperties()) {
                propertySources.addAfter(
                        StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                        composite);
            } else {
                propertySources.addBefore(
                        StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                        composite);
            }
        } else {
            propertySources.addLast(composite);
        }
    }

}
