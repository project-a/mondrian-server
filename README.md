# Mondrian Server

This project bundles and tightly integrates the [Mondrian OLAP engine](http://mondrian.pentaho.com), the Mondrian XMLA server, and the [Saiku Ad-hoc analysis tool](http://meteorite.bi/saiku) in a self-contained and easy-to-configure war file. It allows to run Saiku and an external XMLA based reporting frontend on the same single data source.

If you work with Python, then you can use [Mara Mondrian](https://github.com/mara/mara-mondrian) to interact with Mondrian Server.

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

**!!! Important: ** currently Mondrian Server does not support recent Java versions. Please use Java 8 to run or build the project.

&nbsp;
  

## Features

Mondrian Server makes a few assumptions / simplifications that have worked well for us in the past:

- **Single database connection**: Only one JDBC database connection can be configured for accessing the data warehouse (rather than a catalog of connections). This connection is then hard-wired in the XMLA server and in Saiku.

- **Mondrian 8 together with Saiku**: Saiku works with Mondrian 4, which is not backward compatible with Mondrian 3. However, more development has happened on Mondrian 3, and [it is called now Mondrian 8](https://community.hitachivantara.com/thread/14069-what-is-the-status-of-mondrian-4x-where-is-the-latest-code). Mondrian Server patches Saiku to work together with Mondrian 8.

- **External ACL for Saiku and XMLA**: External ACL providers can be integrated for authenticating users.

- **Cube level permissions**: When the external ACL is used, permissions can be defined per user and cube.

- **Simplified user managment in Saiku**: The internal user management and other configuration features of Saiku have been disabled in favor of external ACL and folder based query repositories.

If you use Saiku in your organization, then please consider [purchasing a commercial Saiku licence](https://www.meteorite.bi/products/saiku-pricing/).

&nbsp;

## Configuring and running Mondrian Server

Only one configuration file [mondrian-server.properties](mondrian-server.properties) is used to configure the whole app. No need to unpack the war file. The path to this file is passed via the `mondrian-server.properties` system property.

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

1. No authentication. That should only be used for local development. This is the default when none of the other two options are configured.

2. Hard-coded single username / password. Set them with the `saikuUsername` and `saikuPassword` properties in [mondrian-server.properties](mondrian-server.properties). Only recommended when the option 3 is not possible.

3. Header based authentication and external ACL. An auth proxy such as the [oauth2_proxy](https://github.com/pusher/oauth2_proxy) sits in front of Saiku and authenticates users against an external auth provider (e.g. Google, Github, Azure etc.). The proxy adds the email of the authenticated user as a `saiku-user` http header to the request.

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

    A Python implementation that provides such an endpoint can be found in the [mara mondrian](https://github.com/mara/mara-mondrian) package.

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

### XMLA Server

Authentication and ACL for the `/xmla-with-auth` endpoints works slightly different. XMLA clients such as Excel usually can't cope with auth proxies, which is why a username / password authentication is used.

There are 2 options:

1. Hard-coded single username / password, configured via the  `xmlaUsername` and `xmlaPassword` properties.

2. External ACL. The XMLA client needs needs to supply HTTP basic auth credentials. The username / password is then posted to an external ACL endpoint (configured via the `xmlaAuthorizationUrl`) like this:

        ➜ curl -X POST -F 'email=foo@bar.com' -F 'password=123abc' http://localhost:5000/mondrian/xmla/authorize
        {
          "allowed": false,
          "cubes": []
        }

        ➜ curl -X POST -F 'email=martin.loetzsch@project-a.com' -F 'password=123abc' http://localhost:5000/mondrian/xmla/authorize
        {
          "allowed": true,
          "cubes": [
            "Cube 1",
            "Cube 2"
          ]
        }

   The response is then interpreted in the same way as for the Saiku endpoint.

    This is our recommended nginx config for exposing the XMLA server on the internet:

```nginx
server {
    listen 443; # not behind auth proxy, apply ip restrictions or VPN
    include ssl.conf; # never run without SSL

    server_name excel.example.com; # host name for the XMLA server

    real_ip_header X-Forwarded-For;

    location / {
        # allow if the request comes from an office ip
        default_type text/html;

        # set some proxy parameters
        proxy_set_header HOST $http_host;
        proxy_set_header X-Real-Ip $http_x_real_ip;
        proxy_send_timeout 600;
        proxy_read_timeout 600;
        proxy_buffering off;
        send_timeout 600;

        # Send all requests to single endpoint
        proxy_pass http://127.0.0.1:8080/xmla-with-auth/;
    }

}
```

&nbsp;



### Other endpoints

The `/xmla`, `/flush-caches` and `/stats` endpoints have no ACL at all, don't expose them to the internet. The nginx host for Saiku above will put them also behind the auth proxy.


&nbsp;

## PostgreSQL Compatibility Features

Mondrian Server implements features that improve compatiblity with PostgreSQL:

**Enum Support**: Values of the Postgres `enum` type in fact or dimension tables
are automatically converted to type text. See class `SqlProxy.java` for more details.

**HyperLogLog Support**: Mondrian Server integrates with the Citus Data
[HyperLogLog](https://github.com/citusdata/postgresql-hll) extension
to allow the calculation of an approximated [distinct count](https://docs.citusdata.com/en/stable/articles/hll_count_distinct.html)
on large datasets.
Mondrian Server allows fact tables to contain columns of type `hll`
and automatically uses the `hll_cardinality` function for aggregations
on these columns. See class `SqlRewriter.java` for more details.


&nbsp;

## Building Mondrian Server

Install **gradle** with `brew install gradle` or `apt-get install gradle`. Then run `gradle` in the project root directory. This will download all required ressources and build `mondrian-server.war` in the project root directory.


&nbsp;

## Updating Mondrian Server

The Mondrian Server is built against specific versions of Mondrian
and Saiku. These are the necessary steps to upgrade one of these components to a newer version.


### Updating Mondrian

To update Mondrian to a newer version, follow these steps:

1. **Determine new Mondrian version**: Check the [Pentaho GitHub](https://github.com/pentaho/mondrian/releases) for available releases and the [Penthaho Nexus Repository](https://nexus.pentaho.org/#browse/browse:omni:pentaho%2Fmondrian) for available builds.

2. **Update build.gradle**: Edit the `build.gradle` file and set the property `ext.mondrianVersion` to the version number from the Pentaho Nexus Repository.

3. **Check Saiku integration**: Make sure that the class `mondrian.olap4j.SaikuMondrianHelper` still compiles. This file provides the connector that Saiku uses to access the internal Mondrian APIs.
If not, check if a newer version of the [original file](https://github.com/OSBI/saiku/blob/master/saiku-core/saiku-olap-util/src/main/java/mondrian/olap4j/SaikuMondrianHelper.java) exists.

4. **Verify patch**: This project contains a patched version of `mondrian.rolap.SmartMemberReader`. This fixes an issue with member ordering we experienced in production. Check if there were any changes in the [original file](https://github.com/pentaho/mondrian/commits/master/mondrian/src/main/java/mondrian/rolap/SmartMemberReader.java) and apply the same changes to the patched file. The modified lines are marked with `[PATCH START]` and `[PATCH END]`.

&nbsp;

### Updating Saiku version

To update Saiku to a newer version, follow these steps:

1. **Determine new Saiku version**: Check the [Saiku GitHub](https://github.com/OSBI/saiku/releases) for available releases.

2. **Update build.gradle**: Edit the `build.gradle` file and set the property `ext.saikuReleaseTag` to the selected release number.

3. **Update dependencies**: The included `build.gradle` contains the merged dependencies from multiple Saiku repositories.
Check if there were any changes in the `<dependencies>` section in one of the following `pom.xml` files:
   - [/saiku-core/saiku-olap-util/pom.xml](https://github.com/OSBI/saiku/blob/master/saiku-core/saiku-olap-util/pom.xml)
   - [/saiku-core/saiku-service/pom.xml](https://github.com/OSBI/saiku/blob/master/saiku-core/saiku-service/pom.xml)
   - [/saiku-core/saiku-web/pom.xml](https://github.com/OSBI/saiku/blob/master/saiku-core/saiku-web/pom.xml)
   - [/saiku-core/pom.xml](https://github.com/OSBI/saiku/blob/master/saiku-core/pom.xml)
   - [/pom.xml](https://github.com/OSBI/saiku/blob/master/pom.xml)

   Update the 'dependencies' section of the `build.gradle` file accordingly.

4. **Check Spring components**: The package `com.projecta.mondrianserver.saiku` contains several Spring components that override the original Saiku implementation to
remove administrative functionality. Check if these classes still compile and fix them if necessary.

5. **Update patched frontend files**: The directory `/src/main/webapp/js` contains 2 patched JavaScript files with minor usability improvements:

   - [SessionWorkspace.js](https://github.com/OSBI/saiku/blob/master/saiku-ui/js/saiku/models/SessionWorkspace.js)
   - [SaveQuery.js](https://github.com/OSBI/saiku/blob/master/saiku-ui/js/saiku/views/SaveQuery.js)

   Checkout the updated version of these files and reapply the patch (lines are marked with `// PATCHED`)

