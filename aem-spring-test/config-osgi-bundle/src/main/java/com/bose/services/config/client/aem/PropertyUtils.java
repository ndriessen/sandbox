package com.bose.services.config.client.aem;

import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.ComponentContext;
import org.springframework.util.Assert;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.util.*;

/**
 * Various utility methods for handling properties.
 */
public class PropertyUtils {

    public static String getProperty(String name, ComponentContext context, String defaultValue) {
        Assert.notNull(name);
        Assert.notNull(context);
        return PropertiesUtil.toString(context.getProperties().get(name), defaultValue);
    }

    public static String[] getPropertyAsArray(String name, ComponentContext context, String[] defaultValue) {
        Assert.notNull(name);
        Assert.notNull(context);
        return PropertiesUtil.toStringArray(context.getProperties().get(name), defaultValue);
    }

    public static String[] getPropertyAsArray(Property property) throws RepositoryException {
        Assert.notNull(property);
        if (property.isMultiple()) {
            Value[] values = property.getValues();
            List<String> result = new ArrayList<>();
            for (Value value : values) {
                result.add(value.getString());
            }
            return result.toArray(new String[result.size()]);
        } else {
            return new String[]{property.getString()};
        }
    }

    public static Set<String> getPropertyAsSet(String name, ComponentContext context, String[] defaultValue) {
        Assert.notNull(name);
        Assert.notNull(context);
        Set<String> collection = new HashSet<>();
        Collections.addAll(collection, PropertiesUtil.toStringArray(context.getProperties().get(name), defaultValue));
        return collection;
    }

}
