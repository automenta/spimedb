package automenta.netention.net.proxy.depr;

import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.HashMap;

// Does work for socket connections in a new thread.
public class ProxyCacheHandler implements Runnable {
    HashMap<URI, HttpResponse> cache = ProxyCache.cache;
    Socket client = null;

    public ProxyCacheHandler(Socket c) {
        client = c;
    }

    @Override
    public void run() {
        Socket server = null;
        HttpRequest request = null;
        HttpResponse response = null;

		/* Process request. If there are any exceptions, then simply
		 * return and end this request. This unfortunately means the
		 * client will hang for a while, until it timeouts. */
	
		/* Read request */
        try {
            BufferedReader fromClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
            request = new HttpRequest(fromClient);
        } catch (Exception e) {
            System.out.println("Error reading request from client: " + e);
            return;
        }

        response = cache.get(request.uri);

		/* Send request to server */
        if (response != null) {
            try {


                DataOutputStream toClient = new DataOutputStream(client.getOutputStream());
			    /* Fill in */
			    /* Write response to client. First headers, then body */
                toClient.write(response.toString().getBytes());
                toClient.write(response.body);
                toClient.flush();

            } catch (IOException e) {
                System.out.println("Error writing cached response to client: " + e);
            }
        } else {
            try {
			    /* Open socket and write request to socket */
                server = new Socket(request.getHost(), request.getPort());
                DataOutputStream toServer = new DataOutputStream(server.getOutputStream());
                toServer.write(request.toString().getBytes());
                toServer.flush();
                toServer.close();
            } catch (UnknownHostException e) {
                System.out.println("Unknown host: " + request.getHost());
                System.out.println(e);
                return;
            } catch (IOException e) {
                System.out.println("Error writing request to server: " + e);
                return;
            }

            cache.put(request.uri, response);

			/* Read response and forward it to client */
            try {
                DataInputStream fromServer = new DataInputStream(server.getInputStream());
                response = new HttpResponse(fromServer);
                DataOutputStream toClient = new DataOutputStream(client.getOutputStream());
			    /* Fill in */

                System.out.println("Providing client with: " + response.body.length + " bytes");
			    /* Write response to client. First headers, then body */
                toClient.write(response.toString().getBytes());
                toClient.write(response.body);
                toClient.flush();
                toClient.close();
			    /* Insert object into the cache */
			    /* Fill in (optional exercise only) */
            } catch (IOException e) {
                System.out.println("Error writing response to client: " + e);
            }
        }

    }
}