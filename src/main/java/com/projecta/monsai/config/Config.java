package com.projecta.monsai.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.stereotype.Component;

/**
 * Utility class that provides the configuration information by reading the
 * cubes.properties file, the path to which must either be provided as the
 * system property or the context parameter with the name "cubes.config"
 */
@Component
public class Config extends PropertiesFactoryBean {

    @Autowired
    private ServletContext servletContext;

    private Properties properties;


    /**
     * Loads the properties on spring intialisation
     */
    @Override
    protected Properties createProperties() throws IOException {

        // determine location of configuration file
        String configFileName = System.getProperty("cubes.config");
        if (StringUtils.isEmpty(configFileName)) {
            configFileName = servletContext.getInitParameter("cubes.config");
        }
        if (StringUtils.isEmpty(configFileName)) {
            throw new RuntimeException("Please set \"cubes.config\" as a system property or a context parameter");
        }

        // load configuration file
        properties = new Properties();
        try (Reader reader = new InputStreamReader(new FileInputStream(configFileName), StandardCharsets.UTF_8.name())) {
            properties.load(reader);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        // add location of web app
        String webappRoot = servletContext.getRealPath("/");
        properties.put("webappRoot", webappRoot);

        return properties;
    }


    /**
     * Retrieves the property with the given name
     */
    public String getProperty(String key) {
        return properties.getProperty(key);
    }


    /**
     * Retrieves the list of properties
     */
    public Properties getProperties() {
        return properties;
    }


    /**
     * Retrieves the property with the given name. If the property is not set,
     * the default value is used.
     */
    public String getProperty(String key, String defaultValue) {

        String value = properties.getProperty(key);
        return StringUtils.isBlank(value) ? defaultValue : value;
    }


    /**
     * Retrieves the property with the given name. If the property is not set,
     * this will cause an exception
     */
    public String getRequiredProperty(String key) {

        String result = properties.getProperty(key);
        if (StringUtils.isBlank(result)) {
            throw new RuntimeException("Property \"" + key + "\" not set");
        }
        return result;
    }

}
