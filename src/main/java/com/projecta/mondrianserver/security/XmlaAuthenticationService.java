package com.projecta.mondrianserver.security;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;
//import org.apache.log4j.Logger;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.projecta.mondrianserver.config.Config;

import mondrian.util.Base64;

/**
 * Service for authorization of requests to the xmla servlet, using an external
 * url
 */
@Component
public class XmlaAuthenticationService {

    private Cache<String, CubeAccess> authorizationCache;
    private String xmlaAuthorizationUrl;
    private String xmlaUsername;
    private String xmlaPassword;

    @Autowired private Config config;

    private static final int MAX_CACHE_SIZE         = 10000;
    private static final int CACHE_TIME_MINUTES     = 30;
    private static final int REQUEST_TIMEOUT_MILLIS = 30000;

    private static final Logger LOG = LogManager.getLogger(XmlaAuthenticationService.class);

    @PostConstruct
    public void init() {

        xmlaAuthorizationUrl = StringUtils.trimToNull(config.getProperty("xmlaAuthorizationUrl"));
        xmlaUsername = StringUtils.trimToNull(config.getProperty("xmlaUsername"));
        xmlaPassword = StringUtils.trimToNull(config.getProperty("xmlaPassword"));

        authorizationCache = CacheBuilder.newBuilder().maximumSize(MAX_CACHE_SIZE)
                .expireAfterWrite(CACHE_TIME_MINUTES, TimeUnit.MINUTES).build();
    }


    /**
     * Checks if a request is authenticated
     */
    public boolean authenticateRequest(HttpServletRequest request) {

        // either xmla user name or url must be set
        if (xmlaUsername == null && xmlaAuthorizationUrl == null) {
            LOG.info("No xmlaUsername or xmlaAuthorizationUrl configured, access denied");
            return false;
        }

        String userName = null;
        String password = null;

        // extract credentials in the URL
        String url = request.getRequestURL().toString();
        String[] parts = url.split("/");

        if (parts.length > 3 && parts[parts.length - 3].startsWith("xmla")) {
            try {
                userName = URLDecoder.decode(parts[parts.length - 2], "UTF-8");
                password = URLDecoder.decode(parts[parts.length - 1], "UTF-8");
            }
            catch (UnsupportedEncodingException e) {
            }
        }
        else {
            // check for basic authentication
            String authorization = request.getHeader("Authorization");
            String credentials = StringUtils.trim(StringUtils.substringAfter(authorization, "Basic"));
            if (StringUtils.isEmpty(credentials)) {
                LOG.info("No authentication credentials found, access denied");
                return false;
            }

            // extract username and password
            String decoded = new String(Base64.decode(credentials), Charset.forName("UTF-8"));
            userName = StringUtils.substringBefore(decoded, ":");
            password = StringUtils.substringAfter(decoded, ":");
        }

        // check if we want to use fixed username and password
        if (xmlaAuthorizationUrl == null) {
            return StringUtils.equals(userName, xmlaUsername) && StringUtils.equals(password, xmlaPassword);
        }

        // check if we have a cached authentication
        CubeAccess cubeAccess = authorizationCache.getIfPresent(userName + ":" + password);
        if (cubeAccess == null) {

            // call the authorization url
            cubeAccess = doAuthorizationRequest(userName, password);
            if (cubeAccess == null || !cubeAccess.isAllowed()) {
                LOG.info("Wrong username/password, access denied");
                return false;
            }
        }

        authorizationCache.put(userName + ":" + password, cubeAccess);
        request.setAttribute(CubeAccess.REQUEST_ATTR, cubeAccess);
        return true;
    }


    /**
     * Calls the configured authorization URL and parses the response as JSON
     */
    private CubeAccess doAuthorizationRequest(String userName, String password) {

        try {
            Request request = Request.Post(xmlaAuthorizationUrl)
                                     .bodyForm(Form.form().add("username", userName).add("password", password).build())
                                     .connectTimeout(REQUEST_TIMEOUT_MILLIS).socketTimeout(REQUEST_TIMEOUT_MILLIS);

            String responseString = request.execute().returnContent().asString();

            // parse the JSON repsponse
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(responseString, CubeAccess.class);
        }
        catch (Throwable e) {
            // any error means that the authorization failed
            LOG.error("Error calling " + xmlaAuthorizationUrl, e);
            return null;
        }
    }

}
