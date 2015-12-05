package com.bose.services.config.client.aem;

import javax.jcr.observation.EventListener;

/**
 * {@see EventListener} sub interface that defines some additional methods so the listener can define it's own
 * event filtering parameters.
 *
 * @author Niki Driessen
 */
public interface ConfigEventListener extends EventListener {
    /**
     * Return the {@see javax.jcr.observation.Event} constants using bitwise or | to combine them.
     * This will be used to register the listener so the listener can determine how to filter on event type.
     *
     * @return and int value specifying the combination of event types to listen to.
     */
    int getEventTypes();
}
