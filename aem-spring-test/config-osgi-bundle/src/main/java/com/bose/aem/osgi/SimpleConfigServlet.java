package com.bose.aem.osgi;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

/**
 * Created by niki on 29/11/15.
 */
//@Service
@SlingServlet(paths = {"/bin/config/info"})
public class SimpleConfigServlet extends SlingAllMethodsServlet {
    @Reference
    ConfigurationService configurationService;

    public void bindConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }


    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        /*This code just starts a dummy job*/
        PrintWriter writer = response.getWriter();
        writer.println("<ul>");
        for (Map.Entry<String, String> entry : configurationService.getProperties().entrySet()) {
            writer.println(String.format("<li><strong>%s:</strong> %s</li>", entry.getKey(), entry.getValue()));
        }
        writer.println("</ul>");
    }
}
