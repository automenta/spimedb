package com.kixeye.kixmpp.client;

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


/**
 * A configuration option for the {@link KixmppClient}.
 * 
 * @author ebahtijaragic
 */
public class KixmppClientOption<T> {
	public static final KixmppClientOption<String> DOMAIN_NAME = valueOf("XMPP_DOMAIN");
	public static final KixmppClientOption<Boolean> ENABLE_TLS = valueOf("ENABLE_TLS");
	public static final KixmppClientOption<Boolean> ENABLE_COMPRESSION = valueOf("ENABLE_COMPRESSION");

	private final String name;

	/**
	 * Creates an option with a name.
	 * 
	 * @param name
	 */
	protected KixmppClientOption(String name) {
		this.name = name;
	}

	/**
	 * Creates a {@link KixmppClientOption} with the given name.
	 * 
	 * @param name
	 * @return
	 */
	public static <T> KixmppClientOption<T> valueOf(String name) {
		return new KixmppClientOption<T>(name);
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		
		if (obj == null) {
			return false;
		}
		
		if (getClass() != obj.getClass()) {
			return false;
		}
		
		KixmppClientOption<?> other = (KixmppClientOption<?>) obj;
		
		if (name == null) {
			if (other.name != null){
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		
		return true;
	}
}
