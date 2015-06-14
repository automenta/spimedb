package automenta.netention;

import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.jboss.resteasy.test.TestPortProvider;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import static org.junit.Assert.assertEquals;

/**
 * Created by me on 6/14/15.
 */
public class JaxExampleMain  {


    public static void main(String[] args) {


        //port 8081 test default
        UndertowJaxrsServer server = new UndertowJaxrsServer().start();

        //http://localhost:8081/base/test
        server.deploy(JaxExample.MyApp.class);
        Client client = ClientBuilder.newClient();
        String val = client.target(TestPortProvider.generateURL("/base/test"))
                .request().get(String.class);

        assertEquals("hello world", val);
        client.close();
    }

}
