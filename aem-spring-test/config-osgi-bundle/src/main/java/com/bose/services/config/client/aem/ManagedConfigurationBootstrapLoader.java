package com.bose.services.config.client.aem;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 *
 */
@Component(immediate = true)
public class ManagedConfigurationBootstrapLoader {
    //    @Property(name = "paths", description = "The paths to watch for configuration placeholders.",
//            value = {"/etc", "/apps"})
    public static final String PROPERTY_PATHS = "paths";
    private static final Logger logger = LoggerFactory.getLogger(ManagedConfigurationBootstrapLoader.class);
    private static final String[] DEFAULTS_PATHS = {"/etc", "/apps"};

    private static final String[] DEFAULTS_NODE_TYPES = {"sling:OsgiConfig"};
    //    @Property(name = "nodeTypes", description = "The node types to watch for configuration placeholders.",
//            value = {"sling:OsgiConfig"}, cardinality = 10)
    private static final String PROPERTY_NODE_TYPES = "nodeTypes";
    private static final String QUERY_CONFIG_NODES = "SELECT * FROM [" + ManagedConfigurationMixin.NODE_TYPE + "] AS s WHERE ISDESCENDANTNODE([/apps])";

    /**
     * The paths to watch for.
     * Defaults to /etc and /apps
     */
    private Set<String> paths = new HashSet<>();
    private Set<String> nodeTypes = new HashSet<>();


    @Reference
    private ResourceResolverFactory resourceResolverFactory;
    @Reference
    private ConfigurationService configurationService;

    private void searchConfigNodes(Session session) {
        try {
            QueryManager queryManager = session.getWorkspace().getQueryManager();
            Query query = queryManager.createQuery(QUERY_CONFIG_NODES, Query.JCR_SQL2);
            QueryResult result = query.execute();
            NodeIterator nodes = result.getNodes();
            while (nodes.hasNext()) {
                Node node = nodes.nextNode();
                logger.info("Checking node {} for configuration placeholders", node.getPath());
                String[] additionalProfiles = null;
                if (node.hasProperty(ManagedConfigurationMixin.PROPERTY_ADDITIONAL_PROFILES)) {
                    additionalProfiles = PropertyUtils.getPropertyAsArray(node.getProperty(ManagedConfigurationMixin.PROPERTY_ADDITIONAL_PROFILES));
                }
                Properties remoteProperties = configurationService.getProperties(node.getName(), additionalProfiles);
                if (!CollectionUtils.isEmpty(remoteProperties)) {
                    //only do work if we have actual properties...
                    PlaceHolderAwareNode wrapper = new PlaceHolderAwareNode(node);
                    if (wrapper.resolvePlaceholders(remoteProperties)) {
                        //updated props
                        session.save();
                    }
                }
            }
        } catch (Throwable e) {
            logger.error("Error while boostrapping remote configuration. Property placeholders will not be replaced!", e);
        }
    }

    @Activate
    public void activate() {
        //this.paths = PropertyUtils.getPropertyAsSet(PROPERTY_PATHS, componentContext, DEFAULTS_PATHS);
        //this.nodeTypes = PropertyUtils.getPropertyAsSet(PROPERTY_NODE_TYPES, componentContext, DEFAULTS_NODE_TYPES);
        Session session = null;
        try {
            if (this.resourceResolverFactory != null) {
                ResourceResolver resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
                session = resourceResolver.adaptTo(Session.class);

                ManagedConfigurationMixin.registerMixin(session);
                searchConfigNodes(session);
            } else {
                logger.error("ObservationManager is <null>. Cannot register event listeners.");
            }
        } catch (Exception e) {
            logger.error("Error bootstrapping managed configuration", e);
        } finally {
            if (session != null) {
                session.logout();
            }
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
    public void bindResourceResolverFactory(ResourceResolverFactory resourceResolverFactory) {
        this.resourceResolverFactory = resourceResolverFactory;
    }

    @SuppressWarnings("unused")
    public void unbindResourceResolverFactory(ResourceResolverFactory resourceResolverFactory) {
        this.resourceResolverFactory = null;
    }


}
