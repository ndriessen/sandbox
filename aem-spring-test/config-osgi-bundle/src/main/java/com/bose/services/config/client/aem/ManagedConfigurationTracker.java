package com.bose.services.config.client.aem;

import com.rabbitmq.client.*;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.jcr.api.SlingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import javax.jcr.Node;
import javax.jcr.Session;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Service for tracking managed configurations.
 * <p>
 * Note that this service is going be called from multiple threads, so everything in here must be thread-safe and optimized
 * for concurrent access!
 */
@Component
@Service(ManagedConfigurationTracker.class)
public class ManagedConfigurationTracker {
    private static final Logger logger = LoggerFactory.getLogger(ManagedConfigurationTracker.class);
    private static final ReadWriteLock lock = new ReentrantReadWriteLock();
    private Set<ManagedConfiguration> detectedConfigurations = new HashSet<>();

    @Reference
    private ConfigurationService configurationService;
    @Reference
    private SlingRepository repository;
    private JcrSessionTemplate<Void> sessionTemplate;

    public void track(ManagedConfiguration configuration) {
        lock.writeLock().lock();
        this.detectedConfigurations.add(configuration);
        lock.writeLock().unlock();
    }

    public Set<ManagedConfiguration> getDetectedConfigurations() {
        try {
            lock.readLock().lock();
            return detectedConfigurations;
        } finally {
            lock.readLock().unlock();
        }
    }

    //    private RefreshRemoteApplicationEvent read(byte[] payload) throws Exception {
//        ObjectMapper mapper = new ObjectMapper();
//        mapper.registerModule(new SubtypeModule());
//        ObjectReader reader = mapper.readerFor(RefreshRemoteApplicationEvent.class);
//        return reader.readValue(payload);
//    }

//                            try {
//                                RefreshRemoteApplicationEvent event = read(body);
//                            } catch (Exception e) {
//                                logger.error("Error reading payload with jackson", e);
//                            }
//                            try {
//                                Kryo kryo = new Kryo();
//                                kryo.register(RefreshRemoteApplicationEvent.class);
//                                Input input = new Input(new ByteArrayInputStream(body));
//                                RefreshRemoteApplicationEvent refreshEvent = kryo.readObject(input, RefreshRemoteApplicationEvent.class);
//                                input.close();
//
//                                // (process the message components here ...)
//                                channel.basicAck(deliveryTag, false);
//                            } catch (Exception e) {
//                                logger.error("Error reading payload with kryo", e);
//                            }

    @Activate
    public void activate() {
        try {
            this.sessionTemplate = new JcrSessionTemplate<>(repository);
            ConnectionFactory factory = new ConnectionFactory();
            factory.setAutomaticRecoveryEnabled(true);
            //factory.setUri("amqp://userName:password@hostName:portNumber/virtualHost");
            Connection conn = factory.newConnection();
            Channel channel = conn.createChannel();
            channel.basicConsume("binder.springCloudBus", true, "tax:dev,us",
                    new ConfigRefreshConsumer(channel));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    public void bindConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @SuppressWarnings("unused")
    public void unbindConfigurationService(ConfigurationService configurationService) {
        this.configurationService = null;
    }

    @SuppressWarnings("unused")
    public void bindSlingRepository(SlingRepository repository) {
        this.repository = repository;
    }

    @SuppressWarnings("unused")
    public void unbindSlingRepository(SlingRepository repository) {
        this.repository = null;
    }

    private class ConfigRefreshConsumer extends DefaultConsumer {
        public ConfigRefreshConsumer(Channel channel) {
            super(channel);
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
             *
             * TODO: optimize this, preferably with the ability to use the payload.
             */
            try {
                sessionTemplate.execute(new JcrSessionTemplate.Callback<Void>() {
                    @Override
                    public Void execute(Session session) throws Exception {
                        lock.readLock().lock();
                        //make copy, because the rest of the code will take a while...
                        List<ManagedConfiguration> managedConfigurations = new ArrayList<>(detectedConfigurations);
                        lock.readLock().unlock();
                        for (ManagedConfiguration configuration : managedConfigurations) {
                            try {
                                Node node = session.getNode(configuration.getNodePath());
                                if (node != null) {
                                    PlaceHolderAwareNode wrapper = new PlaceHolderAwareNode(node);
                                    String[] additionalProfiles = wrapper.getAdditionalProfilesProperty(node);
                                    if (configurationService.refresh(node.getName(), additionalProfiles)) {
                                        Properties remoteProperties = configurationService.getProperties(node.getName(), additionalProfiles);
                                        if (!CollectionUtils.isEmpty(remoteProperties)) {
                                            //only do work if we have actual properties...
                                            if (wrapper.resolvePlaceholders(remoteProperties)) {
                                                //updated some props...
                                                session.save();
                                            } //else: nothing changed...
                                        }
                                    }
                                } else {
                                    logger.warn("Could not find node at '{}', maybe node was deleted? Skipping...", configuration.getNodePath());
                                }
                            } catch (Exception e) {
                                logger.error("Error refreshing configuration for node " + configuration.getNodePath(), e);
                            }
                        }
                        return null;
                    }
                });
            } catch (Exception e) {
                logger.error("Unexpected error handling configuration refresh", e);
            }

        }
    }

    /**
     * TODO: This class should listen to config updates from the spring config service for the tracked configurations
     */
}
