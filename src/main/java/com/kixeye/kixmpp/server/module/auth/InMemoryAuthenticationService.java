package com.kixeye.kixmpp.server.module.auth;

/*
 * #%L
 * KIXMPP
 * %%
 * Copyright (C) 2014 KIXEYE, Inc
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.kixeye.kixmpp.server.KixmppServer;
import io.netty.util.concurrent.Promise;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authenticates users based on an in-memory map.
 * 
 * @author ebahtijaragic
 */
public class InMemoryAuthenticationService implements AuthenticationService {

	private KixmppServer server;
	private Map<String, String> users = new ConcurrentHashMap<>();

	public InMemoryAuthenticationService(KixmppServer server) {
		this.server = server;
	}
	
	/**
	 * Adds a user.
	 */
	public void addUser(String username, String password) {
		users.put(username.toLowerCase(), password);
	}
	
	/**
	 * Removes a user.
	 */
	public void removeUser(String username) {
		users.remove(username);
	}
	
	/**
	 * @see com.kixeye.kixmpp.server.module.auth.AuthenticationService#authenticate(java.lang.String, java.lang.String)
	 */
	public Promise<Boolean> authenticate(String username, String password) {
		Promise<Boolean> promise = server.createPromise();
		promise.setSuccess(password.equals(users.get(username.toLowerCase())));
		return promise;
	}
}
