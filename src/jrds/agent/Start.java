package jrds.agent;

import java.io.IOException;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

public class Start implements Serializable {
    static final private int defaultPort = 2002;
    static final private String defaultProto = "rmi";
    public static enum PROTOCOL {
        rmi,
        jmx,
        jmxmp,
        jolokia,
    }

    public final static RProbeActor actor = new RProbeActor();

    static private final class JrdsMBeanInfo {
        MBeanServer mbs;
        JMXServiceURL url;
        JMXConnectorServer cs;

        public JrdsMBeanInfo(PROTOCOL protocol, String host, int port) throws IOException, NotBoundException {
            String path = "/";
            String protocolString = protocol.toString();
            if (protocol == PROTOCOL.jmx) {
                protocolString = "rmi";
                java.rmi.registry.LocateRegistry.createRegistry(port);
                path = "/jndi/rmi://" + host + ":" + port + "/jmxrmi";
            }
            url = new JMXServiceURL(protocolString, host, port, path);
            mbs = ManagementFactory.getPlatformMBeanServer();
            cs = JMXConnectorServerFactory.newJMXConnectorServer(url, null, mbs);
            cs.start();
            JMXServiceURL addr = cs.getAddress();
            JMXConnectorFactory.connect(addr);
        }
    }

    /**
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {
        String portProp = System.getProperty("jrds.port");
        int port = parseStringNumber(portProp, defaultPort);
        if(port == 0)
            port = defaultPort;

        PROTOCOL proto = PROTOCOL.valueOf(defaultProto);
        String protoProp = System.getProperty("jrds.proto", proto.toString()).trim().toLowerCase();
        if( ! "".equals(protoProp) )
            proto = PROTOCOL.valueOf(protoProp);

        start(port, proto);

        //Initialization done, set the security manager
        String withSecurity = System.getProperty("jrds.security", "true");
        if (System.getSecurityManager() == null && Boolean.parseBoolean(withSecurity)) {
            boolean debugPerm = false;
            if(System.getProperty("jrds.debugperm") != null) {
                debugPerm = true;
            }
            System.setSecurityManager( new AgentSecurityManager(debugPerm, proto) );
        }

        //Make it wait on himself to wait forever
        try {
            Thread.currentThread().join();
            System.out.print("joined");
        } catch (InterruptedException e) {
        }
    }

    /**
     * start the listen thread with a predefined port and security manager, so jrdsagent can be used as a lib and not a standalone daemon
     * @param port
     * @throws RemoteException
     * @throws AlreadyBoundException
     */
    public static void start(int port, PROTOCOL proto) {
        try {
            switch(proto) {
            case jolokia: 
                RProbeJolokiaImpl.register(actor, port);
                break;
            case rmi: 
                RProbeRMIImpl.register(actor, port);
                break;
            case jmxmp:
            case jmx:
                new JrdsMBeanInfo(proto, "localhost", port);
                RProbeJMXImpl.register(actor);
                break;
            }
            //Check if actor can read uptime;
            actor.getUptime();
        } catch (InvocationTargetException e) {
            throw new RuntimeException("failed to start jrdsagent", e.getCause());
        } catch (Exception e) {
            throw new RuntimeException("failed to start jrdsagent", e);
        }
    }

    /**
     * <p>A compact and exception free number parser.<p>
     * <p>If the string can be parsed as the specified type, it return the default value<p>
     * @param toParse The string to parse
     * @param defaultVal A default value to use it the string can't be parsed
     * @return An Number object using the same type than the default value.
     */
    @SuppressWarnings("unchecked")
    public static <NumberClass extends Number> NumberClass parseStringNumber(String toParse, NumberClass defaultVal) {
        if(toParse == null || "".equals(toParse))
            return defaultVal;

        try {
            Class<NumberClass> clazz = (Class<NumberClass>) defaultVal.getClass();
            Constructor<NumberClass> c = clazz.getConstructor(String.class);
            return c.newInstance(toParse);
        } catch (SecurityException e) {
        } catch (NoSuchMethodException e) {
        } catch (IllegalArgumentException e) {
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        }
        return defaultVal;
    }

}
