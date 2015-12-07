package com.bose.services.config.client.aem;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bundle activator that manages registering a consumer to the AMQP refresh channel from spring cloud config.
 */
public class RefreshChannelActivator implements org.osgi.framework.BundleActivator {
    private static final Logger logger = LoggerFactory.getLogger(RefreshChannelActivator.class);
    private static final String AMQP_REFRESH_CHANNEL = "binder.springCloudBus";
    private static final String CONSUMER_TAG = "tax:dev,us";
    private Channel channel;
    private ConfigRefreshConsumer consumer;

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        try {
            logger.info("Registering consumer for refresh events on AMQP channel '{}' ...", AMQP_REFRESH_CHANNEL);
            ConnectionFactory factory = new ConnectionFactory();
            factory.setAutomaticRecoveryEnabled(true);
            //factory.setUri("amqp://userName:password@hostName:portNumber/virtualHost");
            Connection conn = factory.newConnection();
            channel = conn.createChannel();
            consumer = new ConfigRefreshConsumer(channel, bundleContext);
//            channel.basicConsume("binder.springCloudBus", true, "testconf:default",
//                    new DefaultConsumer(channel) {
//                        @Override
//                        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
//                            logger.info("======================received TESTCONF event=========================");
//                            envelope.getDeliveryTag();
//                            envelope.getRoutingKey();
//
//                        }
//                    });
//            channel.basicConsume("binder.springCloudBus", true, "tax:default",
//                    new DefaultConsumer(channel) {
//                        @Override
//                        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
//                            logger.info("======================received TAX event=========================");
//                            envelope.getDeliveryTag();
//                            envelope.getRoutingKey();
//
//                        }
//                    });
            channel.basicConsume(AMQP_REFRESH_CHANNEL, true, "tax:dev,us", consumer);

            logger.info("Succesfully registered consumer on AMQP channel '{}'.", AMQP_REFRESH_CHANNEL);
        } catch (Exception e) {
            logger.error("Error creating consumer for refresh events", e);
        }
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        try {
            channel.basicCancel(CONSUMER_TAG);
            channel.close();
        } catch (Exception e) {
            logger.error("Error cleaning up consumer and connections...", e);
        }
    }
}
