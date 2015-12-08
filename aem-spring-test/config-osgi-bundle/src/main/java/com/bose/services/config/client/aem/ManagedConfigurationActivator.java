package com.bose.services.config.client.aem;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;

/**
 * Bundle activator that manages registering a consumer to the AMQP refresh channel from spring cloud config.
 */
@SuppressWarnings("Convert2Lambda")
public class ManagedConfigurationActivator implements org.osgi.framework.BundleActivator {
    private static final Logger logger = LoggerFactory.getLogger(ManagedConfigurationActivator.class);
    private static final String AMQP_REFRESH_CHANNEL = "binder.springCloudBus";
    private static final String CONSUMER_TAG = "aem.config.client";
    private Channel channel;
    private ConfigRefreshConsumer consumer;
    private Connection rabbitConnection;

    protected void initEventChannel(BundleContext bundleContext) throws ConfigurationException {
        try {
            logger.info("Registering consumer for refresh events on AMQP channel '{}' ...", AMQP_REFRESH_CHANNEL);
            ConnectionFactory factory = new ConnectionFactory();
            factory.setAutomaticRecoveryEnabled(true);
            //factory.setUri("amqp://userName:password@hostName:portNumber/virtualHost");
            rabbitConnection = factory.newConnection();
            channel = rabbitConnection.createChannel();
            consumer = new ConfigRefreshConsumer(channel, bundleContext);
            channel.basicConsume(AMQP_REFRESH_CHANNEL, true, CONSUMER_TAG, consumer);
            logger.info("Successfully registered consumer on AMQP channel '{}'.", AMQP_REFRESH_CHANNEL);
        } catch (Exception e) {
            throw new ConfigurationException("Error registering consumer on AMQP channel " + AMQP_REFRESH_CHANNEL, e);
        }
    }

    protected void closeEventChannel() {
        try {
            if(channel != null) {
                channel.basicCancel(CONSUMER_TAG);
                if(channel.isOpen()){
                    channel.close();
                }
            }
            if(rabbitConnection != null && rabbitConnection.isOpen()){
                rabbitConnection.close();
            }
            consumer = null;
        } catch (Exception e) {
            logger.error("Error cleaning up consumer and connections...", e);
        }
    }

    protected void registerMixin(BundleContext bundleContext) throws Exception {
        ServiceReference ref = bundleContext.getServiceReference(SlingRepository.class.getName());
        try {
            if(ref != null) {
                SlingRepository repository = (SlingRepository) bundleContext.getService(ref);
                new JcrSessionTemplate<Void>(repository).execute(new JcrSessionTemplate.Callback<Void>() {
                    @Override
                    public Void execute(Session session) throws Exception {
                        logger.info("Registering mixin: '{}'", ManagedConfigurationMixin.NODE_TYPE);
                        ManagedConfigurationMixin.registerMixin(session);
                        return null;
                    }
                });
            }
        } catch (Exception e) {
            bundleContext.ungetService(ref);
        }
    }

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        try {
            registerMixin(bundleContext);
            initEventChannel(bundleContext);
        } catch (Exception e) {
            logger.error("Error creating consumer for refresh events", e);
        }
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        try {
            closeEventChannel();
        } catch (Exception e) {
            logger.error("Error stopping bundle.", e);
        }
    }
}
