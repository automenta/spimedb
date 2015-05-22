/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.knowtention.auth;

/**
 *
 * @author me
 */
public class MozillaPersona {
    
}
///*
// * Copyright (C) 2014 me
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//package netention.web.auth;
//
//import info.modprobe.browserid.BrowserIDResponse;
//import info.modprobe.browserid.Verifier;
//import java.net.MalformedURLException;
//import java.net.URL;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import netention.web.Session;
//import netention.web.WebServer;
//import org.vertx.java.core.Handler;
//import org.vertx.java.core.http.HttpServerRequest;
//
///**
// *
// * @see https://github.com/mozilla/browserid-cookbook/tree/master/java/spring
// * @see
// * https://github.com/user454322/browserid-verifier/blob/master/sample/src/main/java/info/modprobe/browserid/sample/servlet/In.java
// */
//public class MozillaPersonaHandler implements Handler<HttpServerRequest> {
//    private final WebServer web;
//    private final String host;
//    private final int port;
//    
//
//    public MozillaPersonaHandler(String host, int port, WebServer server) {
//        this.web = server;        
//        this.host = host;        
//        this.port = port;        
//    }
//
//    
//    @Override
//    public void handle(HttpServerRequest req) {
//
//        req.expectMultiPart(true);
//        
//        req.endHandler(new Handler() {
//
//            @Override
//            public void handle(Object e) {
//                URL url = null;
//                try {
//                    url = req.absoluteURI().toURL();
//                } catch (MalformedURLException ex) {
//                    Logger.getLogger(MozillaPersonaHandler.class.getName()).log(Level.SEVERE, null, ex);
//                }
//                StringBuilder urlBuilder = new StringBuilder()
//                        .append(url.getProtocol()).append("://").append(host)
//                        .append(':').append(port);
//
//                final String audience = urlBuilder.toString();
//                final String assertion = req.formAttributes().get("assertion");
//                final Verifier verifier = new Verifier();
//                final BrowserIDResponse personaResponse = verifier.verify(assertion, audience);
//                BrowserIDResponse.Status status = personaResponse.getStatus();
//                
//                if (status == BrowserIDResponse.Status.OK) {
//                    /* Authentication with Persona was successful */
//                    String email = personaResponse.getEmail();
//                    //log.info("{} has sucessfully signed in", email);
//
//                    web.startSession(req, email, new Handler<Session>() {
//                        @Override public void handle(Session e) {
//                            req.response().end("Mozilla Persona Authentication OK: " + email);
//                        }                 
//                    });
//
//                } else {
//                    /* Authentication with Persona failed */
//                    req.response().setStatusCode(403);
//                    req.response().end("Mozilla Persona Authentication Fail: {}" +  personaResponse.getReason());
//                }
//
//            }
//            
//        });
//    }
//
//}
