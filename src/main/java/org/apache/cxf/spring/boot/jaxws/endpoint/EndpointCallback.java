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

import jakarta.xml.ws.Endpoint;

import org.apache.cxf.jaxws.EndpointImpl;

/**
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
public interface EndpointCallback {

	/**
	 * Endpoint 初始化 后的回调
	 * @author <a href="https://github.com/loong10k">Loong Wan</a>
	 * @param implementor  	：服务实现
	 * @param endpoint 	   	：{@link Endpoint} instance
	 * @return The Endpoint
	 */
	Endpoint doCallback(Object implementor, EndpointImpl endpoint);
	
}
