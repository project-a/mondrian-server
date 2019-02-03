
**coming soon**

Monsai = MONdrian xmla server + SAIku.

Monsai bundles the Pentaho Mondrian OLAP engine (http://mondrian.pentaho.com), the Mondrian XMLA server, and the Saiku Ad-hoc analysis tool (http://meteorite.bi/saiku) in a self-contained and easy-to-configure war file with a single shared instance of the Mondrian engine.

This can be be useful if you want to be able to run Saiku and an external XMLA based reporting frontend on the same data source. 

To get it running (provided you have a Mondrian schema and a matching database), copy *monsai.war* to some directory and configure your Tomcat *server.xml* like this:

```xml
<?xml version='1.0' encoding='utf-8'?>
<Server port="8005" shutdown="SHUTDOWN">
    <Listener className="org.apache.catalina.core.JasperListener" />
    <Listener className="org.apache.catalina.core.JreMemoryLeakPreventionListener" />

    <Service name="catalina">
        <Connector port="8080" protocol="HTTP/1.1" connectionTimeout="20000"
                   URIEncoding="UTF-8" redirectPort="8443" />

        <Engine name="default">

            <Host name="localhost" unpackWARs="false" autoDeploy="false">
                <Context docBase="/path/to/monsai.war" path="" crossContext="true" swallowOutput="true" reloadable="false" >
                    <Parameter name="monsai.config" value="/path/to/monsai.properties" override="false"/>
                </Context>
            </Host>

        </Engine>
    </Service>
</Server>
```

The referenced configuration file *monsai.properties* looks like this:

```ini
# jdbc url
jdbcUrl=jdbc:postgresql://localhost/dwh;JdbcDrivers=org.postgresql.Driver;JdbcUser=user

# absolute path to mondrian schema
mondrianSchemaFile=/path/to/mondrian-schema.xml


# locale (mostly for displaying numbers)
locale=en_US

# fixed saiku user / password
saikuUsername=asdf
saikuPassword=asdf
```


Restart Tomcat and hopefully see the following urls working

- http://localhost:8080 Saiku (user / password: asdf)

- http://localhost:8080/xmla The XMLA interface

- http://localhost:8080/flush-caches Purging of all caches and reloading of mondrian schema



Monsai Documentation
====================

Monsai is a combined build of Saiku, Mondrian and some additional functionality.

store saiku queries in a file system directory


Build Process
-------------

- You need to have gradle installed (use `brew install gradle`)
- then just run gradle in the project root directory
- this downloads all required ressources and builds mondrian-server.war


Installation
------------

- mondrian-server.war must be installed in a Apache Tomcat in the root path
- the context parameter `mondrian-server.properties` must be provided and contain a path to the configuration file


Configuration
-------------

The configuration file is a properties file that contains all availaible configuration parameters.
See `mondrian-properties.properties.example` as an example.

The following configuration parameters can be set:

- `baseUrl`: Public base url of the Tomcat where mondrian-server.war is installed

- `databaseUrl`: JDBC connection string for the database connection to be used

- `mondrianSchemaFile`: Absolute path to mondrian schema xml file
- `mondrianPropertiesFile`: Absolute path to mondrian.properties file (optional)

- `saikuStorageDir`: Absolute path to the directory where Saiku user queries will be stored. This directory must exist and be writable.
- `saikuAuthorizationUrl`: URL that will be called to check whether a given user has access rights to Saiku (optional)
- `saikuUsername`: A fixed user name for simple Saiku authentication
- `saikuPassword`: A fixed password for simple Saiku authentication
- `excelAuthorizationUrl`: URL that will be called to check whether a given user has access rights to Excel (optional)
- `excelUsername`: A fixed user name for simple Excel authentication
- `excelPassword`: A fixed password for simple Excel authentication

- `logMdx`: Set to "true" to enable logging of all executed MDX statements
- `logSql`: Set to "true" to enable logging of all executed SQL statements
- `logXmla`: Set to "true" to enable logging of all XMLA requests and responses
- `logAll`: Set to "true" to enable logging of all mondrian output

- `locale`: The locale that is used for formatting numbers


API
---

The following URLs are provided as an external API:

- `/`: Starts the Saiku web application
- `/xmla`: API endpoint for MDX queries
- `/excel`: API endpoint for MDX queries from Excel integration
- `/flush-caches`: Clears all mondrian caches


Authentication
--------------

By default, all users are allowed to access Saiku and all API endpoints. It is
assumed that authentication will have happend at the proxy level.

When the configuration parameter `saikuAuthorizationUrl` is set, all requests to Saiku
resources require the HTTP header `saiku-user` to be set (this must be set at a proxy level).
The configured URL is then called with the given user name as parameter. This URL must
return a JSON response `{"allowed":true}` or `{"allowed":false}` to control access to Saiku.
A successful authentication will be cached for 30 minutes.

When the configuration parameter `excelAuthorizationUrl` is set, all requests to the
`\excel` endpoint require a username and password via HTTP Basic Authentication.
The configured URL is then called with the given user name and password as parameters.
This URL must return a JSON response `{"allowed":true}` or `{"allowed":false}` to control
access to the excel endpoint. A successful authentication will be cached for 30 minutes.


Excel Integration Guide
-----------------------

To access DWH information from Microsoft Excel, you have to do the following steps:

- First, download the XMLA driver [XMLA_provider_v1.0.0.103.exe](https://sourceforge.net/projects/xmlaconnect/files) on the client machine and go through the steps of the installation process.


- Start Microsoft Excel
- Click on "Insert", then "PivotTable"
- Select "Choose external data source"
- Click on "Choose connection", the "Existing Connections" dialog is displayed
- Click on "Browse for More...", the "Select Data Source" dialog is displayed
- Click on "New Source..."
- Select "Others", then click "Continue >"
- Select "XMLA Data Source", then click "Continue >>"
- For "Location", enter the Saiku URL + `/excel`
- For "User name" and "Password" enter the configured user name and password
- Select the catalog, then click "OK"
- Select the cube you want to query, then click "Continue >"
- Click on "Finish"
- Click on "OK"
- You can now select the fields and columns you want to query from the Excel PivotTable view
- All queries are executed automatically and are shown as a table in Excel



