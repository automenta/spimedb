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


import com.google.common.util.concurrent.ListenableFuture;
import com.kixeye.kixmpp.client.module.presence.Presence;
import com.kixeye.kixmpp.client.module.presence.PresenceKixmppClientModule;
import com.kixeye.kixmpp.server.KixmppServer;
import io.netty.handler.ssl.SslContext;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.junit.*;

import java.io.*;
import java.net.ServerSocket;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Tests the {@link KixmppClient}
 * 
 * @author ebahtijaragic
 */
public class KixmppClientTest {
	static private String domain;
	static private String username;
	static private String password;
	static private String resource;
	static private int port;

	static KixmppServer server;

	
	static {

		domain = UUID.randomUUID().toString().replace("-", "");
		username = UUID.randomUUID().toString().replace("-", "");
		password = UUID.randomUUID().toString().replace("-", "");
		resource = UUID.randomUUID().toString().replace("-", "");

		//new Thread( () ->
		{
			try {
				ServerSocket socketServer = new ServerSocket();
				socketServer.bind(null);

				port = socketServer.getLocalPort();
				socketServer.close();

				server = new KixmppServer("localhost", port, domain);

				ListenableFuture<KixmppServer> x = server.start();

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// ).start();


//        StorageProviderRegistry providerRegistry = new MemoryStorageProviderRegistry();
//
//        final Entity adminJID = EntityImpl.parseUnchecked(username + "@" + domain);
//        final AccountManagement accountManagement = (AccountManagement) providerRegistry .retrieve(AccountManagement.class);
//
//        if (!accountManagement.verifyAccountExists(adminJID)) {
//            accountManagement.addUser(adminJID, password);
//        }
//
//        TCPEndpoint tcpEndpoint = new TCPEndpoint();
//        tcpEndpoint.setPort(port);
//
//        try (InputStream certStream = this.getClass().getResourceAsStream("/bogus_mina_tls.cert")) {
//			server = new XMPPServer(domain);
//	        server.addEndpoint(tcpEndpoint);
//	        server.setStorageProviderRegistry(providerRegistry);
//	        server.setTLSCertificateInfo(certStream, "boguspw");
//
//	        server.start();
//
//	        server.addModule(new SoftwareVersionModule());
//	        server.addModule(new EntityTimeModule());
//	        server.addModule(new XmppPingModule());
//	        server.addModule(new InBandRegistrationModule());
//	        server.addModule(new AdhocCommandsModule());
//	        final ServiceAdministrationModule serviceAdministrationModule = new ServiceAdministrationModule();
//	        // unless admin user account with a secure password is added, this will be not become effective
//	        serviceAdministrationModule.setAddAdminJIDs(Arrays.asList(adminJID));
//	        server.addModule(serviceAdministrationModule);
//        }
	}
	
//	@After
//	public void tearDown() {
//        server.stop();
//	}
	
	@Test @Ignore
	public void testSimpleSSLConnect() throws Exception {
		try (KixmppClient client = new KixmppClient(createSslContext(), KixmppClient.Type.TCP)) {
			Assert.assertNotNull(client.connect("localhost", port, domain).get(2, TimeUnit.SECONDS));
			Assert.assertNotNull(client.login(username, password, resource).get(2, TimeUnit.SECONDS));
			client.module(PresenceKixmppClientModule.class).updatePresence(new Presence());
		}
	}

	@Test
	public void testSimpleConnect() throws Exception {
		try (KixmppClient client = new KixmppClient()) {
			Assert.assertNotNull(client.connect("localhost", port, domain).get(6, TimeUnit.SECONDS));
			Assert.assertNotNull(client.login(username, password, resource).get(6, TimeUnit.SECONDS));
			client.module(PresenceKixmppClientModule.class).updatePresence(new Presence());
		}
	}

	@Ignore
	private SslContext createSslContext() throws Exception {
		Certificate cert;
	    
		try (InputStream certStream = this.getClass().getResourceAsStream("/bogus_mina_tls.cert")) {
			KeyStore ks = KeyStore.getInstance("JKS");
		    ks.load(certStream, "boguspw".toCharArray());
		    cert = ks.getCertificate("bogus");
		}
	    
		File certFile = File.createTempFile(UUID.randomUUID().toString().replace("-", ""), null);
		FileOutputStream certFileOutputStream = new FileOutputStream(certFile);
		IOUtils.copy(new StringReader("-----BEGIN CERTIFICATE-----\n"), certFileOutputStream);
		IOUtils.copy(new ByteArrayInputStream(Base64.encodeBase64(cert.getEncoded())), certFileOutputStream);
		IOUtils.copy(new StringReader("\n-----END CERTIFICATE-----"), certFileOutputStream);
		certFileOutputStream.close();
		
		return SslContext.newClientContext(certFile);
	}
}
