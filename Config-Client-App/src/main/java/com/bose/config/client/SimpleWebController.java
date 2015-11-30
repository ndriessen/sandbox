package com.bose.config.client;

import com.bose.config.restclient.ConfigurationClient;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
public class SimpleWebController {
    private ConfigurationClient configurationClient;

    public SimpleWebController() {
        this.configurationClient = new ConfigurationClient("tax", null);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/test/{profiles}/")
    @ResponseBody
    public Object test(@PathVariable String profiles) {
        return configurationClient.getConfiguration(StringUtils.commaDelimitedListToStringArray(profiles));
    }
}
