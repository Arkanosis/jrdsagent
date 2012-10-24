package jrds.agent;

import java.io.FileDescriptor;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.Permission;
import java.util.Collections;
import java.util.HashSet;

public class Start implements Serializable {
    static final private int defaultPort = 2002;

    /**
     * @param args
     * @throws RemoteException 
     * @throws AlreadyBoundException 
     * @throws MalformedURLException 
     */
    public static void main(String[] args) throws RemoteException, MalformedURLException, AlreadyBoundException {
        String portProp = System.getProperty("jrds.port");
        int port = parseStringNumber(portProp, Integer.class, defaultPort).intValue();
        if(port == 0)
            port = defaultPort;

        start(port);
    }

    static final private SecurityManager getSecurityManager() {
        return new SecurityManager() {
            private final HashSet<String> runtimePermissions = new HashSet<String>();
            {
                Collections.addAll(runtimePermissions, "setContextClassLoader", "getFileSystemAttributes", "writeFileDescriptor", "getClassLoader", "createClassLoader",
                        "sun.rmi.runtime.RuntimeUtil.getInstance", "reflectionFactoryAccess", "accessClassInPackage.sun.reflect",
                        "accessDeclaredMembers");
            }
            public void checkPermission(Permission perm) {
                final String permName = perm.getName();
                final String permAction = perm.getActions();
                final String permClass = perm.getClass().getName();
                if("java.io.FilePermission".equals(permClass)) {
                    if("read".equals(permAction)  && (permName.startsWith("/proc/") || permName.startsWith("/sys/")))
                        return;
                }
                else if("java.net.SocketPermission".equals(permClass)) {
                    if(! "connect".equals(permAction))
                        return;
                }
                else if("java.net.NetPermission".equals(permClass)) {
                    return;
                }
                else if("java.util.PropertyPermission".equals(permClass) ) {
                    if ("read".equals(permAction))
                        return;
                }
                else if("java.lang.RuntimePermission".equals(permClass)) {
                    if(runtimePermissions.contains(permName))
                        return;
                    System.out.println(permName);
                }
                else if("java.io.SerializablePermission".equals(permClass)) {
                    return;
                }
                else if("java.lang.reflect.ReflectPermission".equals(permClass)) {
                    return;
                }
                else if("java.util.logging.LoggingPermission".equals(permClass)) {
                    return;
                }
                else if("java.security.SecurityPermission".equals(permClass)) {
                    if ("getPolicy".equals(permName))
                        return;
                }
                super.checkPermission(perm);
            }
            public void checkAccept(String host, int port) {}
            public void checkAccess(Thread t) {}
            public void checkAccess(ThreadGroup g) {}
            public void checkConnect(String host, int port, Object context) {}
            public void checkConnect(String host, int port) {}
            public void checkRead(FileDescriptor fd) {}
            public void checkRead(String file, Object context) {}
            public void checkRead(String file) {}
            public void checkLink(String lib) {}
        };    
    }

    /**
     * start the listen thread with a predefined port and security manager, so jrdsagent can be used as a lib and not a standalone daemon
     * @param port
     * @throws RemoteException
     * @throws AlreadyBoundException
     */
    public static void start(int port) throws RemoteException, AlreadyBoundException {
        RProbe dispatcher = new RProbeImpl(port);

        Registry registry = LocateRegistry.createRegistry(port);
        registry.bind(RProbe.NAME, dispatcher);
        if (System.getSecurityManager() == null)
            System.setSecurityManager ( getSecurityManager() );
        //Make it wait on himself to wait forever
        try {
            Thread.currentThread().join();
            System.out.print("joined");
        } catch (InterruptedException e) {
        }
    }

    /**
     * A function from jrds.Util
     * @param toParse
     * @param numberClass
     * @param defaultVal
     * @return
     */
    public static Number parseStringNumber(String toParse, Class<? extends Number> numberClass, Number defaultVal) {
        if(toParse == null || "".equals(toParse))
            return defaultVal;
        if(! (Number.class.isAssignableFrom(numberClass))) {
            return defaultVal;
        }

        try {
            Constructor<? extends Number> c = numberClass.getConstructor(String.class);
            Number n = c.newInstance(toParse);
            return n;
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
