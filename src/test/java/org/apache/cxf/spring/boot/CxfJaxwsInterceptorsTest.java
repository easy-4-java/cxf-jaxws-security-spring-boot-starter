package org.apache.cxf.spring.boot;

import jakarta.xml.soap.SOAPHeader;
import jakarta.xml.soap.SOAPMessage;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.headers.Header;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.spring.boot.jaxws.feature.EndpointPauseFeature;
import org.apache.cxf.spring.boot.jaxws.feature.EndpointPauseInterceptor;
import org.apache.cxf.spring.boot.jaxws.security.ClientLoginInterceptor;
import org.apache.cxf.spring.boot.jaxws.security.ClientTokenInInterceptor;
import org.apache.cxf.spring.boot.jaxws.security.UsernamePwdAuthInterceptor;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CxfJaxwsInterceptorsTest {
    @Test
    void rejectsMissingAndEmptyClientTokensButAcceptsPresentToken() {
        ClientTokenInInterceptor interceptor = new ClientTokenInInterceptor("token", Phase.PRE_PROTOCOL, false);
        SoapMessage soap = mock(SoapMessage.class);
        Exchange exchange = mock(Exchange.class);
        MessageImpl request = new MessageImpl();
        InterceptorChain chain = mock(InterceptorChain.class);
        when(soap.getExchange()).thenReturn(exchange);
        when(exchange.getInMessage()).thenReturn(request);
        when(soap.getInterceptorChain()).thenReturn(chain);
        request.put(Message.PROTOCOL_HEADERS, Map.of());
        interceptor.handleMessage(soap);
        request.put(Message.PROTOCOL_HEADERS, Map.of("token", List.of("")));
        interceptor.handleMessage(soap);
        verify(chain, times(2)).abort();
        request.put(Message.PROTOCOL_HEADERS, Map.of("token", List.of("valid")));
        interceptor.handleMessage(soap);
        verifyNoMoreInteractions(chain);
    }

    @Test
    void addsLoginHeaderAndPausesEndpoints() {
        SoapMessage soap = mock(SoapMessage.class);
        List<Header> headers = new ArrayList<>();
        when(soap.getHeaders()).thenReturn(headers);
        new ClientLoginInterceptor("alice", "secret").handleMessage(soap);
        assertEquals(1, headers.size());
        Element auth = (Element) headers.get(0).getObject();
        assertEquals("alice", auth.getElementsByTagName("username").item(0).getTextContent());
        assertEquals("secret", auth.getElementsByTagName("password").item(0).getTextContent());
        RuntimeException paused = assertThrows(RuntimeException.class,
                () -> new EndpointPauseInterceptor("maintenance").handleMessage(soap));
        assertTrue(paused.getMessage().contains("maintenance"));
        InterceptorProvider provider = mock(InterceptorProvider.class);
        when(provider.getInInterceptors()).thenReturn(new ArrayList<>());
        class Feature extends EndpointPauseFeature {
            Feature() { super("maintenance"); }
            void install(InterceptorProvider p) { initializeProvider(p, null); }
        }
        Feature feature = new Feature();
        feature.install(provider);
        assertEquals(1, provider.getInInterceptors().size());
        assertInstanceOf(EndpointPauseInterceptor.class, provider.getInInterceptors().get(0));
    }

    @Test
    void validatesSoapHeaderCredentials() throws Exception {
        UsernamePwdAuthInterceptor interceptor = new UsernamePwdAuthInterceptor();
        SoapMessage soap = mock(SoapMessage.class);
        SOAPMessage saaj = mock(SOAPMessage.class);
        SOAPHeader header = mock(SOAPHeader.class);
        when(soap.getContent(SOAPMessage.class)).thenReturn(saaj);
        when(saaj.getSOAPHeader()).thenReturn(header);
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element root = document.createElement("header");
        document.appendChild(root);
        when(header.getElementsByTagName("admin")).thenAnswer(i -> root.getElementsByTagName("admin"));
        when(header.getElementsByTagName("pass")).thenAnswer(i -> root.getElementsByTagName("pass"));
        assertThrows(Fault.class, () -> interceptor.handleMessage(soap));
        Element user = document.createElement("admin"); user.setTextContent("admin"); root.appendChild(user);
        assertThrows(Fault.class, () -> interceptor.handleMessage(soap));
        Element password = document.createElement("pass"); password.setTextContent("pass"); root.appendChild(password);
        assertDoesNotThrow(() -> interceptor.handleMessage(soap));
        password.setTextContent("wrong");
        assertThrows(Fault.class, () -> interceptor.handleMessage(soap));
    }

    @Test
    void rejectsSoapMessagesWithoutAReadableHeader() throws Exception {
        UsernamePwdAuthInterceptor interceptor = new UsernamePwdAuthInterceptor();
        SoapMessage soap = mock(SoapMessage.class);
        SOAPMessage saaj = mock(SOAPMessage.class);
        when(soap.getContent(SOAPMessage.class)).thenReturn(saaj);
        when(saaj.getSOAPHeader()).thenReturn(null);
        assertThrows(Fault.class, () -> interceptor.handleMessage(soap));
        when(saaj.getSOAPHeader()).thenThrow(new jakarta.xml.soap.SOAPException("malformed header"));
        assertThrows(Fault.class, () -> interceptor.handleMessage(soap));
    }
}
