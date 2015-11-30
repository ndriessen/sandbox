package com.bose.aem.osgi;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.settings.SlingSettingsService;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component(immediate = true, name = "com.bose.aem.osgi.ConfigurationService")
@Service
public class ConfigurationServiceImpl implements ConfigurationService {
    public static final String CONFIG_NAME = "tax";
    public static final String CONFIG_SERVER_URL = "http://localhost:8888";

    @Reference
    private SlingSettingsService slingSettings;

    private String[] profiles;

    private Map<String, String> configurationCache;

    public ConfigurationServiceImpl() {
        this.configurationCache = new HashMap<>(getConfiguration("dev", "us"));
    }

    @Activate
    public void activate() {
        if (this.slingSettings != null) {
            Set<String> runModes = this.slingSettings.getRunModes();
            this.profiles = runModes.toArray(new String[runModes.size()]);
        } else {
            this.profiles = new String[]{"default"};
        }
        this.configurationCache = new HashMap<>(getConfiguration(this.profiles));
    }

    @Override
    public Map<String, String> getProperties() {
        return configurationCache;
    }

    protected Map<String, String> getConfiguration(String... profiles) {
        RestTemplate restTemplate = new RestTemplate();
        RemoteConfig response = restTemplate.getForObject(
                String.format(CONFIG_SERVER_URL, CONFIG_NAME, StringUtils.arrayToCommaDelimitedString(profiles)),
                RemoteConfig.class);

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
        return properties;
    }

    @Override
    public String getProperty(String name, String... profiles) {
        return this.configurationCache.get(name);
    }

    @Override
    public String getRequiredProperty(String name, String... profiles) {
        String value = this.configurationCache.get(name);
        if (StringUtils.isEmpty(value)) throw new IllegalArgumentException("No property with name " + name);
        return value;
    }
}
