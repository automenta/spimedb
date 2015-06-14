package automenta.netention;

import io.swagger.jaxrs.config.BeanConfig;
import io.undertow.servlet.api.DeploymentInfo;
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.jboss.resteasy.test.TestPortProvider;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 * http://docs.jboss.org/resteasy/docs/3.0.4.Final/userguide/html/RESTEasy_Embedded_Container.html
 */
public class JaxExample {

    private static UndertowJaxrsServer server;

    @Path("/test")
    public static class Resource
    {
        @GET
        @Produces("text/plain")
        public String get()
        {
            return "hello world";
        }
    }

    @ApplicationPath("/base")
    public static class MyApp extends Application
    {


        public MyApp() {
            super();

            BeanConfig beanConfig = new BeanConfig();
            beanConfig.setVersion("1.0.2");
            beanConfig.setSchemes(new String[]{"http"});
            beanConfig.setHost("localhost:8081");
            beanConfig.setBasePath("/base");
            beanConfig.setResourcePackage("io.swagger.resources");


            beanConfig.setScan(true);

        }

        @Override
        public Set<Class<?>> getClasses()
        {
            HashSet<Class<?>> classes = new HashSet<Class<?>>();

            classes.add(Resource.class);

            classes.add(io.swagger.jaxrs.listing.ApiListingResource.class);
            classes.add(io.swagger.jaxrs.listing.SwaggerSerializers.class);

            return classes;
        }
    }



    @BeforeClass
    public static void init() throws Exception
    {
        server = new UndertowJaxrsServer().start();
    }

    @AfterClass
    public static void stop() throws Exception
    {
        server.stop();
    }

    @Test
    public void testApplicationPath() throws Exception
    {
        server.deploy(MyApp.class);
        Client client = ClientBuilder.newClient();
        String val = client.target(TestPortProvider.generateURL("/base/test"))
                .request().get(String.class);

        assertEquals("hello world", val);
        client.close();
    }

    @Test
    public void testApplicationContext() throws Exception
    {
        server.deploy(MyApp.class, "/root");
        Client client = ClientBuilder.newClient();
        String val = client.target(TestPortProvider.generateURL("/root/test"))
                .request().get(String.class);
        assertEquals("hello world", val);
        client.close();
    }

    @Test
    public void testDeploymentInfo() throws Exception
    {
        DeploymentInfo di = server.undertowDeployment(MyApp.class);
        di.setContextPath("/di");
        di.setDeploymentName("DI");
        server.deploy(di);
        Client client = ClientBuilder.newClient();
        String val = client.target(TestPortProvider.generateURL("/di/base/test"))
                .request().get(String.class);
        assertEquals("hello world", val);
        client.close();
    }
}

