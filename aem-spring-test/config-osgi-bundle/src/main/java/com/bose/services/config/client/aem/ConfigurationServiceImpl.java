package com.bose.services.config.client.aem;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.settings.SlingSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component(immediate = true, name = "com.bose.services.config.client.aem.ConfigurationService")
@Service(ConfigurationService.class)
public class ConfigurationServiceImpl implements ConfigurationService {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationServiceImpl.class);
    private static final String CONFIG_SERVER_URL = "http://localhost:8888/%s/%s";
    private static final String DEFAULT_PROFILE = "default"; //only used if no runmodes are active

    @Reference
    private SlingSettingsService slingSettings;
    private Set<String> profiles;
    private Map<String, Properties> configurationCache;

    public ConfigurationServiceImpl() {
    }

    public void bindSlingSettings(SlingSettingsService slingSettingsService) {
        this.slingSettings = slingSettingsService;
    }

    public void unbindSlingSettings(SlingSettingsService slingSettingsService) {
        this.slingSettings = null;
    }

    @Activate
    public void activate() {
        this.configurationCache = new ConcurrentHashMap<>();
        this.profiles = new HashSet<>();
        if (this.slingSettings != null) {
            this.profiles = this.slingSettings.getRunModes();
            logger.info("Setting base configuration profiles to runmode list: {}", StringUtils.collectionToCommaDelimitedString(this.profiles));
        }
    }

    protected List<String> getFinalProfiles(String... additionalProfiles) {
        List<String> result = new ArrayList<>();
        result.addAll(this.profiles);
        //add additional as last so they are always "more specific"
        if (additionalProfiles != null) {
            for (String profile : additionalProfiles) {
                if (!result.contains(profile)) {
                    result.add(profile);
                }
            }
        }
        if (CollectionUtils.isEmpty(result)) {
            result.add(DEFAULT_PROFILE);
        }
        return result;
    }

    @Override
    public Properties getProperties(String name, String... additionalProfiles) {
        String cacheKey = name + "#" + StringUtils.arrayToCommaDelimitedString(additionalProfiles);
        if (!this.configurationCache.containsKey(cacheKey)) {
            this.configurationCache.put(cacheKey, getConfiguration(name, additionalProfiles));
        }
        return this.configurationCache.get(cacheKey);
    }

    protected Properties getConfiguration(String name, String... additionalProfiles) {
        Properties dictionary = new Properties();
        RestTemplate restTemplate = new RestTemplate();
        String profileList = StringUtils.collectionToCommaDelimitedString(getFinalProfiles(additionalProfiles));
        logger.info("Querying service for configuration with name '{}' and profiles '{}'", name, profileList);
        RemoteConfig response = restTemplate.getForObject(
                String.format(CONFIG_SERVER_URL, name, profileList),
                RemoteConfig.class);
        if (response != null) {
            Map<String, String> properties = new HashMap<>();
            for (PropertySource source : response.getPropertySources()) {
                @SuppressWarnings("unchecked")
                Map<String, String> map = (Map<String, String>) source
                        .getSource();
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    if (!properties.containsKey(entry.getKey())) {
                        properties.putIfAbsent(entry.getKey(), entry.getValue());
                    }
                }

            }
            dictionary.putAll(properties);
            logger.info("Retrieved {} properties for '{}': ", properties.size(), name);
            if (logger.isDebugEnabled()) {
                for (String key : properties.keySet()) {
                    logger.debug("** {} = {}", key, properties.get(key));
                }
            }
        }
        return dictionary;
    }
}
