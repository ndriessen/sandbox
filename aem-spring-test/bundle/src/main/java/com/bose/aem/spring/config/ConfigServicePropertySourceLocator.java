/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bose.aem.spring.config;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

/**
 * @author Dave Syer
 */
@Order(Ordered.LOWEST_PRECEDENCE)
public class ConfigServicePropertySourceLocator implements PropertySourceLocator {
    private org.springframework.core.env.Environment environment;

    //private static Log logger = LogFactory.getLog(ConfigServicePropertySourceLocator.class);

    private RestTemplate restTemplate;
    private ConfigClientProperties defaults;

    public ConfigServicePropertySourceLocator(org.springframework.core.env.Environment environment) {
        this.defaults = new ConfigClientProperties(environment);
    }

    @Override
    public org.springframework.core.env.PropertySource<?> locate(
            org.springframework.core.env.Environment environment) {
        ConfigClientProperties client = this.defaults.override(environment);
        CompositePropertySource composite = new CompositePropertySource("configService");
        RestTemplate restTemplate = this.restTemplate == null ? getSecureRestTemplate(client)
                : this.restTemplate;
        Exception error = null;
        String errorBody = null;
        //logger.info("Fetching config from server at: " + client.getRawUri());
        try {
            String[] labels = new String[]{""};
            if (StringUtils.hasText(client.getLabel())) {
                labels = StringUtils.commaDelimitedListToStringArray(client.getLabel());
            }
            // Try all the labels until one works
            for (String label : labels) {
                Environment result = getRemoteEnvironment(restTemplate, client.getRawUri(), client.getName(), client.getProfile(), label.trim());
                if (result != null) {
//                    logger.info(String.format("Located environment: name=%s, profiles=%s, label=%s, version=%s",
//                            result.getName(),
//                            result.getProfiles() == null ? "" : Arrays.asList(result.getProfiles()),
//                            result.getLabel(), result.getVersion()));

                    for (PropertySource source : result.getPropertySources()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) source
                                .getSource();
                        composite.addPropertySource(new MapPropertySource(source
                                .getName(), map));
                    }
                    return composite;
                }
            }
        } catch (HttpServerErrorException e) {
            error = e;
            if (MediaType.APPLICATION_JSON.includes(e.getResponseHeaders()
                    .getContentType())) {
                errorBody = e.getResponseBodyAsString();
            }
        } catch (Exception e) {
            error = e;
        }
        if (client != null && client.isFailFast()) {
            throw new IllegalStateException(
                    "Could not locate PropertySource and the fail fast property is set, failing",
                    error);
        }
//        logger.warn("Could not locate PropertySource: "
//                + (errorBody == null ? error == null ? "label not found" : error.getMessage() : errorBody));
        return null;

    }

    private Environment getRemoteEnvironment(RestTemplate restTemplate, String uri, String name, String profile, String label) {
        String path = "/{name}/{profile}";
        Object[] args = new String[]{name, profile};
        if (StringUtils.hasText(label)) {
            args = new String[]{name, profile, label};
            path = path + "/{label}";
        }
        ResponseEntity<Environment> response = null;

        try {
            response = restTemplate.exchange(uri + path,
                    HttpMethod.GET, new HttpEntity<Void>((Void) null),
                    Environment.class, args);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() != HttpStatus.NOT_FOUND) {
                throw e;
            }
        }

        if (response == null || response.getStatusCode() != HttpStatus.OK) {
            return null;
        }
        Environment result = response.getBody();
        return result;
    }

    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private RestTemplate getSecureRestTemplate(ConfigClientProperties client) {
        RestTemplate template = new RestTemplate();
        String password = client.getPassword();
        if (password != null) {
            template.setInterceptors(Arrays
                    .<ClientHttpRequestInterceptor>asList(new BasicAuthorizationInterceptor(
                            client.getUsername(), password)));
        }
        return template;
    }

    private static class BasicAuthorizationInterceptor implements
            ClientHttpRequestInterceptor {

        private final String username;

        private final String password;

        public BasicAuthorizationInterceptor(String username, String password) {
            this.username = username;
            this.password = (password == null ? "" : password);
        }

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                            ClientHttpRequestExecution execution) throws IOException {
            byte[] token = java.util.Base64.getEncoder()
                    .encode((this.username + ":" + this.password).getBytes());
            request.getHeaders().add("Authorization", "Basic " + new String(token));
            return execution.execute(request, body);
        }

    }

}

