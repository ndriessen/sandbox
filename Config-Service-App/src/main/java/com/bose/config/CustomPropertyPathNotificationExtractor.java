package com.bose.config;

import org.springframework.cloud.config.monitor.PropertyPathNotification;
import org.springframework.cloud.config.monitor.PropertyPathNotificationExtractor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Created by niki on 24/11/15.
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 500)
public class CustomPropertyPathNotificationExtractor implements PropertyPathNotificationExtractor {
    @Override
    public PropertyPathNotification extract(MultiValueMap<String, String> headers,
                                            Map<String, Object> request) {
        Object object = request.get("path");
        if (object instanceof String) {
            return new PropertyPathNotification(sanitize((String) object));
        }
        if (object instanceof Collection) {
            @SuppressWarnings("unchecked")
            Collection<String> input = (Collection<String>) object;
            Collection<String> output = new ArrayList<>();
            for (String s : input) {
                output.add(sanitize(s));
            }
            return new PropertyPathNotification(output.toArray(new String[0]));
        }
        return null;
    }

    private String sanitize(String input) {
        String stem = StringUtils
                .stripFilenameExtension(StringUtils.getFilename(input));
        int dash = stem.indexOf("-");
        if (dash > 0) {
            stem = stem.substring(0, dash);
        }
        return stem;
    }

}
