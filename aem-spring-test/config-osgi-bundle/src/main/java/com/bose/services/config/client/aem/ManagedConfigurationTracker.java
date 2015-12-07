package com.bose.services.config.client.aem;

import org.apache.felix.scr.annotations.*;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Service for tracking managed configurations.
 * <p>
 * Note that this service is going be called from multiple threads, so everything in here must be thread-safe and optimized
 * for concurrent access!
 */
@Component(immediate = true, name = "com.bose.services.config.client.aem.ManagedConfigurationTracker")
@Service(ManagedConfigurationTracker.class)
public class ManagedConfigurationTracker {
    private static final Logger logger = LoggerFactory.getLogger(ManagedConfigurationTracker.class);
    private static final ReadWriteLock lock = new ReentrantReadWriteLock();
    //NEVER use directly, always go through the getter to ensure proper synchronization in multi-threaded access...
    private Set<ManagedConfiguration> detectedConfigurations = new HashSet<>();

    @Reference
    private ConfigurationService configurationService;
    @Reference
    private SlingRepository repository;
    private JcrSessionTemplate<Void> sessionTemplate;

    private BundleContext bundleContext;

    public void track(Node node) throws ConfigurationException {
        try {
            logger.info("Tracking node '{}' for managed configuration changes.", node.getPath());
            lock.writeLock().lock();
            ManagedConfiguration configuration = new ManagedConfiguration(node);
            detectedConfigurations.add(configuration);
            lock.writeLock().unlock();
            configuration.configure(configurationService, sessionTemplate);
        } catch (Exception e) {
            throw new ConfigurationException("Error tracking node.", e);
        }
    }

    public Set<ManagedConfiguration> getDetectedConfigurations() {
        try {
            lock.readLock().lock();
            return new HashSet<>(detectedConfigurations);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void refresh() {
        Set<ManagedConfiguration> managedConfigurations = getDetectedConfigurations();
        for (ManagedConfiguration configuration : managedConfigurations) {
            configuration.configure(configurationService, sessionTemplate, true);
        }
    }

    @Activate
    public void activate(ComponentContext componentContext) {
        try {
            this.bundleContext = componentContext.getBundleContext();
            this.sessionTemplate = new JcrSessionTemplate<>(repository);
        } catch (Exception e) {
            logger.error("FATAL - Error activating " + this.getClass().getName() + " component.", e);
        }
    }

    @Deactivate
    public void deactivate(ComponentContext componentContext) {
        try {
            this.sessionTemplate = null;
            this.detectedConfigurations = new HashSet<>();
        } catch (Exception e) {
            //ignore, nothing we can do anymore...
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
}
