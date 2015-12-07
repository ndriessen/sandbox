package com.bose.services.config.client.aem;

import com.rabbitmq.client.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Simple RabbitMQ consumer for configuration refresh notifications.
 */
public class ConfigRefreshConsumer extends DefaultConsumer {
    private static final Logger logger = LoggerFactory.getLogger(ConfigRefreshConsumer.class);
    private BundleContext bundleContext;

    public ConfigRefreshConsumer(Channel channel, BundleContext componentContext) {
        super(channel);
        this.bundleContext = componentContext;
    }

    @Override
    public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {

    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope,
                               AMQP.BasicProperties properties, byte[] body) throws IOException {
        /**
         * Spring Bus sends some serialized form of 'org.springframework.cloud.bus.event.RefreshRemoteApplicationEvent'
         * and I can't seem to deserialize it with neither Jackson2 or Kryo (which are the both 'default' serializers as far as
         * I could find out in the code/docs. Could be because it's a pain in the ass to get them working in OSGi
         * I had to embed a lot of deps to get this running
         *
         * For now, ignoring payload and triggering a refresh check for all tracked configurations...
         * TODO: optimize this, preferably with the ability to use the payload.
         */
        ServiceReference ref = null;
        try {
            StringBuilder name = new StringBuilder();
            for (byte b : body) {
                if (b > '\u0001') {
                    name.append((char) b);
                }
            }
            String configurationName = name.substring(0, name.indexOf(":"));

            logger.info("Received configuration REFRESH notification");
            ref = bundleContext.getServiceReference(ManagedConfigurationTracker.class.getName());
            ManagedConfigurationTracker tracker = (ManagedConfigurationTracker) bundleContext.getService(ref);
            tracker.refresh();
        } catch (Exception e) {
            logger.error("Unexpected error in refresh channel consumer", e);
        } finally {
            try {
                bundleContext.ungetService(ref);
            } catch (Exception e) {
                //ignore
            }
        }

    }
}
