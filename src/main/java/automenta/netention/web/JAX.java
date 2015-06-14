package automenta.netention.web;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.servlet.api.ServletInfo;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.spi.ResteasyDeployment;

import javax.servlet.ServletException;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by me on 6/14/15.
 */

public class JAX extends PathHandler {

    private final Set<Class<?>> services = new HashSet();

    public final ServletContainer container = ServletContainer.Factory.newInstance();
    public Undertow server;

    public JAX() {}

    public DeploymentInfo undertowDeployment(ResteasyDeployment deployment, String mapping)
    {
        if (mapping == null) {
            mapping = "/";
        }
        if (!mapping.startsWith("/")) {
            mapping = "/" + mapping;
        }
        if (!mapping.endsWith("/")) {
            mapping = mapping + "/";
        }
        mapping = mapping + "*";
        String prefix = null;
        if (!mapping.equals("/*")) {
            prefix = mapping.substring(0, mapping.length() - 2);
        }
        ServletInfo resteasyServlet = Servlets.servlet("ResteasyServlet", HttpServlet30Dispatcher.class).setAsyncSupported(true).setLoadOnStartup(Integer.valueOf(1)).addMapping(mapping);
        if (prefix != null) {
            resteasyServlet.addInitParam("resteasy.servlet.mapping.prefix", prefix);
        }
        return new DeploymentInfo().addServletContextAttribute(ResteasyDeployment.class.getName(), deployment).addServlet(resteasyServlet);
    }

    public DeploymentInfo undertowDeployment(ResteasyDeployment deployment)
    {
        return undertowDeployment(deployment, "/");
    }

    public DeploymentInfo undertowDeployment(Class<? extends Application> application, String mapping)
    {
        ResteasyDeployment deployment = new ResteasyDeployment();
        deployment.setApplicationClass(application.getName());
        DeploymentInfo di = undertowDeployment(deployment, mapping);
        di.setClassLoader(application.getClassLoader());
        return di;
    }

    public DeploymentInfo undertowDeployment(Class<? extends Application> application)
    {
        ApplicationPath appPath = (ApplicationPath)application.getAnnotation(ApplicationPath.class);
        String path = "/";
        if (appPath != null) {
            path = appPath.value();
        }
        return undertowDeployment(application, path);
    }

    public JAX deploy(ResteasyDeployment deployment)
    {
        return deploy(deployment, "/");
    }

    public JAX deploy(ResteasyDeployment deployment, String contextPath)
    {
        if (contextPath == null) {
            contextPath = "/";
        }
        if (!contextPath.startsWith("/")) {
            contextPath = "/" + contextPath;
        }
        DeploymentInfo builder = undertowDeployment(deployment);
        builder.setContextPath(contextPath);
        builder.setDeploymentName("Resteasy" + contextPath);
        return deploy(builder);
    }

    public JAX deploy(Class<? extends Application> application)
    {
        ApplicationPath appPath = (ApplicationPath)application.getAnnotation(ApplicationPath.class);
        String path = "/";
        if (appPath != null) {
            path = appPath.value();
        }
        return deploy(application, path);
    }

    public JAX deploy(Class<? extends Application> application, String contextPath)
    {
        if (contextPath == null) {
            contextPath = "/";
        }
        if (!contextPath.startsWith("/")) {
            contextPath = "/" + contextPath;
        }
        ResteasyDeployment deployment = new ResteasyDeployment();
        deployment.setApplicationClass(application.getName());
        DeploymentInfo di = undertowDeployment(deployment);
        di.setClassLoader(application.getClassLoader());
        di.setContextPath(contextPath);
        di.setDeploymentName("Resteasy" + contextPath);
        return deploy(di);
    }

    public JAX deploy(Application application)
    {
        ApplicationPath appPath = (ApplicationPath)application.getClass().getAnnotation(ApplicationPath.class);
        String path = "/";
        if (appPath != null) {
            path = appPath.value();
        }
        return deploy(application, path);
    }

    public JAX deploy(Application application, String contextPath)
    {
        if (contextPath == null) {
            contextPath = "/";
        }
        if (!contextPath.startsWith("/")) {
            contextPath = "/" + contextPath;
        }
        ResteasyDeployment deployment = new ResteasyDeployment();
        deployment.setApplication(application);
        DeploymentInfo di = undertowDeployment(deployment);
        di.setClassLoader(application.getClass().getClassLoader());
        di.setContextPath(contextPath);
        di.setDeploymentName("Resteasy" + contextPath);
        return deploy(di);
    }

    public JAX deploy(DeploymentInfo builder)
    {
        DeploymentManager manager = this.container.addDeployment(builder);
        manager.deploy();
        try
        {
            addPath(builder.getContextPath(), manager.start());
        }
        catch (ServletException e)
        {
            throw new RuntimeException(e);
        }
        return this;
    }

    public JAX add(String path, HttpHandler ph) {
        addPrefixPath(path, ph);
        return this;
    }

    public JAX add(Class<?> service) {
        services.add(service);
        return this;
    }

    public JAX start(Undertow.Builder builder)     {
        deploy(new Application() {
            @Override public Set<Class<?>> getClasses() {
                return services;
            }
        }, "/api");
        this.server = builder.setHandler(this).build();
        this.server.start();
        return this;
    }



    public void stop()
    {
        this.server.stop();
    }

    public JAX start(String host, int port) {

            return start( Undertow.builder()
                    .addHttpListener(8080, "localhost")
                    .setIoThreads(4) );

    }
}

