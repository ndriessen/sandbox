package com.bose.services.config.client.aem;

import com.rabbitmq.client.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

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
         * For now, I have implemented a bit of a dirty hacks with filters out the readable string parts from the somewhat serialized byte stream.
         * TODO: FIX THIS DIRTY HACK! with the ability to deserialize the payload decently :)
         */
        ServiceReference ref = null;
        try {
            logger.info("Received configuration REFRESH notification");
            String configurationName = null;
            try {
                StringBuilder name = new StringBuilder();
                //this should filter out all the unreadable chars...
                for (byte b : body) {
                    if (b > '\u0001') {
                        name.append((char) b);
                    }
                }
                //TODO: and for now, this gives us the name of the config... very BRITTLE!! needs fixing..
                configurationName = name.substring(0, name.indexOf(":"));
            } catch (Throwable e) {
                //because this is crappy code, make it as fail-safe as possible for now, if anything goes wrong, just refresh everything...
                logger.error("The very CRAPPY hack is causing errors, you might want to invest some time and fix this in a decent way", e);
                configurationName = null;
            }
            //ensure to default to everything...
            if(StringUtils.isEmpty(configurationName)){
                configurationName = ManagedConfigurationTracker.REFRESH_ALL;
            }
            ref = bundleContext.getServiceReference(ManagedConfigurationTracker.class.getName());
            ManagedConfigurationTracker tracker = (ManagedConfigurationTracker) bundleContext.getService(ref);
            tracker.refresh(configurationName);
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
