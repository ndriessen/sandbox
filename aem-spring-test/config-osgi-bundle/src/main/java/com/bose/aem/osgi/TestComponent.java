package com.bose.aem.osgi;

import aQute.bnd.annotation.component.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;

import java.util.Map;

/**
 * Created by niki on 29/11/15.
 */
@Component(immediate = true)
public class TestComponent {
    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    private ResourceResolver resourceResolver;
    private ResourceResolver administrativeResourceResolver;

    @Activate
    public void activate() throws Exception {
        this.resourceResolver = resourceResolverFactory.getResourceResolver(null);
        this.administrativeResourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
    }

    public void bindResourceResolverFactory(ResourceResolverFactory resourceResolverFactory){
        this.resourceResolver = resourceResolver;
    }

    public void unbindResourceResolverFactory(){
        this.resourceResolver = null;
    }
}
