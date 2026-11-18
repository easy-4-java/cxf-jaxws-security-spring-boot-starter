package org.apache.cxf.spring.boot;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import jakarta.jws.WebService;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.endpoint.annotation.WebEndpoint;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.spring.boot.jaxws.callback.DefaultEndpointCallback;
import org.apache.cxf.spring.boot.jaxws.endpoint.EndpointApiTemplate;
import org.apache.cxf.spring.boot.jaxws.endpoint.EndpointCallback;
import org.apache.cxf.spring.boot.jaxws.property.LoggingFeatureProperty;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.apache.cxf.validation.BeanValidationFeature;
import org.apache.cxf.validation.BeanValidationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ObjectUtils;

//http://cxf.apache.org/docs/springboot.html

@AutoConfigureAfter(name = { "org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration" })
@Configuration
@ConditionalOnWebApplication
@ConditionalOnClass({ SpringBus.class, CXFServlet.class })
@ConditionalOnProperty(prefix = CxfJaxwsProperties.PREFIX, value = "enabled", havingValue = "true")
@EnableConfigurationProperties({ CxfJaxwsProperties.class })
public class CxfJaxwsAutoConfiguration implements ApplicationContextAware {

	private static final Logger LOG = LoggerFactory.getLogger(CxfJaxwsAutoConfiguration.class);
	private ApplicationContext applicationContext;

	@Bean(name = Bus.DEFAULT_BUS_ID)
	@ConditionalOnMissingBean(Bus.class)
	public SpringBus bus() {
		SpringBus bus = new SpringBus();
		BusFactory.setDefaultBus(bus);
		return bus;
	}

	@Bean
	@ConditionalOnMissingBean(BeanValidationProvider.class)
	public BeanValidationProvider validationProvider() {
		return new BeanValidationProvider();
	}

	@Bean
	public BeanValidationFeature validationFeature(BeanValidationProvider validationProvider) {
		BeanValidationFeature feature = new BeanValidationFeature();
		feature.setProvider(validationProvider);
		return feature;
	}
	
	@Bean
	public LoggingFeature loggingFeature(CxfJaxwsProperties properties) {
		
		LoggingFeatureProperty property = properties.getLoggingFeature();

		LoggingFeature feature = new LoggingFeature();
		feature.setInMemThreshold(property.getThreshold());
		feature.setLimit(property.getLimit());
		feature.setLogBinary(property.isLogBinary());
		feature.setLogMultipart(property.isLogMultipart());
		feature.setPrettyLogging(property.isPrettyLogging());
		feature.setVerbose(property.isVerbose());
		
		return feature;
	}
	
	/*@Bean
	@ConditionalOnMissingBean(MetricsProvider.class)
	public MetricsProvider metricsProvider(Bus bus) {
		return new CodahaleMetricsProvider(bus);
	}

	@Bean
	public MetricsFeature metricsFeature(MetricsProvider metricsProvider) {
		return new MetricsFeature(metricsProvider);
	}*/
	
	@Bean
	@ConditionalOnMissingBean(EndpointCallback.class)
	public EndpointCallback endpointCallback(
			LoggingFeature loggingFeature,
			BeanValidationFeature validationFeature) {
		return new DefaultEndpointCallback(loggingFeature, validationFeature);
	}
	
	@Bean
	public EndpointApiTemplate endpointTemplate(Bus bus,
			EndpointCallback endpointCallback,
			LoggingFeature loggingFeature,
			BeanValidationFeature validationFeature) {

		EndpointApiTemplate template = new EndpointApiTemplate(bus, endpointCallback);
		
		// 动态创建、发布 Ws
		Map<String, Object> beansOfType = getApplicationContext().getBeansWithAnnotation(WebService.class);
		if (!ObjectUtils.isEmpty(beansOfType)) {

			Iterator<Entry<String, Object>> ite = beansOfType.entrySet().iterator();
			while (ite.hasNext()) {
				Entry<String, Object> entry = ite.next();	
				// 查找该实现上的自定义注解
				WebEndpoint annotationType = getApplicationContext().findAnnotationOnBean(entry.getKey(),
						WebEndpoint.class);
				if (annotationType == null) {
					// 注解为空，则跳过该实现，并打印错误信息
					LOG.error("Not Found AnnotationType {0} on Bean {1} Whith Name {2}", WebEndpoint.class,
							entry.getValue().getClass(), entry.getKey());
					continue;
				}
				template.publish(annotationType.addr(), entry.getValue());
			}
		}

		return template;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}

}
