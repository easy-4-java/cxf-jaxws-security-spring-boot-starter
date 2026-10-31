package org.apache.cxf.spring.boot;

import java.io.IOException;
import java.net.URI;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Restores the JMX connector options that CXF 4 no longer manages itself. */
public final class JmxConnectorServerAdapter implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(JmxConnectorServerAdapter.class);
    private static final String DEFAULT_URL = "service:jmx:rmi:///jndi/rmi://localhost:9913/jmxrmi";

    private final JMXConnectorServer connector;
    private final Registry ownedRegistry;
    private final Thread startThread;

    public JmxConnectorServerAdapter(MBeanServer server, CxfJaxwsManagerProperties properties)
            throws IOException {
        if (server == null) {
            throw new IllegalStateException("CXF JMX MBeanServer has not been initialized");
        }
        String configuredUrl = properties.getJmxServiceURL();
        String address = configuredUrl == null || configuredUrl.isEmpty() ? DEFAULT_URL : configuredUrl;
        JMXServiceURL url = new JMXServiceURL(address);
        ownedRegistry = createLocalRegistry(url);
        try {
            connector = JMXConnectorServerFactory.newJMXConnectorServer(url, null, server);
            if (properties.isThreaded()) {
                startThread = new Thread(() -> {
                    try {
                        connector.start();
                    } catch (IOException ex) {
                        LOG.error("Unable to start CXF JMX connector at {}", address, ex);
                    }
                }, "JMX Connector Thread [" + address + "]");
                startThread.setDaemon(properties.isDaemon());
                startThread.start();
            } else {
                startThread = null;
                connector.start();
            }
        } catch (IOException | RuntimeException ex) {
            releaseRegistry();
            throw ex;
        }
    }

    private Registry createLocalRegistry(JMXServiceURL url) throws IOException {
        if (!"rmi".equalsIgnoreCase(url.getProtocol())
                || !url.getURLPath().startsWith("/jndi/")) {
            return null;
        }
        URI registryAddress = URI.create(url.getURLPath().substring("/jndi/".length()));
        if (!"localhost".equalsIgnoreCase(registryAddress.getHost())
                || registryAddress.getPort() < 1) {
            return null;
        }
        int port = registryAddress.getPort();
        try {
            return LocateRegistry.createRegistry(port);
        } catch (java.rmi.server.ExportException alreadyRunning) {
            Registry existing = LocateRegistry.getRegistry(port);
            existing.list();
            return null;
        }
    }

    @Override
    public void close() throws IOException {
        try {
            if (startThread != null) {
                startThread.join();
            }
            if (connector != null && connector.isActive()) {
                connector.stop();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while stopping CXF JMX connector", ex);
        } finally {
            releaseRegistry();
        }
    }

    private void releaseRegistry() throws IOException {
        if (ownedRegistry != null) {
            try {
                UnicastRemoteObject.unexportObject(ownedRegistry, true);
            } catch (java.rmi.NoSuchObjectException ex) {
                throw new IOException("CXF JMX registry is already closed", ex);
            }
        }
    }
}
