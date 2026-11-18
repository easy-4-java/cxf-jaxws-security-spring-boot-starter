package org.apache.cxf.spring.boot.jaxws.security;

import org.apache.cxf.binding.soap.SoapMessage;  
import org.apache.cxf.binding.soap.saaj.SAAJInInterceptor;  
import org.apache.cxf.interceptor.Fault;  
import org.apache.cxf.phase.AbstractPhaseInterceptor;  
import org.apache.cxf.phase.Phase;  
import org.slf4j.Logger;  
import org.slf4j.LoggerFactory;  
import org.w3c.dom.NodeList;  
  
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPHeader;
import jakarta.xml.soap.SOAPMessage;
  
public class UsernamePwdAuthInterceptor extends AbstractPhaseInterceptor<SoapMessage>{  
    
	private static final Logger LOG = LoggerFactory.getLogger(UsernamePwdAuthInterceptor.class);  
    private SAAJInInterceptor saa = new SAAJInInterceptor(); 
  
    private static final String USER_NAME = "admin";  
    private static final String USER_PASSWORD = "pass";  
  
    public UsernamePwdAuthInterceptor() {  
        super(Phase.PRE_PROTOCOL);  
        getAfter().add(SAAJInInterceptor.class.getName());  
    }  
  
    @Override  
    public void handleMessage(SoapMessage message) throws Fault {  
        SOAPMessage mess = message.getContent(SOAPMessage.class);  
        if (mess == null) {  
            saa.handleMessage(message);  
            mess = message.getContent(SOAPMessage.class);  
        }  
        SOAPHeader head = null;  
        try {  
            head = mess.getSOAPHeader();  
        } catch (Exception e) {  
            LOG.error("getSOAPHeader error: {}",e.getMessage(),e);  
        }  
        if (head == null) {  
            throw new Fault(new IllegalArgumentException("找不到Header，无法验证用户信息"));  
        }  
  
        NodeList users = head.getElementsByTagName(USER_NAME);  
        NodeList passwords = head.getElementsByTagName(USER_PASSWORD);  
        if (users.getLength() < 1) {  
            throw new Fault(new IllegalArgumentException("找不到用户信息"));  
        }  
        if (passwords.getLength() < 1) {  
            throw new Fault(new IllegalArgumentException("找不到密码信息"));  
        }  
  
        String userName = users.item(0).getTextContent().trim();  
        String password = passwords.item(0).getTextContent().trim();  
        if(USER_NAME.equals(userName) && USER_PASSWORD.equals(password)){  
            LOG.debug("admin auth success");  
        } else {  
            SOAPException soapExc = new SOAPException("认证错误");  
            LOG.debug("admin auth failed");  
            throw new Fault(soapExc);  
        }  
    }  
}  
