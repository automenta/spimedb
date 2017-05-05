package spimedb.server;

import io.swagger.annotations.Api;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.AuthorizationScope;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * Created by me on 5/4/17.
 */
@Path("/test")
//@Api(value = "/pet", description = "Operations about pets", authorizations = {
//        @Authorization(value = "petstore_auth",
//                scopes = {
//                        @AuthorizationScope(scope = "write:pets", description = "modify pets in your account"),
//                        @AuthorizationScope(scope = "read:pets", description = "read your pets")
//                })
//})
//@Produces({"application/json", "application/xml"})

public class ExampleJaxResource {
    @GET
    @Produces("text/plain")
    public String get() {
        return "hello world";
    }
}
