package com.bose.aem.spring.config;

import org.eclipse.gemini.blueprint.extender.OsgiBeanFactoryPostProcessor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * Created by niki on 28/11/15.
 */
public class BootstrapBeanFactoryPostProcessor implements OsgiBeanFactoryPostProcessor {

    public void postProcessBeanFactory(BundleContext bundleContext, ConfigurableListableBeanFactory beanFactory) throws BeansException, InvalidSyntaxException, BundleException {

    }
}
