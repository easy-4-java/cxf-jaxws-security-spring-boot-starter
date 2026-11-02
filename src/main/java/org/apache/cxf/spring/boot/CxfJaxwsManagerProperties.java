package org.apache.cxf.spring.boot;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(CxfJaxwsManagerProperties.PREFIX)
public class CxfJaxwsManagerProperties {

	public static final String PREFIX = "cxf.manager";

	/**
	 * If the JMX integration should be enabled or not
	 */
	private boolean enabled = false;
	/**
	 * If true and no reference to an MBeanServer is supplied, the JMX extension
	 * registers MBeans with the platform MBean server.
	 */
	private boolean usePlatformMBeanServer = false;
	/**
	 * If true, a connector is created on the MBeanServer.
	 */
	private boolean createMBServerConnectorFactory = false;
	/**
	 * Determines if the creation of the MBean connector is performed in this thread
	 * or in a separate thread. Only relevant if createMBServerConnectorFactory is
	 * true.
	 */
	private boolean threaded = false;
	/**
	 * Determines if the MBean connector creation thread is marked as a daemon
	 * thread or not. Only relevant if createMBServerConnectorFactory is true.
	 */
	private boolean daemon = false;
	/**
	 * If supplied, usePlatformMBeanServer is false, and no reference to an
	 * MBeanServer is supplied, the JMX extension registers MBeans with the MBean
	 * server carrying this name.
	 */
	private String serverName;
	/**
	 * The URL of the connector to create on the MBeanServer. Only relevant if
	 * createMBServerConnectorFactory is true.
	 * <code>service:jmx:rmi:///jndi/rmi://localhost:9913/jmxrmi</code>
	 */
	private String jmxServiceURL;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isUsePlatformMBeanServer() {
		return usePlatformMBeanServer;
	}

	public void setUsePlatformMBeanServer(boolean usePlatformMBeanServer) {
		this.usePlatformMBeanServer = usePlatformMBeanServer;
	}

	public boolean isCreateMBServerConnectorFactory() {
		return createMBServerConnectorFactory;
	}

	public void setCreateMBServerConnectorFactory(boolean createMBServerConnectorFactory) {
		this.createMBServerConnectorFactory = createMBServerConnectorFactory;
	}

	public boolean isThreaded() {
		return threaded;
	}

	public void setThreaded(boolean threaded) {
		this.threaded = threaded;
	}

	public boolean isDaemon() {
		return daemon;
	}

	public void setDaemon(boolean daemon) {
		this.daemon = daemon;
	}

	public String getServerName() {
		return serverName;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	public String getJmxServiceURL() {
		return jmxServiceURL;
	}

	public void setJmxServiceURL(String jmxServiceURL) {
		this.jmxServiceURL = jmxServiceURL;
	}

}
