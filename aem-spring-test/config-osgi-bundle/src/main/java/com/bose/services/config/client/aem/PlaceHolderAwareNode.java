package com.bose.services.config.client.aem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.StringUtils;

import javax.jcr.*;
import javax.jcr.nodetype.NodeType;
import java.util.*;

/**
 * Helper class for managing configuration placeholders in a JCR {@see Node}.
 */
public class PlaceHolderAwareNode {
    private static final Logger logger = LoggerFactory.getLogger(PlaceHolderAwareNode.class);
    private static final PropertyPlaceholderHelper placeholderHelper = new PropertyPlaceholderHelper("${", "}", ":", true);
    private Node node;
    private String nodePath;

    /**
     * Creates a new wrapper for the provided node that can be used to manage placeholders in the node's property values.
     *
     * @param node the JCR node to wrap.
     * @throws RepositoryException when creating the wrapper fails.
     */
    public PlaceHolderAwareNode(Node node) throws RepositoryException {
        this.node = node;
        this.nodePath = node.getPath();
    }

    private boolean hasMixin() throws RepositoryException {
        NodeType[] mixins = node.getMixinNodeTypes();
        for (NodeType mixin : mixins) {
            if (mixin.getName().equalsIgnoreCase(ManagedConfigurationMixin.NODE_TYPE)) {
                return true;
            }
        }
        return false;
    }

    public String[] getAdditionalProfilesProperty() throws RepositoryException {
        if (node != null && node.hasProperty(ManagedConfigurationMixin.PROPERTY_ADDITIONAL_PROFILES)) {
            return PropertyUtils.getPropertyAsArray(node.getProperty(ManagedConfigurationMixin.PROPERTY_ADDITIONAL_PROFILES));
        }
        return new String[0];
    }

    protected void restorePlaceHolders() throws RepositoryException {
        if (node.hasProperty(ManagedConfigurationMixin.PROPERTY_MANAGED_PROPS)) {
            String[] value = PropertyUtils.getPropertyAsArray(node.getProperty(ManagedConfigurationMixin.PROPERTY_MANAGED_PROPS));
            if (value != null) {
                for (String entry : value) {
                    String[] parts = entry.split("=");
                    if (parts.length == 2) {
                        String key = parts[0];
                        String[] placeHolders = parts[1].split(",");
                        if (placeHolders.length == 1) {
                            node.setProperty(key, placeHolders[0]);
                        } else {
                            node.setProperty(key, placeHolders);
                        }
                    } else {
                        logger.error("Illegal format in property " + ManagedConfigurationMixin.PROPERTY_MANAGED_PROPS + ", cannot parse entry: " + entry);
                    }
                }
            }
            node.setProperty(ManagedConfigurationMixin.PROPERTY_MANAGED_PROPS, new String[0]);
        }
    }

    /**
     * Resolves all placeholders in the node properties, using the provided properties.
     *
     * @param properties the properties to use for placeholder resolution.
     * @return <code>true</code> if any placeholders where resolved, <code>false</code> otherwise.
     * @throws ConfigurationException When placeholder resolution fails.
     */
    public boolean resolvePlaceholders(Properties properties) throws ConfigurationException {
        try {
            if (!node.hasProperties()) return false;
            logger.info("Checking node '{}' for configuration placeholders", node.getPath());

            this.restorePlaceHolders();
            //and now run a placeholder resolution process again... note that we don't track the above as "changed" made, so they are
            //reversed if we don't do anything else below that changes the nodes.
            PropertyIterator nodeProperties = node.getProperties();
            Set<String> changedProps = new HashSet<>();
            while (nodeProperties.hasNext()) {
                Property property = nodeProperties.nextProperty();
                try {
                    if (accept(property)) {
                        if (property.isMultiple()) {
                            Value[] values = property.getValues();
                            String[] newValues = new String[values.length];
                            String[] oldValues = new String[values.length];
                            int i = 0;
                            for (Value valueObj : values) {
                                String value = valueObj.getString();
                                oldValues[i] = value;
                                String newValue = placeholderHelper.replacePlaceholders(value, properties);
                                if (!value.equalsIgnoreCase(newValue)) {
                                    property.setValue(newValue);
                                }
                                newValues[i++] = newValue;
                            }
                            property.setValue(newValues);
                            changedProps.add(property.getName() + "=" + StringUtils.arrayToCommaDelimitedString(oldValues));
                            logger.info("Replaced '{}' by '{}' for node property '{}'", new Object[]{oldValues, newValues, property.getPath()});
                        } else {
                            String value = property.getString();
                            String newValue = placeholderHelper.replacePlaceholders(value, properties);
                            if (!value.equalsIgnoreCase(newValue)) {
                                property.setValue(newValue);
                                changedProps.add(property.getName() + "=" + value);
                                logger.info("Replaced '{}' by '{}' for node property '{}'", new Object[]{value, newValue, property.getPath()});
                            }
                        }
                    }
                } catch (Exception e) {
                    throw new ConfigurationException("Error while trying to resolve placeholders in node '{}', property '{}'", e, nodePath, property.getName());
                }
            }
            boolean changed = !CollectionUtils.isEmpty(changedProps);
            if (changed) {
                if (!hasMixin()) {
                    node.addMixin(ManagedConfigurationMixin.NODE_TYPE);
                }
                //we changed something, update metadata
                node.setProperty(ManagedConfigurationMixin.PROPERTY_MANAGED_PROPS, changedProps.toArray(new String[changedProps.size()]));
                Calendar cal = Calendar.getInstance();
                cal.setTime(new Date());
                node.setProperty(ManagedConfigurationMixin.PROPERTY_LAST_UPDATE, cal);
            }
            return changed;
        } catch (Exception e) {
            throw new ConfigurationException("Error while resolving placeholders in node '%s'", e, this.nodePath);
        }
    }

    /**
     * Filters out properties that are not supported for placeholder resolution.
     * <p>
     * This includes any property whose name starts with 'jcr:', 'cq:'
     * or '{@see ManagedConfigurationMixin.PREFIX}'.
     *
     * @param property the property to filter, not null.
     * @return <code>true</code> if the property is electable for placeholder resolution.
     * @throws RepositoryException when an error occurs accessing the JCR property.
     */
    protected boolean accept(Property property) throws RepositoryException {
        return !(property.getName().startsWith("jcr:") || property.getName().startsWith("cq:")
                || property.getName().startsWith(ManagedConfigurationMixin.PREFIX));
    }
}
