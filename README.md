# Mondrian Server

This project bundles and tightly integrates the [Mondrian OLAP engine](http://mondrian.pentaho.com), the Mondrian XMLA server, and the [Saiku Ad-hoc analysis tool](http://meteorite.bi/saiku) in a self-contained and easy-to-configure war file. It allows to run Saiku and an external XMLA based reporting frontend on the same single data source.

If you work with Python, then you can use [Mara Mondrian](https://github.com/project-a/mara-mondrian) to interact with Mondrian Server.

&nbsp;


Given that you have

1. a JDBC connection URL for an existing Data Warehouse,
2. a Mondrian [cube definitions xml file](https://mondrian.pentaho.com/documentation/schema.php), and a
3. [mondrian-server.properties](mondrian-server.properties) file,

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

- **Single database connection**: Only one JDBC database connection can be configured for accessing the data warehouse (rather than a catalog of connections). This connection is then hard-wired in the XMLA server and in Saiku.

- **Mondrian 8 together with Saiku**: Saiku works with Mondrian 4, which is not backward compatible with Mondrian 3. However, more development has happened on Mondrian 3, and [it is called now Mondrian 8](https://community.hitachivantara.com/thread/14069-what-is-the-status-of-mondrian-4x-where-is-the-latest-code). Mondrian Server patches Saiku to work together with Mondrian 8.

- **External ACL for Saiku and XMLA**: External ACL providers can be integrated for authenticating users.

- **Cube level permissions**: When the external ACL is used, permissions can be defined per user and cube.

- **Simplified user managment in Saiku**: The internal user management and other configuration features of Saiku have been disabled in favor of external ACL and folder based query repositories.

&nsbp;


## Configuring and running Mondrian Server

Only one configuration file [mondrian-server.properties](mondrian-server.properties) is used to configure the whole app. No need to unpack the war file. The path to this file is passed via the `mondrian-server.properties` system properties.

If you want to use jetty (recommended), then you can run Mondrian Server with 

```
java -Dmondrian-server.properties=/path/to/mondrian-server.properties \
    -jar jetty-runner.jar --port 8080 mondrian-server.war
```

&nbsp;

If you want to use Tomcat, then this is a minimal `server.xml` for running the app:

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
                <Context docBase="/path/to/mondrian-server.war" path="" crossContext="true" swallowOutput="true" reloadable="false" >
                    <Parameter name="mondrian-server.properties" value="/path/to/mondrian-server.properties" override="false"/>
                </Context>
            </Host>

        </Engine>
    </Service>
</Server>
```

&nbsp;

If you want to use another JDBC driver than the included driver for PostgreSQL, then pass the directory containing the .jar file via the `--lib` option in jetty-runner or put the .jar file in the `lib` folder of Tomcat.

&nbsp;

## Authentication & ACL 

### Saiku 

There are three different options for securing the `/` endpoint (Saiku):

1. No authentication. That should only be used for local development. This is the default when none of the other two options is configured.

2. Hard-coded single username / password. Set them with the `saikuUsername` and `saikuPassword` properties in [mondrian-server.properties](mondrian-server.properties). Only recommended when the option 3 is not possible.

3. Header based authentication and external ACL. An auth proxy such as the [oauth2_proxy](https://github.com/pusher/oauth2_proxy) sits infront of Saiku and authenticates users against an external auth provider (e.g. Google, Github, Azure etc.). The proxy adds the email of the authenticated user as a `saiku-user` http header to the request. 

   Mondrian will then post this user name as `username` form field to an ACL endpoint that is configured via the `saikuAuthorizationUrl` property:
   
        ➜ curl -X POST -F 'username=foo@bar.com' http://localhost:5000/mondrian/saiku/authorize
        {
          "allowed": false, 
          "cubes": []
        }
        
        ➜ curl -X POST -F 'username=martin.loetzsch@project-a.com' http://localhost:5000/mondrian/saiku/authorize
        {
          "allowed": true, 
          "cubes": [
            "Cube 1", 
            "Cube 2"
          ]
        }
   
    The external ACL endpoint either returns an `"allowed": false` JSON object as in the first example or a an `"allowed": true` response followed by the list of all cubes that the user has access to. The user will only be able to access those cubes in Saiku.
    
    A Python implementation that provides such an endpoint can be found in the [mara mondrian](https://github.com/project-a/mara-mondrian) package.
    
    This is our recommended way for exposing Saiku through Nginx:

```nginx
server {
    listen 127.0.0.1:81;  # listen as a downstream of an auth proxy
    
    server_name saiku.example.com; # the host name to run Saiku on
    
    location / {
        # set some proxy parameters
        proxy_set_header HOST $http_host;
        proxy_set_header X-Real-Ip $http_x_real_ip;
        proxy_send_timeout 600;
        proxy_read_timeout 600;
        proxy_buffering off;
        send_timeout 600;
    
        # the host / port where mondrian server is running
        proxy_pass http://127.0.0.1:8080; 
        
        # Somehow needed
        proxy_set_header Authorization ""; 
        
        # Add the email or username of the already authenticated user as a header
        proxy_set_header saiku-user $http_X_FORWARDED_EMAIL;       
    }
}
```
    
&nbsp;

### XMLA

Authentication and ACL for the `/xmla-with-auth` endpoints works slightly different. XMLA clients such as Excel usually can't cope with auth proxies, which is why a username / password authentication is used.

Again, there are 3 options:

1. No authentication. 

2. Hard-coded single username / password, configured via the  `xmlaUsername` and `xmlaPassword` properties.

3. External ACL. 



The `/xmla`, `/flush-caches` and `/stats` endpoints have no ACL at all, don't expose them to users / the internet.


&nbsp;

## Building Mondrian Server

Install **gradle** with `brew install gradle` or `apt-get install gradle`. The runn `gradle` in the project root directory. This will download all required ressources and build `mondrian-server.war` in the project root directory.


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

