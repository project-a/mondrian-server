package com.projecta.mondrianserver.saiku;

import org.saiku.datasources.connection.ISaikuConnection;
import org.saiku.datasources.datasource.SaikuDatasource;
import org.saiku.web.core.SecurityAwareConnectionManager;
import org.springframework.beans.factory.annotation.Autowired;

import com.projecta.mondrianserver.mondrian.MondrianConnector;

/**
 * Extension of SecurityAwareConnectionManager that always uses the internal
 * datasource
 */
public class SaikuConnectionManager extends SecurityAwareConnectionManager {

    @Autowired private MondrianConnector mondrianConnector;

    private static final long serialVersionUID = 265675089208665427L;

    @Override
    protected ISaikuConnection getInternalConnection(String name, SaikuDatasource datasource) {
        return new SaikuOlapConnection(name);
    }

    @Override
    public ISaikuConnection getConnection(String name) {
        return getInternalConnection(name, new SaikuDatasource());
    }

}
