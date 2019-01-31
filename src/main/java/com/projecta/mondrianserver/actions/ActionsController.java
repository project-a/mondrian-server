package com.projecta.mondrianserver.actions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.projecta.mondrianserver.mondrian.MondrianConnector;

/**
 * Controller class for the API used by the DWH
 */
@Controller
public class ActionsController {

    @Autowired private MondrianConnector  mondrianConnector;
    @Autowired private StatisticsProvider statisticsProvider;

    /**
     * Flushes the mondrian caches
     */
    @RequestMapping(value = "/flush-caches", produces = "text/plain")
    @ResponseBody
    public String flushCaches() {
        return mondrianConnector.flushCaches();
    }


    /**
     * Displays internal statistics
     */
    @RequestMapping(value = "/stats", produces = "text/plain")
    @ResponseBody
    public String stats() throws Exception {
        return statisticsProvider.getStatistics();
    }

}
