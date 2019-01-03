package com.projecta.monsai.saiku;

import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;
import org.apache.log4j.Logger;
import org.saiku.service.ISessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.projecta.monsai.config.Config;

/**
 * Implementation of the Saiku {@link ISessionService} that uses an external URL
 * to allow or deny requests to Saiku.
 */
public class MonsaiSessionService implements ISessionService {

    private Cache<String, Map<String, Object>> sessionCache;
    private String saikuAuthorizationUrl;
    private String saikuUsername;
    private String saikuPassword;

    @Autowired
    private Config config;

    private static final String SAIKU_SESSION       = "saiku-session";
    private static final String SAIKU_USER_HEADER   = "saiku-user";
    private static final int MAX_CACHE_SIZE         = 10000;
    private static final int CACHE_TIME_MINUTES     = 30;
    private static final int REQUEST_TIMEOUT_MILLIS = 30000;

    private static final Logger LOG = Logger.getLogger(MonsaiSessionService.class);


    @PostConstruct
    public void init() {

        saikuAuthorizationUrl = StringUtils.trimToNull(config.getProperty("saikuAuthorizationUrl"));
        saikuUsername         = StringUtils.trimToNull(config.getProperty("saikuUsername"));
        saikuPassword         = StringUtils.trimToNull(config.getProperty("saikuPassword"));

        sessionCache = CacheBuilder.newBuilder().maximumSize(MAX_CACHE_SIZE)
                .expireAfterWrite(CACHE_TIME_MINUTES, TimeUnit.MINUTES).build();
    }


    /**
     * Checks if a request is authenticated
     */
    public boolean authenticateRequest(HttpServletRequest request, HttpServletResponse response, boolean refresh) {

        // if a fixed username/password is set, we are always authenticated
        if (saikuUsername != null) {
            return true;
        }

        return getSession(request, response, refresh) != null;
    }


    /**
     * Creates a new session with the given user name
     *
     * @param response2
     */
    private Map<String, Object> getSession(HttpServletRequest request, HttpServletResponse response, boolean refresh) {

        // retrieve the username from the HTTP request header
        if (request == null) {
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            request = requestAttributes.getRequest();
        }

        // check if we have a HTTP session
        if (saikuUsername != null) {
            return (Map<String, Object>) request.getSession().getAttribute(SAIKU_SESSION);
        }

        String userName = StringUtils.defaultString(request.getHeader(SAIKU_USER_HEADER));

        // check the cache if we already have a session for this user
        Map<String, Object> session = null;
        if (!refresh) {
            session = sessionCache.getIfPresent(userName);
            if (session != null) {
                return session;
            }
        }

        // build a session object
        session = new HashMap<String, Object>();
        session.put("username", StringUtils.defaultIfEmpty(userName, "anonymous"));
        session.put("sessionid", UUID.randomUUID().toString());
        session.put("roles", Collections.emptyList());

        // when authorisation url is not set, requests are always authorized
        if (saikuAuthorizationUrl == null) {
            sessionCache.put(userName, session);
            return session;
        }

        if (StringUtils.isEmpty(userName)) {
            LOG.info(SAIKU_USER_HEADER + " not set, access denied");
            return null;
        }

        // call the authorization url
        Map<String, Object> authResponse = doAuthorizationRequest(userName);
        if (authResponse == null || authResponse.get("allowed") == null) {
            LOG.info("No valid authorization response, access denied to " + userName);
            sessionCache.invalidate(userName);
            return null;
        }

        if (!Objects.equals(authResponse.get("allowed"), Boolean.TRUE)) {

            LOG.info("Access denied to " + userName);
            sessionCache.invalidate(userName);

            if (response != null) {
                response.setContentType("text/plain");
                response.setCharacterEncoding("UTF-8");

                try (OutputStream out = response.getOutputStream()) {
                    out.write(("Sorry, but you don't have enough permissions to use Saiku.\n"
                            + "If you think that this is a mistake, please contact "
                            + "the person that gave you the link to this page.").getBytes("UTF-8"));
                }
                catch (Throwable e) {
                    // ignore errors
                }
            }

            return null;
        }

        sessionCache.put(userName, session);
        return session;
    }


    /**
     * Calls the configured authorization URL and parses the response as JSON
     */
    private Map<String, Object> doAuthorizationRequest(String userName) {

        try {
            // call the configured url
            Request request = Request.Post(saikuAuthorizationUrl)
                    .bodyForm(Form.form().add("username", userName).build()).connectTimeout(REQUEST_TIMEOUT_MILLIS)
                    .socketTimeout(REQUEST_TIMEOUT_MILLIS);

            String responseString = request.execute().returnContent().asString();

            // parse the JSON repsponse
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(responseString, Map.class);
        }
        catch (Throwable e) {
            // any error means that the authorization failed
            LOG.error("Error calling " + saikuAuthorizationUrl, e);
            return null;
        }
    }


    /**
     * Retrieves the current session to check for a login
     */
    @Override
    public Map<String, Object> getSession() {
        Map<String, Object> session = getSession(null, null, true);
        return session != null ? session : new HashMap<String, Object>();
    }


    /**
     * Retrieves the current session information
     */
    @Override
    public Map<String, Object> getAllSessionObjects() {
        Map<String, Object> session = getSession(null, null, false);
        return session != null ? session : new HashMap<String, Object>();
    }


    /**
     * Create a session with username and password
     */
    @Override
    public Map<String, Object> login(HttpServletRequest request, String userName, String password) {

        if (saikuUsername == null) {
            return getSession(request, null, true);
        }

        // check successful login against fixed credentials
        if (!StringUtils.equals(userName, saikuUsername) || !StringUtils.equals(password, saikuPassword)) {
            return null;
        }

        // build a session object
        Map<String, Object> session = new HashMap<String, Object>();
        session.put("username",  userName);
        session.put("sessionid", UUID.randomUUID().toString());
        session.put("roles",     Collections.emptyList());
        sessionCache.put(userName, session);

        request.getSession().setAttribute(SAIKU_SESSION, session);
        return session;
    }


    /**
     * Logs out the current user
     */
    @Override
    public void logout(HttpServletRequest request) {
        request.getSession().removeAttribute(SAIKU_SESSION);
    }

    @Override
    public void authenticate(HttpServletRequest request, String userName, String password) {
        login(request, userName, password);
    }

    @Override
    public void clearSessions(HttpServletRequest req, String username, String password) throws Exception {
    }

}