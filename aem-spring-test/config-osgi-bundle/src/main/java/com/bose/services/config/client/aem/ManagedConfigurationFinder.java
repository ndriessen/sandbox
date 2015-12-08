package com.bose.services.config.client.aem;

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

/**
 * Searches for managed configuration, retrieves remote configuration properties, and resolves placeholders.
 * Each node that is changed is updated with meta-data defined by {@see ManagedConfigurationMixin} and tracked using {@see ManagedConfigurationTracker}.
 * <p>
 * This service runs in it's own thread
 *
 * @author Niki Driessen
 */
//@Component(immediate = true)
//@Service(ManagedConfigurationFinder.class)
public class ManagedConfigurationFinder implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ManagedConfigurationFinder.class);
    private static final String QUERY_CONFIG_NODES = "SELECT * FROM [%s] AS s WHERE ISDESCENDANTNODE([%s])";

    private ManagedConfigurationTracker managedConfigurationTracker;
    private JcrSessionTemplate<Void> sessionTemplate;
    private final String searchQuery;

    public ManagedConfigurationFinder(String searchPath, ManagedConfigurationTracker managedConfigurationTracker, SlingRepository repository) {
        this.managedConfigurationTracker = managedConfigurationTracker;
        this.sessionTemplate = new JcrSessionTemplate<Void>(repository);
        this.searchQuery = String.format(QUERY_CONFIG_NODES, ManagedConfigurationMixin.NODE_TYPE, searchPath);
    }

    @Override
    public void run() {
        try {
            sessionTemplate.execute(new JcrSessionTemplate.Callback<Void>() {
                @Override
                public Void execute(Session session) throws Exception {
                    logger.info("Searching all managed configuration nodes in the JCR");
                    QueryManager queryManager = session.getWorkspace().getQueryManager();
                    Query query = queryManager.createQuery(searchQuery, Query.JCR_SQL2);
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

}
