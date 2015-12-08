package com.bose.services.config.client.aem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Properties;

/**
 * Main worker class, this handles configuring managed configuration nodes.
 *
 * @author Niki Driessen
 */
public class ManagedConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(ManagedConfigurationTracker.class);
    private String nodePath;
    private String configurationName;
    private String[] profiles;

    /**
     * Creates a new managed configuration.
     *
     * This <strong>MUST</strong> be called from within an active {@link Session}.
     *
     * @param node the node to manage, not null.
     * @throws RepositoryException when not called from and active {@link Session}.
     */
    public ManagedConfiguration(Node node) throws RepositoryException {
        Assert.notNull(node);
        this.nodePath = node.getPath();
        this.configurationName = node.getName();
        PlaceHolderAwareNode wrapper = new PlaceHolderAwareNode(node);
        this.profiles = wrapper.getAdditionalProfilesProperty();
    }

    /**
     * Configures the managed node.
     * <p>
     * This will always use cached configuration properties (if they have been fetched before).
     *
     * @param configurationService the {@see ConfigurationService} to use, not null.
     * @param sessionTemplate      the {@see JcrSessionTemplate} to use, not null.
     * @return <code>true</code> if any properties were updated on the node.
     * @throws ConfigurationException when configuring the node failed.
     * @throws IllegalStateException  when the managed node can not be accessed. This could happen is the node gets deleted by another process.
     * @see #configure(ConfigurationService, JcrSessionTemplate, boolean)
     */
    public boolean configure(ConfigurationService configurationService, JcrSessionTemplate sessionTemplate) throws ConfigurationException, IllegalStateException {
        return configure(configurationService, sessionTemplate, false);
    }

    /**
     * Configures the managed node.
     * <p>
     * Configuring will fetch the properties from the configuration service, and resolve placeholders on the managed nodes.
     * It will check the node for any additional configuration profiles specified in the
     * {@see com.bose.services.config.client.aem.ManagedConfigurationMixin#PROPERTY_ADDITIONAL_PROFILES} property and pass them
     * to get the remote configuration.
     * <p>
     * This method will only report <code>true</code> if anything was actually changed, and also only apply changes to the node in that case to minimize writes to the JCR.
     * If you apply this on nodes with type <code>sling:OsgiConfig</code>, this ensures the nodes are only touched when there are actual changes, ensuring services
     * using these configurations do not restart for no reason.
     * After updates are done, any changed properties are tracked in the <code>config:managedProps</code> and the <code>config:lastUpdate</code> is updated.
     * This allows both JCR level inspection of what this service is doing and provides persisted meta-data for this service.
     *
     * @param configurationService the {@see ConfigurationService} to use, not null.
     * @param sessionTemplate      the {@see JcrSessionTemplate} to use, not null.
     * @param refresh              if <code>true</code> will re-fetch the configuration from the configuration service, if <code>false</code>, will used a cached version if available.
     * @return <code>true</code> if any properties were updated on the node.
     * @throws ConfigurationException when configuring the node failed.
     * @throws IllegalStateException  when the managed node can not be accessed. This could happen is the node gets deleted by another process.
     */
    public boolean configure(ConfigurationService configurationService, JcrSessionTemplate sessionTemplate, boolean refresh) throws ConfigurationException, IllegalStateException {
        Assert.notNull(configurationService);
        Assert.notNull(sessionTemplate);
        try {
            logger.info("Configuring node {} ", nodePath);
            //noinspection unchecked
            return (Boolean) sessionTemplate.executeWithResult(new JcrSessionTemplate.Callback() {
                @Override
                public Object execute(Session session) throws Exception {
                    Node node = session.getNode(getNodePath());
                    if (node != null) {
                        PlaceHolderAwareNode wrapper = new PlaceHolderAwareNode(node);
                        if (!refresh || configurationService.refresh(node.getName(), profiles)) {
                            Properties remoteProperties = configurationService.getProperties(configurationName, profiles);
                            if (!CollectionUtils.isEmpty(remoteProperties)) {
                                logger.info("Configuration has been changed for '{}', updating node...", ManagedConfiguration.this.toString());
                                //only do work if we have actual properties...
                                if (wrapper.resolvePlaceholders(remoteProperties)) {
                                    //updated some props...
                                    session.save();
                                    return Boolean.TRUE;
                                } //else: nothing changed...
                            } else {
                                logger.warn("No properties found at configuration service for '{}'", ManagedConfiguration.this.toString());
                            }
                        } else {
                            logger.info("Configuration not changed for '{}'", ManagedConfiguration.this.toString());
                        }
                    } else {
                        throw new IllegalStateException(String.format("Could not find node at '%s', maybe node was deleted?", getNodePath()));
                    }
                    return Boolean.FALSE;
                }
            });
        } catch (Exception e) {
            throw new ConfigurationException("Error configuring managed node: " + ManagedConfiguration.this.toString(), e);
        }

    }

    public String getNodePath() {
        return nodePath;
    }

    public String getConfigurationName() {
        return configurationName;
    }

    @Override
    public int hashCode() {
        return nodePath.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        //the node path defines the identity of the object
        if (o instanceof ManagedConfiguration) {
            ManagedConfiguration other = (ManagedConfiguration) o;
            return this.nodePath != null && other.getNodePath() != null && this.nodePath.equals(other.getNodePath());
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("[Managed Configuration: '%s' (%s:%s)]",
                nodePath, configurationName, StringUtils.arrayToCommaDelimitedString(profiles));
    }
}
