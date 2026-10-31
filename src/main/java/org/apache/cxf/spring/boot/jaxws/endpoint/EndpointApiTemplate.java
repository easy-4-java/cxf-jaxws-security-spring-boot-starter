/*
 * Copyright (c) 2018, hiwepy (https://github.com/hiwepy).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.cxf.spring.boot.jaxws.endpoint;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;

import jakarta.xml.ws.Endpoint;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.ServerImpl;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.spring.boot.jaxws.feature.EndpointPauseFeature;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.util.UrlPathHelper;

/**
 * TODO
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
public class EndpointApiTemplate {

	private ConcurrentMap<String, Endpoint> endpoints = new ConcurrentHashMap<String, Endpoint>();
	private Bus bus;
	private EndpointCallback callback;
	/** 路径解析工具 */
	private UrlPathHelper urlPathHelper = new UrlPathHelper();
	/** 路径规则匹配工具 */
	private PathMatcher pathMatcher = new AntPathMatcher();
	
	public EndpointApiTemplate(Bus bus, EndpointCallback callback) {
		this.bus = bus;
		this.callback = callback;
	}

	/**
	 * addrEndpoint
	 * @author <a href="https://github.com/loong10k">Loong Wan</a>
	 * @param addr ：service
	 * @param implementor ：serviceimplementation
	 * @return The Endpoint
	 */
	public Endpoint publish(String addr, Object implementor) {
		return this.publish(addr, implementor, callback);
	}

	/**
	 * addrEndpoint
	 * @author <a href="https://github.com/loong10k">Loong Wan</a>
	 * @param addr ：service
	 * @param implementor ：serviceimplementation
	 * @param callback ：
	 * @return The Endpoint
	 */
	public Endpoint publish(String addr, Object implementor, EndpointCallback callback) {

		EndpointImpl endpoint = newEndpoint(implementor);

		callback.doCallback(implementor, endpoint);

		// 接口发布在 addr 目录下
		endpoint.publish(addr);
		
		endpoints.put(addr, endpoint);

		return endpoint;
	}

	protected EndpointImpl newEndpoint(Object implementor) {
		return new EndpointImpl(bus, implementor);
	}
	
	/**
	 * service
	 * @author <a href="https://github.com/loong10k">Loong Wan</a>
	 * @param pattern ：serviceor
	 * @param cause ：
	 * @return The Endpoint paused
	 */
	public List<Endpoint> pause(String pattern, String cause) {
		List<Endpoint> pauses = new ArrayList<>();
		for (String addr : endpoints.keySet()) {
			if (pathMatcher.match(pattern, addr)) {
				EndpointImpl endpoint = (EndpointImpl) endpoints.get(addr);
				if(null != endpoint) {
					
					endpoint.getFeatures().removeIf(new Predicate<Feature>() {

						@Override
						/**
						 * <p>Test.</p>
						 * @param t
						 * @return the result
						 */
						public boolean test(Feature t) {
							return EndpointPauseFeature.class.isAssignableFrom(t.getClass());
						}
					});
					endpoint.getFeatures().add(0, new EndpointPauseFeature(cause));
					pauses.add(endpoint);
				}
			}
		}
		return pauses;
	}
	
	/**
	 * service
	 * @author <a href="https://github.com/loong10k">Loong Wan</a>
	 * @param pattern ：serviceor
	 * @return The Endpoint restored
	 */
	public List<Endpoint> restore(String pattern) {
		List<Endpoint> pauses = new ArrayList<>();
		for (String addr : endpoints.keySet()) {
			if (pathMatcher.match(pattern, addr)) {
				EndpointImpl endpoint = (EndpointImpl) endpoints.get(addr);
				if(null != endpoint) {
					
					endpoint.getFeatures().removeIf(new Predicate<Feature>() {

						@Override
						/**
						 * <p>Test.</p>
						 * @param t
						 * @return the result
						 */
						public boolean test(Feature t) {
							return EndpointPauseFeature.class.isAssignableFrom(t.getClass());
						}
					});
					
					pauses.add(endpoint);
				}
			}
		}
		return pauses;
	}

	/**
	 * Endpoint
	 * @author <a href="https://github.com/loong10k">Loong Wan</a>
	 * @param pattern ：serviceor
	 */
	public void destroy(String pattern) {
		Iterator<Map.Entry<String, Endpoint>> ite = endpoints.entrySet().iterator();
        while(ite.hasNext()){
            Map.Entry<String, Endpoint> entry = ite.next();
            if (pathMatcher.match(pattern, entry.getKey())) {
            	EndpointImpl endpoint = (EndpointImpl) endpoints.get(entry.getKey());
				if (endpoint != null) {
					ServerImpl server = endpoint.getServer(entry.getKey());
					server.destroy();
					ite.remove();  
				}
            }
        }
	}

	/** Returns the bus.
	 * @return the result
	 */
	public Bus getBus() {
		return bus;
	}

	/** Sets the bus.
	 * @param bus the bus
	 */
	public void setBus(Bus bus) {
		this.bus = bus;
	}

	/** Returns the endpoints.
	 * @return the result
	 */
	public ConcurrentMap<String, Endpoint> getEndpoints() {
		return endpoints;
	}
	
	/** Sets the endpoints.
	 * @param endpoints the endpoints
	 */
	public void setEndpoints(Map<String, Endpoint> endpoints) {
		this.endpoints.putAll(endpoints);
	}

	/** Returns the callback.
	 * @return the result
	 */
	public EndpointCallback getCallback() {
		return callback;
	}

	/** Sets the callback.
	 * @param callback the callback
	 */
	public void setCallback(EndpointCallback callback) {
		this.callback = callback;
	}

	/** Returns the url path helper.
	 * @return the result
	 */
	public UrlPathHelper getUrlPathHelper() {
		return urlPathHelper;
	}

	/** Sets the url path helper.
	 * @param urlPathHelper the urlPathHelper
	 */
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		this.urlPathHelper = urlPathHelper;
	}

	/** Returns the path matcher.
	 * @return the result
	 */
	public PathMatcher getPathMatcher() {
		return pathMatcher;
	}

	/** Sets the path matcher.
	 * @param pathMatcher the pathMatcher
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		this.pathMatcher = pathMatcher;
	}
	
}
