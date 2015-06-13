package automenta.netention.web;

import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.resource.FileResourceManager;

import java.io.File;

import static io.undertow.Handlers.header;
import static io.undertow.Handlers.resource;

/**
 * Created by me on 6/12/15.
 */
public class ClientResources {
    //https://github.com/undertow-io/undertow/blob/master/examples/src/main/java/io/undertow/examples/sessionhandling/SessionServer.java


    static final String defaultClientPath = "./src/web";

    public static HttpHandler handleClientResources() {
        return handleClientResources(defaultClientPath);
    }

    public static HttpHandler handleClientResources(String clientPath) {
        return header(resource(
                new FileResourceManager(new File(clientPath), 100, true, "/")).
                setDirectoryListingEnabled(false), "Access-Control-Allow-Origin", "*");
    }

}
