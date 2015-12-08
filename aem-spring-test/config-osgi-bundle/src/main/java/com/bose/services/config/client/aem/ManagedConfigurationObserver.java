package com.bose.services.config.client.aem;

import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.jcr.api.SlingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

/**
 * JCR Observer that will detect added, removed or changed managed configuration nodes and will inform the
 * {@link ManagedConfigurationTracker} accordingly.
 */
//@Component
public class ManagedConfigurationObserver implements EventListener {
    public static final String PROP_SEARCH_PATH = "search.path";
    private static final Logger logger = LoggerFactory.getLogger(ManagedConfigurationObserver.class);
    private static final String JCR_MIXIN_TYPES = "jcr:mixinTypes";
    private Session session;

    //@org.apache.felix.scr.annotations.Property(name = PROP_SEARCH_PATH, label = "Search Path")
    private String searchPath;
    //@Reference
    private SlingRepository repository;

    private JcrSessionTemplate<Void> sessionTemplate;
    @Reference
    private ManagedConfigurationTracker tracker;

    public ManagedConfigurationObserver(String searchPath, ManagedConfigurationTracker tracker, SlingRepository repository) {
        this.repository = repository;
        this.tracker = tracker;
        this.searchPath = searchPath;
        this.sessionTemplate = new JcrSessionTemplate<Void>(repository);
    }

    public void onEvent(EventIterator events) {
        try {
            if (events != null) {
                sessionTemplate.execute(new JcrSessionTemplate.Callback<Void>() {
                    @Override
                    public Void execute(Session session) throws Exception {
                        while (events.hasNext()) {
                            Event event = (Event) events.next();
                            try {
                                if (event.getType() == Event.NODE_ADDED && hasMixin(event)) {
                                    Node node = session.getNode(event.getPath());
                                    tracker.track(node);
                                } else if (event.getType() == Event.NODE_REMOVED) {
                                    tracker.untrack(event.getPath());
                                }
                                logger.info("Received JCR event for path: " + event.getPath());
                            } catch (Exception e) {
                                logger.error("Error adding/removing JCR node to/from tracked managed configuration", e);
                            }
                        }
                        return null;
                    }
                });
            }
        } catch (Exception e) {
            logger.error("Error handling JCR event", e);
        }
    }

    private boolean hasMixin(Event event) throws RepositoryException {
        @SuppressWarnings("unchecked")
        String[] mixins = (String[]) event.getInfo().getOrDefault(JCR_MIXIN_TYPES, new String[0]);
        if (mixins != null && mixins.length > 0) {
            for (String mixin : mixins) {
                if (ManagedConfigurationMixin.NODE_TYPE.equalsIgnoreCase(mixin)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @Activate public void activate(ComponentContext componentContext) {
     * try {
     * String searchPath = PropertyUtils.getProperty(PROP_SEARCH_PATH, componentContext, ManagedConfigurationServiceFactory.DEFAULT_SEARCH_PATH);
     * session = repository.loginAdministrative(null);
     * session.getWorkspace().getObservationManager().addEventListener(this, // listener
     * Event.NODE_ADDED | Event.NODE_REMOVED, // eventTypes
     * searchPath, // absPath
     * true, // isDeep
     * null, // uuid
     * new String[]{ManagedConfigurationMixin.NODE_TYPE}, //nodeTypeNames
     * true // noLocal
     * );
     * } catch (RepositoryException e) {
     * logger.error("FATAL - Error registering JCR Observation listener, will NOT detect new managed config nodes!");
     * }
     * <p>
     * }
     * @Deactivate public void deactivate(ComponentContext componentContext) {
     * try {
     * session.getWorkspace().getObservationManager().removeEventListener(this);
     * session.logout();
     * session = null;
     * } catch (RepositoryException e) {
     * //nothing we can do about that...
     * }
     * }
     */

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
