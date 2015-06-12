/**
 * HttpRequest - HTTP request container and parser
 * <p>
 * $Id: HttpRequest.java,v 1.2 2003/11/26 18:11:53 kangasha Exp $
 */
package automenta.netention.net.proxy.depr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;

public class HttpRequest implements Serializable {
    /** Help variables */
    final static String CRLF = "\r\n";

    /** Store the request parameters */
    String method;
    public URI uri;
    String version;
    String headers = "";
    /** Server and port */

    /** Create HttpRequest by reading it from the client socket */
    public HttpRequest(BufferedReader from) throws URISyntaxException {
        String firstLine = "";
        String[] tmp;
        try {
            firstLine = from.readLine();
        } catch (IOException e) {
            System.out.println("Error reading request line: " + e);
        }

        if (firstLine != null) {
            tmp = firstLine.split(" ");
            method = tmp[0];
            String URI = tmp[1];
            version = tmp[2];

            if (URI.startsWith("/")) URI = URI.substring(1);
            if (URI.startsWith("?")) URI = URI.substring(1);

            if (!method.equals("GET")) {
                System.out.println("Error: Method not GET");
            }

            String host;
            java.net.URI u = new java.net.URI(URI);
            this.uri = u;

            System.out.println("Request URI: " + u);



//            try {
//                String line = from.readLine();
//                if (line.length() != 0) {
//                    while (line.length() != 0) {
//                        headers += line + CRLF;
//                    /* We need to find host header to know which server to
//					 * contact in case the request URI is not complete. */
//                        if (line.startsWith("Host:")) {
//                            tmp = line.split(" ");
//                            if (tmp[1].indexOf(':') > 0) {
//                                String[] tmp2 = tmp[1].split(":");
//                                host = tmp2[0];
//                                port = Integer.parseInt(tmp2[1]);
//                            } else {
//                                host = tmp[1];
//                                port = HTTP_PORT;
//                            }
//                        }
//
//                        line = from.readLine();
//                    }
//                } else {
//                    if (tmp[1].indexOf(':') > 0) {
//                        String[] tmp2 = tmp[1].split(":");
//
//                        if (tmp2[1].indexOf(':') > 0) {
//                            String[] tmp3 = tmp2[1].split(":");
//                            URI = "/";
//                            host = tmp3[0].substring(2);
//                            port = Integer.parseInt(tmp3[1]);
//                        } else {
//                            URI = "/";
//                            host = tmp2[1].substring(2);
//                            port = HTTP_PORT;
//                        }
//                    } else {
//                        URI = "/";
//                        host = tmp[1];
//                        port = HTTP_PORT;
//                    }
//                    headers += "Host: " + host + CRLF;
//                    headers += "Port: " + port + CRLF;
//                }
//
//            } catch (IOException e) {
//                System.out.println("Error reading from socket: " + e);
//                return;
//            }

        }
    }

    /** Return host for which this request is intended */
    public String getHost() {
        return uri.getHost();
    }

    /** Return port for server */
    public int getPort() {

        int p = uri.getPort();
        if (p == -1)
            p = 80; //default http

        return p;
    }

    /**
     * Convert request into a string for easy re-sending.
     */
    public String toString() {
        String req = "";

        req = method + " " + uri + " " + version + CRLF;
        req += "Host: " + getHost() + CRLF;
        req += "Port: " + getPort() + CRLF;
		/* This proxy does not support persistent connections */
        req += "Connection: close" + CRLF;
        req += CRLF;

        return req;
    }
}