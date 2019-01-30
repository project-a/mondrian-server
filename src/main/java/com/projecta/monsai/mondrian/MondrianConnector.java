package com.projecta.monsai.mondrian;

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
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.olap4j.OlapConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import com.projecta.monsai.config.Config;
import com.projecta.monsai.saiku.MonsaiConnectionManager;
import com.projecta.monsai.saiku.OlapConnectionProxy;
import com.projecta.monsai.security.CubeAccess;
import com.projecta.monsai.sql.SqlProxy;
import com.projecta.monsai.sql.SqlRewriter;

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

    @Autowired private Config                  config;
    @Autowired private SchemaProcessor         schemaProcessor;
    @Autowired private MonsaiConnectionManager connectionManager;

    private static MondrianServer server;
    private static String         dataSourceName;
    private static String         catalogName;

    private static final String JDBC_DRIVERS = "org.postgresql.Driver";

    private static final Logger LOG = Logger.getLogger(MondrianConnector.class);

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
        dataSourceName = config.getProperty("dataSourceName", "Cubes");
        catalogName    = config.getProperty("catalogName", "dwh");
        String cubesBaseUrl       = config.getRequiredProperty("cubesBaseUrl");
        String locale             = config.getRequiredProperty("locale");
        String mondrianSchemaFile = config.getRequiredProperty("mondrianSchemaFile");

        // read database connection parameters
        String databaseUrl = config.getProperty("databaseUrl");
        if (StringUtils.isBlank(databaseUrl)) {
            databaseUrl = "jdbc:postgresql://" + config.getRequiredProperty("databaseHost") + "/"
                    + config.getProperty("databaseName");
        }

        // initialize the sql proxy
        @SuppressWarnings( "unused" )
        SqlProxy proxy = new SqlProxy();

        // generate xml DataSources configuration
        Element dataSources = new Element("DataSources");
        Element dataSource = new Element("DataSource");
        dataSources.addContent(dataSource);

        dataSource.addContent(new Element("DataSourceName").setText(dataSourceName));
        dataSource.addContent(new Element("DataSourceDescription").setText(dataSourceName + " Mondrian DWH server"));
        dataSource.addContent(new Element("URL").setText(cubesBaseUrl + "/xmla"));
        dataSource.addContent(new Element("DataSourceInfo").setText(
                  "Provider=mondrian; " + "Locale=" + locale + "; "
                + "DynamicSchemaProcessor=" + SchemaProcessor.class.getName() + "; "
                + "UseContentChecksum=true; "
                + "Jdbc=" + databaseUrl + "; "
                + "JdbcDrivers=" + JDBC_DRIVERS + "; "
                + "JdbcUser=" + StringUtils.defaultString(config.getProperty("databaseUser")) + ";"
                + "JdbcPassword=" + StringUtils.defaultString(config.getProperty("databasePassword"))));

        dataSource.addContent(new Element("ProviderName").setText("Mondrian"));
        dataSource.addContent(new Element("ProviderType").setText("MDP"));
        dataSource.addContent(new Element("AuthenticationMode").setText("Unauthenticated"));

        Element catalog = new Element("Catalog");
        catalog.setAttribute("name", catalogName);
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

        String fileName = config.getProperty("mondrianPropertiesFile");
        if (StringUtils.isBlank(fileName)) {
            return;
        }

        // read the properties file
        LOG.info("Reading " + fileName);
        Properties properties = new Properties();
        try (Reader reader = new InputStreamReader(new FileInputStream(fileName), "UTF8")) {
            properties.load(reader);

            // store the properties as system properties, where mondrian will
            // find them
            for (Entry<Object, Object> entry : properties.entrySet()) {
                String key = (String) entry.getKey();
                String value = (String) entry.getValue();

                if (key.startsWith("mondrian.")) {
                    System.setProperty(key, value);
                }
            }
        }
    }


    /**
     * Configures the log levels for the mondrian server
     */
    private void configureLogLevels() {

        if (StringUtils.equalsIgnoreCase(config.getProperty("logAll"), "true")) {
            Logger.getLogger("mondrian").setLevel(Level.DEBUG);
        }

        if (StringUtils.equalsIgnoreCase(config.getProperty("logSql"), "true")) {
            RolapUtil.SQL_LOGGER.setLevel(Level.DEBUG);
        }

        if (StringUtils.equalsIgnoreCase(config.getProperty("logMdx"), "true")) {
            RolapUtil.MDX_LOGGER.setLevel(Level.DEBUG);
        }

        if (StringUtils.equalsIgnoreCase(config.getProperty("logXmla"), "true")) {
            Logger.getLogger(XmlaServlet.class).setLevel(Level.DEBUG);
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
            OlapConnection connection = server.getConnection(dataSourceName, catalogName, null);
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

            OlapConnectionProxy.closeAllConnections();
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