package com.projecta.monsai.sql;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.postgresql.util.PGobject;

/**
 * A dynamic proxy that intercepts JDBC methods calls
 *
 * @author akuehnel
 */
public class SqlProxy {

    private static final Logger LOG = Logger.getLogger( SqlProxy.class );

    // automatically register this proxy as a JDBC driver
    static {
        LOG.info("Registering SqlProxy");
        // at this point the loading of the DriverManager class has registered the postgres driver, but we really
        // need to have our driver first, so we first deregister the postgres driver. As we proxy it and create our
        // own postgres driver instance, we do not need to readd it
        // Note: tomcat with unpackWar=false does not load the postgres driver, so it isn't needed there but jetty does
        //       See: DriverManager.loadInitialDrivers() and https://stackoverflow.com/a/22780602/1380673 about how
        //            the jdbc drivers get automatically added...
        try {
            DriverManager.deregisterDriver(DriverManager.getDriver("jdbc:postgresql:postgres"));
            //For good measures do it again, if there are multiple drivers
            DriverManager.deregisterDriver(DriverManager.getDriver("jdbc:postgresql:postgres"));
            // to ensure that it is empty
            DriverManager.getDriver("jdbc:postgresql:postgres");
            LOG.error("Didn't remove all postgres drivers");
        } catch (SQLException e) {
            // not an error
        }

        try {
            DriverManager.registerDriver(createProxy(new DriverProxy(), Driver.class));
        }
        catch (SQLException e) {
            LOG.error(e, e);
        }
    }


    /**
     * Creates a new proxy instance
     */
    public static <T> T createProxy(InvocationHandler proxy, Class<T> clazz) {
        return (T) Proxy.newProxyInstance(SqlProxy.class.getClassLoader(), new Class[] { clazz }, proxy );
    }


    /**
     * Proxy class to intercept calls to java.sql.Driver
     */
    public static class DriverProxy implements InvocationHandler {

        private Driver driver;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            try {
                if (method.getName().equals("toString")) {
                    return SqlProxy.class.getName();
                }

                // proxy the postgres driver
                if (driver == null) {
                    driver = new org.postgresql.Driver();
                }

                // proxy every connection
                if (method.getName().equals("connect") && StringUtils.startsWithIgnoreCase((String) args[0], "jdbc:postgresql")) {
                    Connection connection = (Connection) method.invoke(driver, args);
                    return createProxy(new ConnectionProxy(connection), Connection.class);
                }

                // dont do anything different
                return method.invoke(driver, args);
            }
            catch (InvocationTargetException e) {
                throw e.getCause();
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }


    /**
     * Proxy class to intercept calls to java.sql.Connection
     */
    public static class ConnectionProxy implements InvocationHandler {

        Connection connection;

        public ConnectionProxy(Connection connection) {
            this.connection = connection;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            try {
                Object result = method.invoke(connection, args);

                // create a proxy for every Statement object
                if (result instanceof Statement) {
                    if (result instanceof CallableStatement) {
                        return createProxy(new StatementProxy((Statement) result), CallableStatement.class);
                    }
                    if (result instanceof PreparedStatement) {
                        return createProxy(new StatementProxy((Statement) result), PreparedStatement.class);
                    }
                    return createProxy(new StatementProxy((Statement) result), Statement.class);
                }
                return result;
            }
            catch (InvocationTargetException e) {
                throw e.getCause();
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }


    /**
     * Proxy class to intercept calls to java.sql.Statement
     */
    public static class StatementProxy implements InvocationHandler {

        Statement statement;

        public StatementProxy(Statement statement) {
            this.statement = statement;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            try {
                Object result = method.invoke(statement, args);

                // create a proxy for every ResultSet object
                if (result instanceof ResultSet) {
                    return createProxy(new ResultSetProxy((ResultSet) result), ResultSet.class);
                }
                return result;
            }
            catch (InvocationTargetException e) {
                throw e.getCause();
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }


    /**
     * Proxy class to intercept calls to java.sql.ResultSet
     */
    public static class ResultSetProxy implements InvocationHandler {

        ResultSet resultSet;

        public ResultSetProxy(ResultSet resultSet) {
            this.resultSet = resultSet;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            try {
                Object result = method.invoke(resultSet, args);

                if (result instanceof PGobject) {
                    // make getObject() return String values instead of PgObject
                    return ((PGobject) result).getValue();
                }
                return result;
            }
            catch (InvocationTargetException e) {
                throw e.getCause();
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
