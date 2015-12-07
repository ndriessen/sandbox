package com.bose.services.config.client.aem;

import java.util.Date;

/**
 * Simple VO to track some meta data concerning our managed configurations.
 *
 */
public class ManagedConfiguration {
    private String nodePath;
    private Date lastUpdated;

    public ManagedConfiguration(String nodePath) {
        this.nodePath = nodePath;
        this.setLastUpdated();
    }

    public String getNodePath() {
        return nodePath;
    }

    public void setNodePath(String nodePath) {
        this.nodePath = nodePath;
        this.setLastUpdated();
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    private void setLastUpdated() {
        this.lastUpdated = new Date();
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        //the node path defines the identity of the object
        if(o instanceof ManagedConfiguration){
            ManagedConfiguration other = (ManagedConfiguration) o;
            return this.nodePath != null && other.getNodePath()!= null && this.nodePath.equals(other.getNodePath());
        }
        return false;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
