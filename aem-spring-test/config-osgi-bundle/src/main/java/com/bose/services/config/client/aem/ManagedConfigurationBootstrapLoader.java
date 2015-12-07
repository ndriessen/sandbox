package com.bose.services.config.client.aem;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.api.SlingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

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
    @Reference
    private ManagedConfigurationTracker managedConfigurationTracker;
    @Reference
    private SlingRepository repository;

    private ExecutorService executorService;
    private JcrSessionTemplate<Void> sessionTemplate;

    @Activate
    public void activate() {
        //this.paths = PropertyUtils.getPropertyAsSet(PROPERTY_PATHS, componentContext, DEFAULTS_PATHS);
        //this.nodeTypes = PropertyUtils.getPropertyAsSet(PROPERTY_NODE_TYPES, componentContext, DEFAULTS_NODE_TYPES);

        try {
            this.sessionTemplate = new JcrSessionTemplate<>(repository);
            this.sessionTemplate.execute(new JcrSessionTemplate.Callback<Void>() {
                @Override
                public Void execute(Session session) throws Exception {
                    ManagedConfigurationMixin.registerMixin(session);
                    return null;
                }
            });
        } catch (Exception e) {
            logger.error("Error bootstrapping managed configuration", e);
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
