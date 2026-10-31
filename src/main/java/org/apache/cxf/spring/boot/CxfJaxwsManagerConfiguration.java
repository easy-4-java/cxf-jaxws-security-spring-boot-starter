package org.apache.cxf.spring.boot;

import org.apache.cxf.Bus;
import org.apache.cxf.management.InstrumentationManager;
import org.apache.cxf.management.counters.CounterRepository;
import org.apache.cxf.management.jmx.InstrumentationManagerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//http://cxf.apache.org/docs/springboot.html

@Configuration
@ConditionalOnWebApplication
@ConditionalOnClass({ InstrumentationManager.class })
@ConditionalOnProperty(prefix = CxfJaxwsManagerProperties.PREFIX, value = "enabled", havingValue = "true")
@EnableConfigurationProperties({ CxfJaxwsManagerProperties.class })
public class CxfJaxwsManagerConfiguration {

	private static final Logger LOG = LoggerFactory.getLogger(CxfJaxwsManagerConfiguration.class);

	@Bean
	@ConditionalOnMissingBean(InstrumentationManagerImpl.class)
	public InstrumentationManager instrumentationManager(Bus bus, CxfJaxwsManagerProperties properties) {
		InstrumentationManagerImpl mgr = new InstrumentationManagerImpl();
		mgr.setBus(bus);
		mgr.setEnabled(properties.isEnabled());
		mgr.setServerName(properties.getServerName());
		mgr.setUsePlatformMBeanServer(properties.isUsePlatformMBeanServer());
	    return mgr;
	}

	@Bean(destroyMethod = "close")
	@ConditionalOnProperty(prefix = CxfJaxwsManagerProperties.PREFIX,
			name = "create-mb-server-connector-factory", havingValue = "true", matchIfMissing = true)
	public JmxConnectorServerAdapter jmxConnectorServer(InstrumentationManager manager,
			CxfJaxwsManagerProperties properties) throws java.io.IOException {
		return new JmxConnectorServerAdapter(manager.getMBeanServer(), properties);
	}
	
	// Wiring the counter repository 
	@Bean
	@ConditionalOnMissingBean(InstrumentationManagerImpl.class)
	public CounterRepository counterRepository(Bus bus){
		CounterRepository repository = new CounterRepository();
		repository.setBus(bus);
		return repository;
	}
	
}
