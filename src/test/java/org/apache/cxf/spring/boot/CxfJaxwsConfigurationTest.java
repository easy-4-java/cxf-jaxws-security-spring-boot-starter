package org.apache.cxf.spring.boot;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.management.InstrumentationManager;
import org.apache.cxf.management.counters.CounterRepository;
import org.apache.cxf.spring.boot.jaxws.endpoint.EndpointApiTemplate;
import org.apache.cxf.spring.boot.jaxws.endpoint.EndpointCallback;
import org.apache.cxf.spring.boot.jaxws.property.LoggingFeatureProperty;
import org.apache.cxf.validation.BeanValidationFeature;
import org.apache.cxf.validation.BeanValidationProvider;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CxfJaxwsConfigurationTest {
    @Test
    void buildsCoreFeaturesAndEmptyEndpointRegistry() {
        CxfJaxwsAutoConfiguration config = new CxfJaxwsAutoConfiguration();
        SpringBus bus = config.bus();
        try {
            BeanValidationProvider provider = config.validationProvider();
            BeanValidationFeature validation = config.validationFeature(provider);
            assertNotNull(validation);
            CxfJaxwsProperties properties = new CxfJaxwsProperties();
            properties.setEnabled(true); properties.setNamespace("urn:test");
            LoggingFeatureProperty logging = properties.getLoggingFeature();
            logging.setLimit(512); logging.setThreshold(1024L);
            logging.setLogBinary(true); logging.setLogMultipart(false);
            logging.setPrettyLogging(true); logging.setVerbose(true);
            properties.setLoggingFeature(logging);
            LoggingFeature feature = config.loggingFeature(properties);
            assertNotNull(feature);
            assertTrue(properties.isEnabled()); assertEquals("urn:test", properties.getNamespace());
            assertSame(logging, properties.getLoggingFeature());
            assertEquals(512, logging.getLimit()); assertEquals(1024L, logging.getThreshold());
            assertTrue(logging.isLogBinary()); assertFalse(logging.isLogMultipart());
            assertTrue(logging.isPrettyLogging()); assertTrue(logging.isVerbose());
            EndpointCallback callback = config.endpointCallback(feature, validation);
            assertNotNull(callback);
            ApplicationContext context = mock(ApplicationContext.class);
            when(context.getBeansWithAnnotation(jakarta.jws.WebService.class)).thenReturn(Map.of());
            config.setApplicationContext(context);
            assertSame(context, config.getApplicationContext());
            EndpointApiTemplate endpoints = config.endpointTemplate(bus, callback, feature, validation);
            assertTrue(endpoints.getEndpoints().isEmpty());
        } finally {
            bus.shutdown(true);
        }
    }

    @Test
    void configuresJmxAndSecurityFeaturesWithoutOpeningNetworkConnector() {
        Bus bus = mock(Bus.class);
        CxfJaxwsManagerProperties properties = new CxfJaxwsManagerProperties();
        properties.setEnabled(true); properties.setServerName("test-mbeans");
        properties.setUsePlatformMBeanServer(true);
        properties.setCreateMBServerConnectorFactory(false);
        properties.setDaemon(true); properties.setThreaded(true);
        properties.setJmxServiceURL("service:jmx:rmi:///jndi/rmi://localhost:9913/jmxrmi");
        assertTrue(properties.isEnabled()); assertTrue(properties.isUsePlatformMBeanServer());
        assertFalse(properties.isCreateMBServerConnectorFactory());
        assertTrue(properties.isDaemon()); assertTrue(properties.isThreaded());
        assertEquals("test-mbeans", properties.getServerName());
        assertTrue(properties.getJmxServiceURL().contains("9913"));
        CxfJaxwsManagerConfiguration config = new CxfJaxwsManagerConfiguration();
        InstrumentationManager manager = config.instrumentationManager(bus, properties);
        assertTrue(((org.apache.cxf.management.jmx.InstrumentationManagerImpl) manager).isEnabled());
        CounterRepository counters = config.counterRepository(bus);
        assertNotNull(counters);
        CxfJaxwsSecurityConfiguration security = new CxfJaxwsSecurityConfiguration();
        assertNotNull(security.WSS4JInInterceptor());
        assertNotNull(security.WSS4JOutInterceptor());
        assertNotNull(security.UsernamePwdAuthInterceptor());
        assertNotNull(security.policyFeature());
        ApplicationContext context = mock(ApplicationContext.class);
        security.setApplicationContext(context);
        assertSame(context, security.getApplicationContext());
        CxfJaxwsSecurityProperties securityProperties = new CxfJaxwsSecurityProperties();
        securityProperties.setEnabled(true);
        assertTrue(securityProperties.isEnabled());
    }
}
