package com.projecta.mondrianserver.security;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.projecta.mondrianserver.saiku.SaikuSessionService;

/**
 * Filter that checks every web request and applies different ACL mechanisms,
 * depending on the requested endpoint
 *
 * @author akuehnel
 */
public class SecurityFilter implements Filter {

    private SaikuSessionService sessionService;
    private XmlaAuthenticationService     excelUserService;


    /**
     * Retrieves the services that do the actual authentication
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

        WebApplicationContext applicationContext = WebApplicationContextUtils.getWebApplicationContext(filterConfig.getServletContext());
        sessionService = applicationContext.getBean(SaikuSessionService.class);
        excelUserService = applicationContext.getBean(XmlaAuthenticationService.class);
    }


    /**
     * Filters all incoming requests
     */
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
        throws IOException, ServletException {

        if (!(servletRequest instanceof HttpServletRequest) || !(servletResponse instanceof HttpServletResponse)) {
            chain.doFilter(servletRequest, servletResponse);
            return;
        }
        HttpServletRequest  request  = ((HttpServletRequest) servletRequest);
        HttpServletResponse response = ((HttpServletResponse) servletResponse);

        // dont do anything for general urls
        String url = request.getRequestURI();
        if (url.equals("/xmla") || url.equals("/flush-caches") || url.equals("/stats") || url.startsWith("/actions/")) {
            chain.doFilter(servletRequest, servletResponse);
            return;
        }

        // requests for excel are authenticated using the ExcelUserService
        if (url.startsWith("/xmla-with-auth")) {
            if (excelUserService.authenticateRequest(request)) {
                chain.doFilter(servletRequest, servletResponse);
                return;
            }
            else {
                response.sendError(HttpStatus.UNAUTHORIZED.value());
                return;
            }
        }

        // all other requests are Saiku requests
        boolean isStartPage = StringUtils.equals(request.getServletPath(), "/")
                           || StringUtils.equals(request.getServletPath(), "/index.html");

        if (sessionService.authenticateRequest(request, response, isStartPage)) {
            chain.doFilter(servletRequest, servletResponse);
            return;
        }
        else {
            try {
                response.sendError(HttpStatus.FORBIDDEN.value());
            }
            catch (Throwable e) {
            }
            return;
        }
    }


    @Override
    public void destroy() {
        // nothing to do
    }

}
