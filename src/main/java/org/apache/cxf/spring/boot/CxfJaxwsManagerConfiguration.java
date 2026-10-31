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

/**
 * Auto-configuration for the CXF JMX management integration.
 *
 * <p>Activated when the {@link InstrumentationManager} class is on the classpath and
 * {@code cxf.manager.enabled=true}. It registers an {@link InstrumentationManagerImpl}
 * bound to the CXF bus and a {@link CounterRepository} that gathers performance
 * counters exposed via JMX.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
@Configuration
@ConditionalOnWebApplication
@ConditionalOnClass({ InstrumentationManager.class })
@ConditionalOnProperty(prefix = CxfJaxwsManagerProperties.PREFIX, value = "enabled", havingValue = "true")
@EnableConfigurationProperties({ CxfJaxwsManagerProperties.class })
/**
 * <p>Auto-configuration for CxfJaxwsManagerConfiguration.</p>
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
public class CxfJaxwsManagerConfiguration {

	private static final Logger LOG = LoggerFactory.getLogger(CxfJaxwsManagerConfiguration.class);

	/**
	 * Create the {@link InstrumentationManagerImpl} configured from the bound
	 * properties, exposing CXF MBeans through JMX.
	 * @param bus the CXF bus
	 * @param properties the JMX management properties
	 * @return a configured instrumentation manager
	 */
	@Bean
	@ConditionalOnMissingBean(InstrumentationManagerImpl.class)
	/**
	 * <p>Instrumentation manager.</p>
	 * @param bus
	 * @param properties
	 * @return the result
	 */
	public InstrumentationManager instrumentationManager(Bus bus, CxfJaxwsManagerProperties properties) {
		InstrumentationManagerImpl mgr = new InstrumentationManagerImpl();
		mgr.setBus(bus);
		mgr.setEnabled(properties.isEnabled());
	    return mgr;
	}

	/**
	 * Create the {@link CounterRepository} wired to the CXF bus, used to track
	 * performance counters for the published endpoints.
	 * @param bus the CXF bus
	 * @return a configured counter repository
	 */
	@Bean
	@ConditionalOnMissingBean(CounterRepository.class)
	/**
	 * <p>Counter repository.</p>
	 * @param bus
	 * @return the result
	 */
	public CounterRepository counterRepository(Bus bus){
		CounterRepository repository = new CounterRepository();
		repository.setBus(bus);
		return repository;
	}

}
