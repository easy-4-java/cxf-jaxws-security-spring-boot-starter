package org.apache.cxf.spring.boot;

import jakarta.xml.ws.Endpoint;
import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.ServerImpl;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.spring.boot.jaxws.callback.DefaultEndpointCallback;
import org.apache.cxf.spring.boot.jaxws.endpoint.EndpointApiTemplate;
import org.apache.cxf.spring.boot.jaxws.endpoint.EndpointCallback;
import org.apache.cxf.spring.boot.jaxws.feature.EndpointPauseFeature;
import org.apache.cxf.validation.BeanValidationFeature;
import org.junit.jupiter.api.Test;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.util.UrlPathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CxfJaxwsEndpointTest {
    @Test
    void publishesPausesRestoresAndDestroysMatchingEndpointsWithoutOpeningSockets() {
        Bus bus = mock(Bus.class);
        EndpointCallback callback = mock(EndpointCallback.class);
        EndpointImpl endpoint = mock(EndpointImpl.class);
        List<Feature> features = new ArrayList<>();
        when(endpoint.getFeatures()).thenReturn(features);
        ServerImpl server = mock(ServerImpl.class);
        when(endpoint.getServer("/orders")).thenReturn(server);
        class TestTemplate extends EndpointApiTemplate {
            TestTemplate() { super(bus, callback); }
            @Override protected EndpointImpl newEndpoint(Object implementor) { return endpoint; }
        }
        TestTemplate template = new TestTemplate();
        Object service = new Object();
        assertSame(endpoint, template.publish("/orders", service));
        verify(callback).doCallback(service, endpoint);
        verify(endpoint).publish("/orders");
        assertSame(endpoint, template.getEndpoints().get("/orders"));
        assertEquals(List.of(endpoint), template.pause("/*", "maintenance"));
        assertInstanceOf(EndpointPauseFeature.class, features.get(0));
        assertEquals(1, template.pause("/orders", "second").size());
        assertEquals(1, features.size());
        assertEquals(List.of(endpoint), template.restore("/orders"));
        assertTrue(features.isEmpty());
        assertTrue(template.pause("/missing", "maintenance").isEmpty());
        template.destroy("/orders");
        verify(server).destroy();
        assertTrue(template.getEndpoints().isEmpty());
        assertSame(bus, template.getBus());
        Bus anotherBus = mock(Bus.class); template.setBus(anotherBus); assertSame(anotherBus, template.getBus());
        assertSame(callback, template.getCallback());
        EndpointCallback anotherCallback = mock(EndpointCallback.class);
        template.setCallback(anotherCallback); assertSame(anotherCallback, template.getCallback());
        AntPathMatcher matcher = new AntPathMatcher();
        template.setPathMatcher(matcher); assertSame(matcher, template.getPathMatcher());
        UrlPathHelper helper = new UrlPathHelper();
        template.setUrlPathHelper(helper); assertSame(helper, template.getUrlPathHelper());
        template.setEndpoints(Map.of("/other", endpoint));
        assertSame(endpoint, template.getEndpoints().get("/other"));
    }

    @Test
    void defaultCallbackAddsValidationAndLoggingFeatures() {
        LoggingFeature logging = new LoggingFeature();
        BeanValidationFeature validation = new BeanValidationFeature();
        DefaultEndpointCallback callback = new DefaultEndpointCallback(logging, validation);
        EndpointImpl endpoint = mock(EndpointImpl.class);
        List<org.apache.cxf.interceptor.Interceptor<? extends org.apache.cxf.message.Message>> inbound = new ArrayList<>();
        List<org.apache.cxf.interceptor.Interceptor<? extends org.apache.cxf.message.Message>> outbound = new ArrayList<>();
        List<Feature> features = new ArrayList<>();
        when(endpoint.getInInterceptors()).thenReturn(inbound);
        when(endpoint.getOutInterceptors()).thenReturn(outbound);
        when(endpoint.getFeatures()).thenReturn(features);
        assertSame(endpoint, callback.doCallback(new Object(), endpoint));
        assertEquals(List.of(logging, validation), features);
        assertEquals(1, inbound.size()); assertEquals(1, outbound.size());
    }
}
