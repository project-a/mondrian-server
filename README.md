# Mondrian Server

This project bundles and tightly integrates the [Mondrian OLAP engine](http://mondrian.pentaho.com), the Mondrian XMLA server, and the [Saiku Ad-hoc analysis tool](http://meteorite.bi/saiku) in a self-contained and easy-to-configure war file. It allows to run Saiku and an external XMLA based reporting frontend on the same single data source.

If you work with Python, then you can use [Mara Mondrian](https://github.com/project-a/mara-mondrian) to interact with Mondrian Server.

&nbsp;


Given that you have

1. a JDBC connection for an existing Data Warehouse,
2. a Mondrian [cube definitions xml file](https://mondrian.pentaho.com/documentation/schema.php), and a
3. [mondrian-server.properties](#configuring-mondrian-server) file,

running the server is as easy as

```
java -Dmondrian-server.properties=/path/to/mondrian-server.properties -jar jetty-runner.jar --port 8080 mondrian-server.war
```

&nbsp;

This will expose the following apps / apis on [http://localhost:8080](http://localhost:8080):

- `/`: The [Saiku](http://meteorite.bi/saiku) web app running on the configured data source.
- `/xmla`: An unauthenticated API endpoint for running [XMLA](https://en.wikipedia.org/wiki/XML_for_Analysis) requests / [MDX](https://en.wikipedia.org/wiki/MultiDimensional_eXpressions) queries against the Data Warehouse.
- `/xmla-with-auth`: Like `/xmla`, but with user/ password based authentication
- `/flush-caches`: Clears all mondrian caches and reloads the cube definitions .xml file.
- `/stats`: Prints memory usage statistics and currently running queries.


&nbsp;

## Features

Mondrian Server makes a few assumptions / simplifications that have worked well for us in the past:

- **Single database connection**: Only one JDBC database connection can be configured for accessing the data warehouse (rather than a catalog of connections). This connection is then hard-wired in the XML server and in Saiku.

- **Mondrian 8 together with Saiku**: Saiku is based on Mondrian 4, which is not backward compatible with Mondrian 3. However, more development has happened on Mondrian 3, and [it is called now Mondrian 8](https://community.hitachivantara.com/thread/14069-what-is-the-status-of-mondrian-4x-where-is-the-latest-code). Mondrian Server patches Saiku to work together with Mondrian 8.

- **External ACL for Saiku and XMLA**: External ACL providers can be integrated for authenticating users.

- **Cube level permissions**: When the external ACL is used, permissions can be defined per user and cube.

- **Simplified user managment in Saiku**: The internal user management and other configuration features of Saiku have been disabled in favor of external ACL and folder based query repositories.



## Configuring Mondrian Server

Only one configuration file [mondrian-server.properties](#configuring-mondrian-server) is used to configure the whole app. No need to unpack the war file.






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



Authentication
--------------

By default, all users are allowed to access Saiku and all API endpoints. It is
assumed that authentication will have happend at the proxy level.

When the configuration parameter `saikuAuthorizationUrl` is set, all requests to Saiku
resources require the HTTP header `saiku-user` to be set (this must be set at a proxy level).
The configured URL is then called with the given user name as parameter. This URL must
return a JSON response `{"allowed":true}` or `{"allowed":false}` to control access to Saiku.
A successful authentication will be cached for 30 minutes.

When the configuration parameter `xmlaAuthorizationUrl` is set, all requests to the
`\xmla-with-auth` endpoint require a username and password via HTTP Basic Authentication.
The configured URL is then called with the given user name and password as parameters.
This URL must return a JSON response `{"allowed":true}` or `{"allowed":false}` to control
access to the excel endpoint. A successful authentication will be cached for 30 minutes.

