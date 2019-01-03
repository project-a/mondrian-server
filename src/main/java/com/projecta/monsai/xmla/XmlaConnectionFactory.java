package com.projecta.monsai.xmla;

import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import org.olap4j.OlapConnection;

import com.projecta.monsai.mondrian.MondrianConnector;

import mondrian.xmla.XmlaHandler;


/**
 * Connection factory for the XmlaServlet that uses the MondrianServerProvider
 * to retrieve the currently running server.
 */
public class XmlaConnectionFactory implements XmlaHandler.ConnectionFactory {

    @Override
    public OlapConnection getConnection(String catalog, String schema, String roleName, Properties props)
            throws SQLException {

        return MondrianConnector.getMondrianServer().getConnection(catalog, schema, roleName, props);
    }

    @Override
    public Map<String, Object> getPreConfiguredDiscoverDatasourcesResponse() {
        return null;
    }

}

