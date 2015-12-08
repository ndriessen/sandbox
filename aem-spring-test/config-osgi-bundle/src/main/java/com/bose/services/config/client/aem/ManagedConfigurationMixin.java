package com.bose.services.config.client.aem;

import javax.jcr.NamespaceRegistry;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.nodetype.PropertyDefinitionTemplate;

/**
 * Class to define the <code>config:managed</code> JCR mixin type.
 *
 * @author Niki Driessen
 */
public final class ManagedConfigurationMixin {
    private static final String NS = "config";
    private static final String NAMESPACE_URL = "http://www.bose.com/jcr/config";
    public static final String PREFIX = NS + ":";
    /**
     * The node type definition. This resolves to the combination of {@link #PREFIX} and <code>managed</code>.
     * This defaults to <code>config:managed</code>.
     */
    public static final String NODE_TYPE = PREFIX + "managed";
    public static final String PROPERTY_MANAGED_PROPS = PREFIX + "managedProps";
    public static final String PROPERTY_LAST_UPDATE = PREFIX + "lastUpdate";
    public static final String PROPERTY_ADDITIONAL_PROFILES = PREFIX + "additionalProfiles";
    public static final String PROPERTY_CONFIG_NAME = PREFIX + "configName";

    @SuppressWarnings("unchecked")
    public static void registerMixin(Session session) throws ConfigurationException {
        try {
            NodeTypeManager manager = session.getWorkspace().getNodeTypeManager();
            NamespaceRegistry ns = session.getWorkspace().getNamespaceRegistry();
            ns.registerNamespace(NS, NAMESPACE_URL);
            // Create node type
            NodeTypeTemplate nodeTypeTemplate = manager.createNodeTypeTemplate();
            nodeTypeTemplate.setName(NODE_TYPE);
            nodeTypeTemplate.setMixin(true);
            nodeTypeTemplate.setQueryable(true);

            // Create a new property
            PropertyDefinitionTemplate managedPropsDefinition = manager.createPropertyDefinitionTemplate();
            managedPropsDefinition.setName(PROPERTY_MANAGED_PROPS);
            managedPropsDefinition.setMultiple(true);
            managedPropsDefinition.setMandatory(false);
            // Create a new property
            PropertyDefinitionTemplate configNameDefinition = manager.createPropertyDefinitionTemplate();
            configNameDefinition.setName(PROPERTY_CONFIG_NAME);
            configNameDefinition.setMultiple(false);
            configNameDefinition.setMandatory(false);
            //managedPropsDefinition.setProtected(true);
            managedPropsDefinition.setRequiredType(PropertyType.STRING);
            PropertyDefinitionTemplate lastUpdateDefinition = manager.createPropertyDefinitionTemplate();
            lastUpdateDefinition.setName(PROPERTY_LAST_UPDATE);
            lastUpdateDefinition.setMultiple(false);
            lastUpdateDefinition.setMandatory(false);
            //managedPropsDefinition.setProtected(true);
            lastUpdateDefinition.setRequiredType(PropertyType.DATE);
            PropertyDefinitionTemplate additionalProfilesDefinition = manager.createPropertyDefinitionTemplate();
            additionalProfilesDefinition.setName(PROPERTY_ADDITIONAL_PROFILES);
            additionalProfilesDefinition.setMultiple(true);
            additionalProfilesDefinition.setMandatory(false);
            additionalProfilesDefinition.setRequiredType(PropertyType.STRING);
            // Add property to node type
            nodeTypeTemplate.getPropertyDefinitionTemplates().add(managedPropsDefinition);
            nodeTypeTemplate.getPropertyDefinitionTemplates().add(lastUpdateDefinition);
            nodeTypeTemplate.getPropertyDefinitionTemplates().add(additionalProfilesDefinition);
            nodeTypeTemplate.getPropertyDefinitionTemplates().add(configNameDefinition);
            /* Register node type */
            manager.registerNodeType(nodeTypeTemplate, true);
            session.save();
        } catch (RepositoryException e) {
            throw new ConfigurationException("Error registering custom mixin type. Configuration management will not work!", e);
        }
    }
}
