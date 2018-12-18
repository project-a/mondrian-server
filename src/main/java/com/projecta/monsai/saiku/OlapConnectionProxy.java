package com.projecta.monsai.saiku;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.WeakHashMap;

import org.olap4j.OlapConnection;

import com.projecta.monsai.mondrian.MondrianConnector;

/**
 * Dynamic proxy for connections to prevent cacheing of connections by Saiku
 */
public class OlapConnectionProxy implements InvocationHandler {

    private OlapConnection connection;

    private static Map<OlapConnectionProxy, Boolean> instances = new WeakHashMap<OlapConnectionProxy, Boolean>();


    private OlapConnectionProxy() {
    }


    /**
     * Creates a new dynamic proxy instance for olap connections
     */
    public static OlapConnection getProxyInstance() {

        OlapConnectionProxy proxy = new OlapConnectionProxy();
        synchronized (OlapConnectionProxy.class) {
            instances.put(proxy, true);
        }

        return (OlapConnection) Proxy.newProxyInstance(
                OlapConnection.class.getClassLoader(),
                new Class[] { OlapConnection.class }, proxy);
    }


    /**
     * Whenever a method on the proxy is called, we dynamically create a new
     * connection, or use the existing one.
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        OlapConnection con = null;
        synchronized (this) {
            if (connection == null) {
                connection = MondrianConnector.getOlapConnection();
            }
            con = connection;
        }

        return method.invoke(con, args);
    }


    /**
     * Closes all outstanding connections for every proxy instance
     */
    public static synchronized void closeAllConnections() {

        for (OlapConnectionProxy proxy : instances.keySet()) {
            if (proxy != null) {
                synchronized (proxy) {
                    try {
                        if (proxy.connection != null) {
                            proxy.connection.close();
                        }
                    }
                    catch (Throwable e) {
                    }
                    proxy.connection = null;
                }
            }
        }
    }

}

