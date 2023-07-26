package spimedb.server;


import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

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
