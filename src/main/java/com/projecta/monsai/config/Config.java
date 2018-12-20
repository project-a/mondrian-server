package com.projecta.monsai.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ServletContextAware;

/**
 * Utility class that provides the configuration information by reading the
 * cubes.properties file, the path to which must either be provided as the
 * system property or the context parameter with the name "cubes.config"
 */
@Component
public class Config extends PropertySourcesPlaceholderConfigurer implements ServletContextAware {

    private ServletContext servletContext;

    private Map<String, String> properties;


    /**
     * Loads the properties on spring intialisation
     */
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

        // determine location of configuration file
        String configFileName = System.getProperty("cubes.config");
        if (StringUtils.isEmpty(configFileName)) {
            configFileName = servletContext.getInitParameter("cubes.config");
        }
        if (StringUtils.isEmpty(configFileName)) {
            throw new RuntimeException("Please set \"cubes.config\" as a system property or a context parameter");
        }

        // load configuration file
        Properties props = new Properties();
        try (Reader reader = new InputStreamReader(new FileInputStream(configFileName), StandardCharsets.UTF_8.name())) {
            props.load(reader);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        properties = new LinkedHashMap<>();
        for (Entry<Object, Object> entry : props.entrySet()) {
            properties.put((String) entry.getKey(), (String) entry.getValue());
        }

        // make properties available in spring expressions
        MutablePropertySources propertySources = new MutablePropertySources();
        propertySources.addFirst(new MapPropertySource("cubesProperties", (Map) properties));
        setPropertySources(propertySources);
        setIgnoreUnresolvablePlaceholders(true);
    }




    /**
     * Retrieves the property with the given name
     */
    public String getProperty(String key) {
        return properties.get(key);
    }


    /**
     * Retrieves the list of properties
     */
    public Map<String, String> getProperties() {
        return properties;
    }


    /**
     * Retrieves the property with the given name. If the property is not set,
     * the default value is used.
     */
    public String getProperty(String key, String defaultValue) {

        String value = properties.get(key);
        return StringUtils.isBlank(value) ? defaultValue : value;
    }


    /**
     * Retrieves the property with the given name. If the property is not set,
     * this will cause an exception
     */
    public String getRequiredProperty(String key) {

        String result = properties.get(key);
        if (StringUtils.isBlank(result)) {
            throw new RuntimeException("Property \"" + key + "\" not set");
        }
        return result;
    }


    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

}
