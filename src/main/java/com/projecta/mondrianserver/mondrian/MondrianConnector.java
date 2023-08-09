package com.projecta.mondrianserver.mondrian;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.SQLException;
import java.util.Map.Entry;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang.StringUtils;
//import org.apache.log4j.Level;
//import org.apache.logging.log4j.Level;
//import org.apache.log4j.Logger;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.olap4j.OlapConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import com.projecta.mondrianserver.config.Config;
import com.projecta.mondrianserver.saiku.SaikuConnectionManager;
import com.projecta.mondrianserver.security.CubeAccess;
import com.projecta.mondrianserver.security.CubeAccessRole;
import com.projecta.mondrianserver.sql.SqlProxy;
import com.projecta.mondrianserver.sql.SqlRewriter;

import mondrian.olap.MondrianServer;
import mondrian.rolap.RolapConnection;
import mondrian.rolap.RolapUtil;
import mondrian.server.StringRepositoryContentFinder;
import mondrian.spi.impl.IdentityCatalogLocator;
import mondrian.xmla.XmlaServlet;

/**
 * Class that configures a mondrian server based on the configured
 * cubes.properties and provides the connection to it
 */
@Component
public class MondrianConnector {

    @Autowired private Config                 config;
    @Autowired private SchemaProcessor        schemaProcessor;
    @Autowired private SaikuConnectionManager connectionManager;

    private static MondrianServer server;

    private static final String DEFAULT_JDBC_DRIVER    = "org.postgresql.Driver";
    private static final String DEFAULT_DATASOURCE_NAME = "Mondrian";
    private static final String DEFAULT_CATALOG_NAME    = "Mondrian";

    private static final Logger LOG = LogManager.getLogger(MondrianConnector.class);

    /**
     * Initializes the mondrian server connection
     */
    @PostConstruct
    public void init() throws Exception {

        // avoid duplicate initialisation
        if (server != null) {
            return;
        }

        schemaProcessor.readSchema();
        readMondrianProperties();
        configureLogLevels();
        connectMondrianServer();
    }


    /**
     * Initializes the mondrian server
     */
    private synchronized void connectMondrianServer() {

        // read config values
        String baseUrl = config.getProperty("baseUrl", "");
        String locale  = config.getRequiredProperty("locale");
        String mondrianSchemaFile = config.getRequiredProperty("mondrianSchemaFile");

        // read database connection parameters
        String databaseUrl = config.getRequiredProperty("databaseUrl");
        String databaseDriver = config.getProperty("databaseDriver", DEFAULT_JDBC_DRIVER);

        // initialize the sql proxy
        @SuppressWarnings( "unused" )
        SqlProxy proxy = new SqlProxy();

        // generate xml DataSources configuration
        Element dataSources = new Element("DataSources");
        Element dataSource = new Element("DataSource");
        dataSources.addContent(dataSource);

        dataSource.addContent(new Element("DataSourceName").setText(DEFAULT_DATASOURCE_NAME));
        dataSource.addContent(new Element("DataSourceDescription").setText("Mondrian server"));
        dataSource.addContent(new Element("URL").setText(baseUrl + "/xmla"));
        dataSource.addContent(new Element("DataSourceInfo").setText(
                  "Provider=mondrian; " + "Locale=" + locale + "; "
                + "DynamicSchemaProcessor=" + SchemaProcessor.class.getName() + "; "
                + "UseContentChecksum=true; "
                + "JdbcDrivers=" + databaseDriver + "; "
                + "Jdbc=" + databaseUrl));

        dataSource.addContent(new Element("ProviderName").setText("Mondrian"));
        dataSource.addContent(new Element("ProviderType").setText("MDP"));
        dataSource.addContent(new Element("AuthenticationMode").setText("Unauthenticated"));

        Element catalog = new Element("Catalog");
        catalog.setAttribute("name", DEFAULT_CATALOG_NAME);
        catalog.addContent(new Element("Definition").setText("file:/" + mondrianSchemaFile));
        dataSource.addContent(new Element("Catalogs").addContent(catalog));

        String dataSourcesXml = new XMLOutputter().outputString(dataSources);

        // initialize the server
        MondrianServer existingServer = server;
        server = MondrianServer.createWithRepository(
                new StringRepositoryContentFinder(dataSourcesXml),
                new IdentityCatalogLocator());

        // shutdown any existing server
        if (existingServer != null) {
            existingServer.shutdown();
        }
    }


    /**
     * Reads an external mondrian.properties file, if present.
     */
    private void readMondrianProperties() throws IOException {

        // store mondrian specific properties as system properties, where
        // mondrian will find them
        for (Entry<String, String> entry : config.getProperties().entrySet()) {
            if (entry.getKey().startsWith("mondrian.")) {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        }
    }


    /**
     * Configures the log levels for the mondrian server
     */
    private void configureLogLevels() {

        if (StringUtils.equalsIgnoreCase(config.getProperty("logMondrian"), "true")) {
            //LogManager.getLogger("mondrian").setLevel(Level.DEBUG);
        }

        if (StringUtils.equalsIgnoreCase(config.getProperty("logSql"), "true")) {
            //RolapUtil.SQL_LOGGER.setLevel(Level.DEBUG);
        	//LogManager.getLogger(RolapUtil.SQL_LOGGER.getName()).setLevel(Level.DEBUG);
        }

        if (StringUtils.equalsIgnoreCase(config.getProperty("logMdx"), "true")) {
            //RolapUtil.MDX_LOGGER.setLevel(Level.DEBUG);
        	//LogManager.getLogger(RolapUtil.MDX_LOGGER.getName()).setLevel(Level.DEBUG);
        }

        if (StringUtils.equalsIgnoreCase(config.getProperty("logXmla"), "true")) {
            //LogManager.getLogger(XmlaServlet.class).setLevel(Level.DEBUG);
        }
    }


    /**
     * Shuts down the server connection when the Tomcat is stopped
     */
    @PreDestroy
    public void destroy() {
        server.shutdown();
    }


    /**
     * Retrieves a new connection to the configured datasource
     */
    public static OlapConnection getOlapConnection() {
        try {
            OlapConnection connection = server.getConnection(DEFAULT_DATASOURCE_NAME, DEFAULT_CATALOG_NAME, null);
            applyPermissions(connection);
            return connection;
        }
        catch (SQLException e) {
            LOG.error("Failed to get olap connection", e);
            return null;
        }
    }


    /**
     * Applies the permissions for the current request to the mondrian connection
     */
    public static void applyPermissions(OlapConnection connection) {
        try {
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            CubeAccess cubeAccess = (CubeAccess) requestAttributes.getAttribute(
                    CubeAccess.REQUEST_ATTR, RequestAttributes.SCOPE_REQUEST);

            RolapConnection rolapConnection = connection.unwrap(RolapConnection.class);
            rolapConnection.setRole(new CubeAccessRole(cubeAccess));
        }
        catch (Throwable e) {
            LOG.error("Error in applyPermissions: " + e.getMessage(), e);
        }
    }


    /**
     * Flush all the caches and reload the mondrian schema
     */
    public String flushCaches() {

        // reload the schema (the SchemaProcessor caches this as well)
        String response = "";
        try {
            schemaProcessor.readSchema();
            response += "processed new schema definition\n";

            connectMondrianServer();
            response += "reinitialized mondrian server\n";

            ConnectionProxy.closeAllConnections();
            connectionManager.refreshAllConnections();
            response += "reinitialized saiku connections\n";

            SqlRewriter.clearCache();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        return response;
    }


    /**
     * Retrieves the currently configured server
     */
    public static MondrianServer getMondrianServer() {
        return server;
    }

}