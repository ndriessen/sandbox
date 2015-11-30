package com.bose.aem.spring.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * Generic implementation to listen to blueprint references and create beans for the injected OSGi services in the current application context.
 * Osgi services can then be wired as usual, using any valid spring wiring mechanism.
 *
 * @author Niki Driessen
 */
@Component
public class OsgiServiceReferenceBinder<T> implements ApplicationListener<ContextRefreshedEvent>, Ordered {
    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        ConfigurableApplicationContext applicationContext = (ConfigurableApplicationContext) event.getApplicationContext();

    }

    private T reference;

    private void registerOsgiReference(T reference){
        Assert.notNull(reference);
        this.reference = reference;
//        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(SomeClass.class);
//        builder.addPropertyReference("propertyName", "someBean");  // add dependency to other bean
//        builder.addPropertyValue("propertyName", someValue);      // set property value
//        DefaultListableBeanFactory factory = (DefaultListableBeanFactory) context.getBeanFactory();
//        factory.registerBeanDefinition("beanName", builder.getBeanDefinition());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }
}
