package com.bose.aem.spring.model;

import io.neba.api.annotations.Children;
import io.neba.api.annotations.ResourceModel;
import org.apache.sling.api.resource.Resource;

import java.util.List;

@ResourceModel(types = "cq:Page")
public class PageModel {
    @Children
    private List<Resource> children;

    public List<Resource> getChildren() {
        return this.children;
    }
}
