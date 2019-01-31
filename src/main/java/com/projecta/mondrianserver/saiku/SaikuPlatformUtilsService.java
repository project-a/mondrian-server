package com.projecta.mondrianserver.saiku;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;

import org.saiku.service.PlatformUtilsService;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Implementation of PlatformUtilsService that uses the current webapp root
 */
public class SaikuPlatformUtilsService extends PlatformUtilsService {

    @Autowired ServletContext servletContext;

    @PostConstruct
    public void init() {
        setPath(servletContext.getRealPath("/js/saiku/plugins/"));
    }

}
