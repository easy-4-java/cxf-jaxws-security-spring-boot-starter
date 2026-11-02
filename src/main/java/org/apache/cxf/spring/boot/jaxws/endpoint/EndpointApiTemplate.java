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
	 * 为指定的addr发布Endpoint
	 * @author <a href="https://github.com/loong10k">Loong Wan</a>
	 * @param addr  	   	：服务地址
	 * @param implementor  	：服务实现
	 * @return The Endpoint
	 */
	public Endpoint publish(String addr, Object implementor) {
		return this.publish(addr, implementor, callback);
	}

	/**
	 * 为指定的addr发布Endpoint
	 * @author <a href="https://github.com/loong10k">Loong Wan</a>
	 * @param addr  	   	：服务地址
	 * @param implementor  	：服务实现
	 * @param callback  	：回调函数
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
	 * 暂停服务
	 * @author <a href="https://github.com/loong10k">Loong Wan</a>
	 * @param pattern ：服务地址或表达式
	 * @param cause   ：暂停原因
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
	 * 恢复服务
	 * @author <a href="https://github.com/loong10k">Loong Wan</a>
	 * @param pattern ：服务地址或表达式
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
	 * 销毁指定路径匹配的Endpoint
	 * @author <a href="https://github.com/loong10k">Loong Wan</a>
	 * @param pattern ：服务地址或表达式
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

	public Bus getBus() {
		return bus;
	}

	public void setBus(Bus bus) {
		this.bus = bus;
	}

	public ConcurrentMap<String, Endpoint> getEndpoints() {
		return endpoints;
	}
	
	public void setEndpoints(Map<String, Endpoint> endpoints) {
		this.endpoints.putAll(endpoints);
	}

	public EndpointCallback getCallback() {
		return callback;
	}

	public void setCallback(EndpointCallback callback) {
		this.callback = callback;
	}

	public UrlPathHelper getUrlPathHelper() {
		return urlPathHelper;
	}

	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		this.urlPathHelper = urlPathHelper;
	}

	public PathMatcher getPathMatcher() {
		return pathMatcher;
	}

	public void setPathMatcher(PathMatcher pathMatcher) {
		this.pathMatcher = pathMatcher;
	}
	
}
