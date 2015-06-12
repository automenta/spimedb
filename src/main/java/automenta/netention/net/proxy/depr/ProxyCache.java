/**
 * ProxyCache.java - Simple caching proxy
 *
 * $Id: ProxyCache.java,v 1.3 2004/02/16 15:22:00 kangasha Exp $
 *
 */

package automenta.netention.net.proxy.depr;
import java.net.*;
import java.io.*;
import java.util.*;

public class ProxyCache implements Runnable {
    /** Port for the proxy */
    private static int port;
    /** Socket for client connections */
    private static ServerSocket socket;
    private static Socket client = null;
    
    public static HashMap<URI, HttpResponse> cache = new HashMap();
	private boolean running;

	/** Create the ProxyCache object and the socket */
    public ProxyCache(int p) {
		port = p;
		try {
		    socket = new ServerSocket(port);
		} catch (IOException e) {
		    System.out.println("Error creating socket: " + e);
		    System.exit(-1);
		}
	}

	public void run() {
		running = true;
		while (running) {
			try {
				socket.setSoTimeout(8000);

				client = socket.accept();

				ProxyCacheHandler thread = new ProxyCacheHandler(client);
				new Thread(thread).start();

			} catch (IOException e) {
				System.out.println("Error reading request from client: " + e);
				/* Definitely cannot continue processing this request,
				 * so skip to next iteration of while loop. */
				continue;
			}
		}
	}

    /** Read command line arguments and start proxy */
    public static void main(String args[]) {
		int myPort = 8080;
		
		try {
		    myPort = Integer.parseInt(args[0]);
		} catch (Exception e) {
		}

		System.out.println(ProxyCache.class + " on port " + myPort);
		
		new ProxyCache(myPort).run();
	

    }
}

