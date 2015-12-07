package com.bose.services.config.client.aem;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.api.SlingRepository;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Template for working with a JCR session.
 * This template will handle login/logout and saving of the session transparantly.
 */
public class JcrSessionTemplate<T> {
    private ResourceResolverFactory resourceResolverFactory;
    private SlingRepository repository;

    public JcrSessionTemplate(ResourceResolverFactory resourceResolverFactory) {
        if(resourceResolverFactory == null) {
            throw new IllegalArgumentException("ResourceResolverFactory can not be <null>.");
        }
        this.resourceResolverFactory = resourceResolverFactory;
    }

    public JcrSessionTemplate(SlingRepository repository) {
        if(repository == null) {
            throw new IllegalArgumentException("SlingRepository can not be <null>.");
        }
        this.repository = repository;
    }

    private Session createSession() throws RepositoryException, LoginException {
        if(resourceResolverFactory != null) {
            return resourceResolverFactory.getAdministrativeResourceResolver(null).adaptTo(Session.class);
        } else if(repository != null) {
            return repository.loginAdministrative(null);
        }
        throw new IllegalStateException("Both SlingRepository and ResourceResolverFactory are null, can not create session");
    }

    /**
     * Execute the provided callback.
     *
     * Note that you have to handle saving the session yourself.
     *
     * @see #executeWithResult(Callback, boolean)
     * @param callback the callback to executeWithResult.
     * @return the result of the callback.
     * @throws Exception when the callback throws an exception, this is propagated.
     */
    public T executeWithResult(Callback<T> callback) throws Exception {
        return executeWithResult(callback, false);
    }

    /**
     * Execute the provided callback, optionally saving the session.
     *
     * Note that you have to handle saving the session yourself.
     *
     * @see #executeWithResult(Callback, boolean)
     * @param callback the callback to executeWithResult.
     * @param saveSession flag to indicate whether the template needs to save the session for you.
     * @throws Exception when the callback throws an exception, this is propagated.
     */
    public void execute(Callback<T> callback, boolean saveSession) throws Exception {
        executeWithResult(callback, saveSession);
    }

    /**
     * Execute the provided callback.
     *
     * Note that you have to handle saving the session yourself.
     *
     * @see #executeWithResult(Callback, boolean)
     * @param callback the callback to executeWithResult.
     * @throws Exception when the callback throws an exception, this is propagated.
     */
    public void execute(Callback<T> callback) throws Exception {
        executeWithResult(callback);
    }


    /**
     * Executes the given callback, providing it with a fresh session, and handling
     * closing and optionally saving the session.
     *
     * This will create a new adminstrative session, and will handle login, logout, and optionally saving
     * the session in all scenarios. The callback method can assume the session is valid and will be cleaned up,
     * also when exceptions occur in the callback.
     * This method will return any value returned from the callback, and will let any exception thrown be propagated back to the caller.
     *
     * @param callback the callback that contains your code that needs a JCR session.
     * @param saveSession boolean indicating whether this method needs to save your session before logging out.
     * @return any value returned from the callback
     * @throws Exception any exception thrown by the callback
     * @throws LoginException when the session could not be created
     */
    public T executeWithResult(Callback<T> callback, boolean saveSession) throws Exception {
        Session session = null;
        try {
            session = createSession();
            T result = callback.execute(session);
            if(saveSession) {
                session.save();
            }
            return result;
        } finally {
            try {
                if(session != null) {
                    session.logout();
                }
            } catch (Throwable e) {
                //ignore this, we tried our best to cleanup...
            }
        }
    }

    /**
     * Callback for working with a JCR session.
     *
     * @param <T> the type of the return value of the callback.
     */
    public interface Callback<T> {
        T execute(Session session) throws Exception;
    }
}
