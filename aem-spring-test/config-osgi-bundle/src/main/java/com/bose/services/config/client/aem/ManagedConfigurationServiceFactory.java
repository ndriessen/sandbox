package com.bose.services.config.client.aem;

import org.apache.felix.scr.annotations.*;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventListener;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component(immediate = true, name = "com.bose.services.config.client.aem.ManagedConfigurationServiceFactory")
@Service(ManagedConfigurationServiceFactory.class)
public class ManagedConfigurationServiceFactory {
    private static final Logger logger = LoggerFactory.getLogger(ManagedConfigurationServiceFactory.class);

    public static final String DEFAULT_SEARCH_PATH = "/apps";
    private static final String PROP_SEARCH_PATHS = "searchPaths";

    @SuppressWarnings("FieldCanBeLocal")
    @Property(name = PROP_SEARCH_PATHS, label = "Search paths", value = {DEFAULT_SEARCH_PATH})
    private String[] searchPaths;
    @Reference
    private ManagedConfigurationTracker tracker;
    @Reference
    private SlingRepository repository;

    private Map<String, EventListener> listeners = new HashMap<>();
    private ExecutorService searchExecutor;
    //session MUST stay open! otherwise listeners are removed that have been added by the session!
    private Session session;

    @Activate
    public void activate(ComponentContext componentContext) {
        try {
            this.searchExecutor = Executors.newFixedThreadPool(2);
            this.searchPaths = PropertyUtils.getPropertyAsArray(PROP_SEARCH_PATHS, componentContext, new String[]{DEFAULT_SEARCH_PATH});
            this.session = repository.loginAdministrative(null);
            for (String path : searchPaths) {
                //fire finder for path, just needs to run once, so no need to register as service
                logger.info("Triggering {} for search path '{}' (async)", ManagedConfigurationFinder.class.getName(), path);
                searchExecutor.execute(new ManagedConfigurationFinder(path, tracker, repository));
                //add listener
                java.util.Properties props = new java.util.Properties();
                props.put(ManagedConfigurationObserver.PROP_SEARCH_PATH, path);
                ManagedConfigurationObserver listener = new ManagedConfigurationObserver(path, tracker, repository);
                this.listeners.put(path, listener);
                /**
                 * IMPORTANT - DO NOT CLOSE THIS SESSION!!!! CLOSING THE SESSION REMOVES ALL ASSOCIATED LISTENERS!
                 */
                session.getWorkspace().getObservationManager().addEventListener(listener, // listener
                        Event.NODE_ADDED | Event.NODE_REMOVED, // eventTypes | Event.PROPERTY_ADDED
                        path, // absPath
                        true, // isDeep
                        null, // uuid
                        null, //nodeTypeNames
                        true // noLocal
                );
                logger.info("Added {} for search path '{}'", ManagedConfigurationObserver.class.getName(), path);
            }
        } catch (RepositoryException e) {
            cleanup();
            throw new ConfigurationException("FATAL - Error initializing services, the managed configuration system will not work", e);
        }
    }

    protected void cleanup() {
        //very safely cleanup everything...
        try {
            try {
                if (this.searchExecutor != null) {
                    this.searchExecutor.shutdownNow();
                }
            } catch (Exception e) {
                //ignore...
            }
            //remove listener
            try {
                if (listeners != null) {
                    for (EventListener listener : listeners.values()) {
                        session.getWorkspace().getObservationManager().removeEventListener(listener);
                    }
                }
            } catch (Exception e) {
                //ignore...
            }
        } finally {
            if (session != null && session.isLive()) {
                try {
                    session.logout();
                } catch (Exception e) {
                    //ignore
                }
            }
        }
    }

    @Deactivate
    public void deactivate(ComponentContext componentContext) {
        cleanup();
    }

    @SuppressWarnings("unused")
    public void bindSlingRepository(SlingRepository repository) {
        this.repository = repository;
    }

    @SuppressWarnings("unused")
    public void unbindSlingRepository(SlingRepository repository) {
        this.repository = null;
    }

    @SuppressWarnings("unused")
    public void bindManagedConfigurationTracker(ManagedConfigurationTracker managedConfigurationTracker) {
        this.tracker = managedConfigurationTracker;
    }

    @SuppressWarnings("unused")
    public void unbindManagedConfigurationTracker(ManagedConfigurationTracker managedConfigurationTracker) {
        this.tracker = null;
    }
}
