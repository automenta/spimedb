//package spimedb.server;
//
//import com.google.common.collect.Lists;
//import io.swagger.v3.oas.annotations.OpenAPI31;
//import io.swagger.v3.oas.annotations.Operation;
////import io.swagger.annotations.Api;
////import io.swagger.annotations.ApiOperation;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.ws.rs.*;
//import jakarta.ws.rs.core.Context;
//import jakarta.ws.rs.core.MediaType;
//import jakarta.ws.rs.core.Response;
//import jakarta.ws.rs.core.StreamingOutput;
//import jcog.bloom.StableBloomFilter;
//import jcog.bloom.hash.StringHasher;
//import jcog.random.XoRoShiRo128PlusRandom;
//import org.apache.lucene.facet.FacetResult;
//import org.apache.lucene.search.suggest.Lookup;
//import spimedb.NObject;
//import spimedb.SpimeDB;
//import spimedb.index.DObject;
//import spimedb.index.Search;
//import spimedb.query.CollectFacets;
//import spimedb.query.Query;
//import spimedb.util.JSON;
//
//
//import java.util.List;
//import java.util.concurrent.ThreadLocalRandom;
//
//import static spimedb.server.WebIO.*;
//
//
////@Server(description = "SpimeDB")
//@Path("/")
//@Produces({"application/json", "application/xml"})
//public class WebAPI {
//
//    /*
//    * swagger editor: http://editor.swagger.io/#/
//    */
//
////    final static Logger logger = LoggerFactory.getLogger(WebAPI.class);
//
//
//    private final SpimeDB db;
//
//    public WebAPI(SpimeDB db) {
//        this.db = db;
//    }
//
//    final static int SuggestLengthMax = 10;
//    final static int SuggestionResultsMax = 16;
//    final static int SearchResultsMax = 32;
//    final static int GeoResultsMax = 128;
//    final static int FacetResultsMax = 32;
//
//
//
//    @GET
//    @Path("/{I}/json")
//    @Produces({MediaType.APPLICATION_JSON})
//    @Operation(summary="Get by ID (JSON)")
//    public NObject get(@PathParam("I") String q) {
//        DObject n = db.get(q);
//        return (n != null) ? searchResult(n, searchResultFull) : null;
//    }
//
//    @GET
//    @Path("/{I}/icon")
//    @Produces({MediaType.MEDIA_TYPE_WILDCARD})
//    @Operation(summary="Get icon")
//    public Response getIcon(@PathParam("I") String q) {
//        return WebIO.send(db, q, NObject.ICON /* TODO: icon */);
//    }
//
//    @GET
//    @Path("/{I}/data")
//    @Produces({MediaType.MEDIA_TYPE_WILDCARD})
//    @Operation(summary="Get data")
//    public Response getData(@PathParam("I") String q) {
//        return WebIO.send(db, q, NObject.DATA);
//    }
//
//    /** TODO add PathParam version of this */
//    @GET
//    @Path("/suggest")
//    @Produces({MediaType.APPLICATION_JSON})
//    @Operation(summary="Provides search query suggestions given a partially complete input query")
//    public Response suggest(@QueryParam("q") String q) {
//
//        if (q == null || (q = q.trim()).isEmpty() || q.length() > SuggestLengthMax)
//            return Response.noContent().build();
//
//        String x = q;
//        return Response.ok((StreamingOutput) os -> {
//            JSON.toJSON(Lists.transform(
//                db.suggest(x, SuggestionResultsMax), y -> y.key), os);
//        }).build();
//    }
//
//    /** TODO add PathParam version of this */
//    @GET
//    @Path("/find")
//    @Produces({MediaType.APPLICATION_JSON})
//    @Operation(summary="Finds the results of a text search query")
//    public Response find(@QueryParam("q") String q) {
//
//        if (q == null || (q = q.trim()).isEmpty())
//            return Response.noContent().build();
//
//        //try {
//        Search r = new Query(q).limit(SearchResultsMax)
//                .start(db, new CollectFacets(SearchResultsMax/2));
//        return Response.ok((StreamingOutput) os -> WebIO.send(r, os, 0, searchResultFull)).build();
//        //}
////        catch (IOException e) {
////            return Response.serverError().build();
////        } catch (ParseException e) {
////            logger.error("parse {}", e);
////            return Response.status(Response.Status.BAD_REQUEST).build();
////        }
//    }
//
//    /** see: https://www.w3.org/DesignIssues/MatrixURIs.html
//     * TODO try to use ';' as a separator
//     * */
//    @GET
//    @Path("/earth/lonlat/rect/{lonMin}/{lonMax}/{latMin}/{latMax}/json")
//    @Produces({MediaType.APPLICATION_JSON})
//    @Operation(summary="Bounded longitude/latitude rectangle geoquery")
//    public Response earthLonLatRect(
//            @Context HttpServletRequest request,
//            @PathParam("lonMin") float lonMin,
//            @PathParam("lonMax") float lonMax,
//            @PathParam("latMin") float latMin,
//            @PathParam("latMax") float latMax) {
//
//
//        var sess = request.getSession(true);
//
//        //TODO use a unique 'window' id to uniquify each tab/widnow session in case there are multiple
//        //System.out.println(sess.getId() + " " + request.getRemotePort() + " " + sess);
//
//
//
//        Object _sentSTM = sess.getAttribute("sentSTM");
//        final StableBloomFilter<String> sentSTM;
//        int totalCells = 32 * 1024;
//        float unlearnCellsPerSecond = 2;
//        if (_sentSTM==null) {
//            sentSTM = new StableBloomFilter<>(totalCells, 3, 0, new XoRoShiRo128PlusRandom(), new StringHasher());
//            sess.setAttribute("sentSTM", sentSTM);
//        } else {
//            sentSTM = (StableBloomFilter<String>)_sentSTM;
//            long msSinceLastAccess = System.currentTimeMillis() - sess.getLastAccessedTime();
//            int unlearnedCells = Math.round((msSinceLastAccess/1000f) * unlearnCellsPerSecond);
//            if (unlearnedCells > 0)
//                sentSTM.forget(unlearnedCells, ThreadLocalRandom.current());
//        }
//
//        Object prevBounds = sess.getAttribute("earthLonLat");
//        if (prevBounds!=null) {
//
//        }
//        sess.setAttribute("earthLonLat", new double[] { lonMin, lonMax, latMin, latMax });
//
//
//        //TODO validate the lon/lat coords
//        //TODO filter by session's previously known requests
//        //TODO track all sessions in an attention model
//        Search r = new Query()
//                .where(lonMin, lonMax, latMin, latMax)
//                .limit(GeoResultsMax)
//                .start(db, new CollectFacets(SearchResultsMax/2));
//
//        return Response.ok((StreamingOutput) os -> WebIO.send(r, os, 0, searchResultSummary, sentSTM)).build();
//    }
//
//
//    @GET
//    @Path("/facet")
//    @Produces({MediaType.APPLICATION_JSON})
//    @Operation(summary="Finds matching search facets for a given dimension key")
//    public Response facet(@QueryParam("d") String dimension) {
//        if (!(dimension == null || (dimension = dimension.trim()).isEmpty())) {
//            FacetResult x = db.facets(dimension, FacetResultsMax);
//            if (x != null)
//                return Response.ok((StreamingOutput) os -> WebIO.stream(x, os)).build();
//        }
//
//        return Response.noContent().build();
//    }
//
//
////    @POST
////    @Path("/tell/json")
////    @Operation(summary="Input arbitrary JSON")
////    public Response tellJSON(@QueryParam("q") String dimension) {
////        //TODO @Context HttpServletRequest request,
//////            //POST only
//////            if (e.getRequestMethod().equals(HttpString.tryFromString("POST"))) {
//////                //System.out.println(e);
//////                //System.out.println(e.getRequestHeaders());
//////
//////                e.getRequestReceiver().receiveFullString((ex, s) -> {
//////                    JsonNode x = JSON.fromJSON(s);
//////                    if (x != null)
//////                        db.add(x);
//////                    else {
//////                        e.setStatusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY);
//////                    }
//////                });
//////
//////                e.endExchange();
//////            }
//////        });
////        return Response.ok().build();
////    }
//
//}
//
//
////from: https://dzone.com/articles/jax-rs-streaming-response
////package com.markandjim
////
////@Path("/subgraph")
////public class ExtractSubGraphResource {
////    private final GraphDatabaseService database;
////
////    public ExtractSubGraphResource(@Context GraphDatabaseService database) {
////        this.database = database;
////    }
////
////    @GET
////    @Produces(MediaType.TEXT_PLAIN)
////    @Path("/{nodeId}/{depth}")
////    public Response hello(@PathParam("nodeId") long nodeId, @PathParam("depth") int depth) {
////        Node node = database.getNodeById(nodeId);
////
////        final Traverser paths =  Traversal.description()
////                .depthFirst()
////                .relationships(DynamicRelationshipType.withName("whatever"))
////                .evaluator( Evaluators.toDepth(depth) )
////                .traverse(node);
////
////        StreamingOutput stream = new StreamingOutput() {
////            @Override
////            public void write(OutputStream os) throws IOException, WebApplicationException {
////                Writer writer = new BufferedWriter(new OutputStreamWriter(os));
////
////                for (org.neo4j.graphdb.Path path : paths) {
////                    writer.write(path.toString() + "\n");
////                }
////                writer.flush();
////            }
////        };
////
////        return Response.ok(stream).build();
////    }
//
//
/////**
//// * Created by me on 5/4/17.
//// */
////@Path("/test")
//////@Api(value = "/pet", description = "Operations about pets", authorizations = {
//////        @Authorization(value = "petstore_auth",
//////                scopes = {
//////                        @AuthorizationScope(scope = "write:pets", description = "modify pets in your account"),
//////                        @AuthorizationScope(scope = "read:pets", description = "read your pets")
//////                })
//////})
//////@Produces({"application/json", "application/xml"})
////
////public class ExampleJaxResource {
////    @GET
////    @Produces("text/plain")
////    public String get() {
////        return "hello world";
////    }
////}
