package spimedb.web;

import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.resource.FileResourceManager;

import java.io.File;

import static io.undertow.Handlers.resource;

/**
 * Created by me on 6/12/15.
 */
public class ClientResources {
    //https://github.com/undertow-io/undertow/blob/master/examples/src/main/java/io/undertow/examples/sessionhandling/SessionServer.java


    static final String defaultClientPath = "./src/main/resources/public";

    public static HttpHandler handleClientResources() {
        return handleClientResources(defaultClientPath);
    }

    public static HttpHandler handleClientResources(String clientPath) {
        File base = new File(clientPath);

        return resource(

                new FileResourceManager(base, 0))

//                        new CachingResourceManager(
//                                16384,
//                                16*1024*1024,
//                                new DirectBufferCache(100, 10, 1000),
//                                new PathResourceManager(getResourcePath(), 0, true, true),
//                                0 //7 * 24 * 60 * 60 * 1000
//                        ))
                .setCachable((x) -> true)
                //.setDirectoryListingEnabled(true)
                .addWelcomeFiles("index.html")
        ;


//        return header(resource( new FileResourceManager(base, 100, true, "/") )
//                    .setWelcomeFiles("index.html")
//                    .setDirectoryListingEnabled(true), "Access-Control-Allow-Origin", "*");
    }

}
