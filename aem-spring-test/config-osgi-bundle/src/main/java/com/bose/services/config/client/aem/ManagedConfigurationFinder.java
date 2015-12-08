package com.bose.services.config.client.aem;

import org.apache.felix.scr.annotations.*;
import org.apache.sling.jcr.api.SlingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Searches for managed configuration, retrieves remote configuration properties, and resolves placeholders.
 * Each node that is changed is updated with meta-data defined by {@see ManagedConfigurationMixin} and tracked using {@see ManagedConfigurationTracker}.
 * <p>
 * This service runs in it's own thread
 *
 * @author Niki Driessen
 */
@Component(immediate = true)
@Service(ManagedConfigurationFinder.class)
public class ManagedConfigurationFinder implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ManagedConfigurationFinder.class);
    private static final String QUERY_CONFIG_NODES = "SELECT * FROM [" + ManagedConfigurationMixin.NODE_TYPE + "] AS s WHERE ISDESCENDANTNODE([/apps])";

    private JcrSessionTemplate<Void> sessionTemplate;
    @Reference
    private ConfigurationService configurationService;
    @Reference
    private ManagedConfigurationTracker managedConfigurationTracker;
    @Reference
    private SlingRepository repository;

    private ExecutorService searchThread;


    @Activate
    public void activate() {
        try {
            searchThread = Executors.newSingleThreadExecutor();
            searchThread.execute(this);
        } catch (Exception e) {
            logger.error("Error starting configuration finder thread", e);
        }
    }

    @Deactivate
    public void deactivate() {
        try {
            if (searchThread != null) {
                searchThread.shutdownNow();
            }
        } catch (Exception e) {
            //ignore, we tried to clean up
        }
    }

    @Override
    public void run() {
        try {
            this.sessionTemplate = new JcrSessionTemplate<>(repository);
            this.sessionTemplate.execute(new JcrSessionTemplate.Callback<Void>() {
                @Override
                public Void execute(Session session) throws Exception {
                    logger.info("Searching all managed configuration nodes in the JCR");
                    QueryManager queryManager = session.getWorkspace().getQueryManager();
                    Query query = queryManager.createQuery(QUERY_CONFIG_NODES, Query.JCR_SQL2);
                    QueryResult result = query.execute();
                    NodeIterator nodes = result.getNodes();
                    while (nodes.hasNext()) {
                        Node node = nodes.nextNode();
                        try {
                            managedConfigurationTracker.track(node);
                        } catch (RepositoryException e) {
                            logger.error("Error creating managed configuration, issues with accessing the supplied node, skipping node '{}'", node.getPath());
                        } catch (ConfigurationException e) {
                            logger.error("Could not configure the node, skipping node '{}'", node.getPath());
                        }
                    }
                    return null;
                }
            });
        } catch (Throwable e) {
            logger.error("Error while searching managed configuration nodes... Property placeholders might not be replaced!", e);
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
    public void bindManagedConfigurationTracker(ManagedConfigurationTracker managedConfigurationTracker) {
        this.managedConfigurationTracker = managedConfigurationTracker;
    }

    @SuppressWarnings("unused")
    public void unbindManagedConfigurationTracker(ManagedConfigurationTracker managedConfigurationTracker) {
        this.managedConfigurationTracker = null;
    }

    @SuppressWarnings("unused")
    public void bindSlingRepository(SlingRepository repository) {
        this.repository = repository;
    }

    @SuppressWarnings("unused")
    public void unbindSlingRepository(SlingRepository repository) {
        this.repository = null;
    }
}
