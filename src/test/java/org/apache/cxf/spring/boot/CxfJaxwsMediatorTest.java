package org.apache.cxf.spring.boot;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.ServerRegistry;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.spring.boot.jaxws.MediatorInInterceptor;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.MessageObserver;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CxfJaxwsMediatorTest {
    private static final String SOAP = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<soap:Body><m:request xmlns:m=\"%s\"/></soap:Body></soap:Envelope>";

    @Test
    void routesVersionedSoapBodyToMatchingServerAndAbortsOriginalChain() {
        Bus bus = mock(Bus.class);
        ServerRegistry registry = mock(ServerRegistry.class);
        Server v1 = server("local://version1");
        Server v2 = server("local://version2");
        when(bus.getExtension(ServerRegistry.class)).thenReturn(registry);
        when(registry.getServers()).thenReturn(List.of(v1, v2));
        SoapMessage incoming = message(String.format(SOAP, "urn:2007/03/21"));
        new MediatorInInterceptor(bus).handleMessage(incoming);
        verify(v2.getDestination().getMessageObserver()).onMessage(incoming);
        verify(incoming.getInterceptorChain()).abort();
        SoapMessage old = message(String.format(SOAP, "urn:legacy"));
        new MediatorInInterceptor(bus).handleMessage(old);
        verify(v1.getDestination().getMessageObserver()).onMessage(old);
        verify(old.getInterceptorChain()).abort();
    }

    @Test
    void doesNotRedirectWhenNoEndpointIsRegistered() {
        Bus bus = mock(Bus.class);
        ServerRegistry registry = mock(ServerRegistry.class);
        when(bus.getExtension(ServerRegistry.class)).thenReturn(registry);
        when(registry.getServers()).thenReturn(List.of());
        SoapMessage incoming = message(String.format(SOAP, "urn:legacy"));
        assertDoesNotThrow(() -> new MediatorInInterceptor(bus).handleMessage(incoming));
        verify(incoming.getInterceptorChain(), never()).abort();
    }

    private static SoapMessage message(String xml) {
        SoapMessage soap = mock(SoapMessage.class);
        when(soap.getContent(java.io.InputStream.class)).thenReturn(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        when(soap.get(Message.ENCODING)).thenReturn("UTF-8");
        when(soap.getInterceptorChain()).thenReturn(mock(InterceptorChain.class));
        return soap;
    }

    private static Server server(String address) {
        Server server = mock(Server.class, RETURNS_DEEP_STUBS);
        when(server.getEndpoint().getEndpointInfo().getAddress()).thenReturn(address);
        return server;
    }
}
