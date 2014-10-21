
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


