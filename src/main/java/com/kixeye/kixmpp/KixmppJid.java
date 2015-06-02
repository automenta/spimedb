package com.kixeye.kixmpp;

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
 * A JID.
 * 
 * @author ebahtijaragic
 */
public class KixmppJid {
	private final String node;
	private final String domain;
	private final String resource;
	
	/**
	 * @param domain
	 */
	public KixmppJid(String domain) {
		this(null, domain, null);
	}
	
	/**
	 * @param node
	 * @param domain
	 */
	public KixmppJid(String node, String domain) {
		this(node, domain, null);
	}

	/**
	 * @param node
	 * @param domain
	 * @param resource
	 */
	public KixmppJid(String node, String domain, String resource) {
		assert domain != null && !domain.isEmpty() : "Argument 'domain' cannot be null or empty";

		this.node = node;
		this.domain = domain.toLowerCase();
		this.resource = resource;
	}
	
	/**
	 * Creates a {@link KixmppJid} from a raw jid.
	 * 
	 * @param jid
	 * @return
	 */
	public static KixmppJid fromRawJid(String jid) {
		String[] jidSplit = jid.split("/", 2);
		String[] domainSplit = jidSplit[0].split("@", 2);
		
		if (domainSplit.length == 1) {
			return new KixmppJid(null, domainSplit[0], jidSplit.length == 2 ? jidSplit[1] : null);
		} else {
			return new KixmppJid(domainSplit[0], domainSplit[1], jidSplit.length == 2 ? jidSplit[1] : null);
		}
	}

	/**
	 * Returns a clone of this JID with a different node.
	 * 
	 * @param node
	 * @return
	 */
	public KixmppJid withNode(String node) {
		return new KixmppJid(node, domain, resource);
	}

	/**
	 * Returns a clone of this JID with a different domain.
	 * 
	 * @param domain
	 * @return
	 */
	public KixmppJid withDomain(String domain) {
		return new KixmppJid(node, domain, resource);
	}

	/**
	 * Returns a clone of this JID with a different resource.
	 * 
	 * @param resource
	 * @return
	 */
	public KixmppJid withResource(String resource) {
		return new KixmppJid(node, domain, resource);
	}
	
	/**
	 * Returns a clone of this JID without the resource.
	 * 
	 * @return
	 */
	public KixmppJid withoutResource() {
		return new KixmppJid(node, domain);
	}

	/**
	 * @return the node
	 */
	public String getNode() {
		return node;
	}

	/**
	 * @return the domain
	 */
	public String getDomain() {
		return domain;
	}

	/**
	 * @return the resource
	 */
	public String getResource() {
		return resource;
	}
	
	/**
	 * Gets the base JID (node)@(domain).
	 * 
	 * @return
	 */
	public String getBaseJid() {
		if (node == null) {
			return domain;
		} else {
			return node + "@" + domain;
		}
	}
	
	/**
	 * Gets the full JID (node)@(domain)[/(resource)].
	 * 
	 * @return
	 */
	public String getFullJid() {
		if (resource == null) {
			return getBaseJid();
		} else {
			return node + "@" + domain + "/" + resource;
		}
	}
	
	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return getFullJid();
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((domain == null) ? 0 : domain.hashCode());
		result = prime * result + ((node == null) ? 0 : node.hashCode());
		result = prime * result
				+ ((resource == null) ? 0 : resource.hashCode());
		return result;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		KixmppJid other = (KixmppJid) obj;
		if (domain == null) {
			if (other.domain != null)
				return false;
		} else if (!domain.equals(other.domain))
			return false;
		if (node == null) {
			if (other.node != null)
				return false;
		} else if (!node.equals(other.node))
			return false;
		if (resource == null) {
			if (other.resource != null)
				return false;
		} else if (!resource.equals(other.resource))
			return false;
		return true;
	}
}
