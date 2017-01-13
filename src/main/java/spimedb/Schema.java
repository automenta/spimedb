package spimedb;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.bytebuddy.ByteBuddy;
import org.apache.tinkerpop.gremlin.process.computer.ComputerResult;
import org.apache.tinkerpop.gremlin.process.computer.ranking.pagerank.PageRankVertexProgram;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.eclipse.collections.api.map.primitive.ObjectFloatMap;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.outE;

/**
 * Created by me on 1/8/17.
 */
public class Schema {

    final static Logger logger = LoggerFactory.getLogger(Schema.class);

//    @JsonIgnore
//    public final MutableGraph<String> inh = GraphBuilder.directed().allowsSelfLoops(false).expectedNodeCount(512).nodeOrder(ElementOrder.unordered()).build();

    final TinkerGraph inh = TinkerGraph.open();

//    @JsonIgnore
//    public final Map<String,Tag> tag = new ConcurrentHashMap<>();

    @JsonIgnore
    public final Map<String, Class> tagClasses = new ConcurrentHashMap<String, Class>();
    @JsonIgnore
    public final ClassLoader cl = ClassLoader.getSystemClassLoader();
    @JsonIgnore
    public final ByteBuddy tagProxyBuilder = new ByteBuddy();

    public Schema() {
    }

//    public Tag get(String id, boolean createIfUnknown) {
//        if (createIfUnknown) {
//            return tag.computeIfAbsent(id, Tag::new);
//        } else {
//            return tag.get(id);
//        }
//    }



    public void tag(String X, String[] parents) {

        int n = 0;
        Vertex[] ps = new Vertex[parents.length];
        for (String s : parents) {
            if (s.isEmpty()) { //HACK
                logger.warn("{} includes an empty string tag");
                continue;
            }
            ps[n++] = addVertex(s);
        }

        Vertex x = addVertex(X);
        for (Vertex p : ps) {
            if (p!=null) //HACK, for above condition
                p.addEdge("inh", x);
        }

    }

    //cluster:
    //System.out.println( inh.traversal().withComputer().V().peerPressure().by("cluster").valueMap().toList() );

    public ObjectFloatMap<String> rank() {


        ObjectFloatHashMap<String> hh = new ObjectFloatHashMap<>();
        try {
            ComputerResult x = inh.compute().program(PageRankVertexProgram.build().create(inh)).submit().get();
            x.graph().traversal().V().
                    forEachRemaining(pr -> {
                        String id = (String) pr.id();
                        float value = ((Number)pr.property("gremlin.pageRankVertexProgram.pageRank").value()).floatValue();
                        hh.put(id, value);
                    });
                    //forEachRemaining(x1 -> System.out.println(x1.id() + " " + Joiner.on(" ").join(x1.properties())));
        } catch (Exception e) {
            e.printStackTrace();
        }

        float min = hh.min();
        float max = hh.max();
        if (min!=max) {
            //normalize
            float range = max-min;
            ObjectFloatHashMap<String> ii = new ObjectFloatHashMap<>();
            hh.forEachKeyValue((k,v)->{
                ii.put(k, (v - min)/range);
            });
            return ii;
        } else {
            return hh;
        }

    }

    private Vertex addVertex(String s) {
//        Transaction t = inh.tx();
//        try {

        Iterator<Vertex> ex = inh.vertices(s);
        if (ex.hasNext()) {
            return ex.next();
        } else {
            return inh.addVertex(T.id, s);
        }
        //        } finally {
//            t.close();
//        }
    }

    /**
     * computes the set of subtree (children) tags held by the extension of the input (parent) tags
     *
     * @param parentTags if empty, searches all tags; otherwise searches the specified tags and all their subtags
     */
    public Stream<Vertex> tagsAndSubtags(@Nullable String... parentTags) {

        if (parentTags == null || parentTags.length == 0) {
            //return Iterators.transform(inh.vertices(), Element::label);
            return inh.traversal().V().toStream();
        } else {
            return inh.traversal().V(parentTags).repeat(outE("inh").otherV().dedup()).emit().toStream();
            //return inh.traversal().V(parentTags).outE("inh").otherV().tree().V().toStream();
        }
//
//
//        Set<String> s = new HashSet<>();
//        for (String x : parentTags) {
//            if (s.add(x)) {
//                inh.
//                s.addAll(inh.successors(x));
//            }
//        }
//        return s;
    }

    public Class[] resolve(String... tags) {
        Class[] c = new Class[tags.length];
        int i = 0;
        for (String s : tags) {
            Class x = tagClasses.get(s);
            if (x == null)
                throw new NullPointerException("missing class: " + s);
            c[i++] = x;
        }
        return c;
    }

    public Class the(String tagID, String... supertags) {

        synchronized (tagClasses) {
            if (tagClasses.containsKey(tagID))
                throw new RuntimeException(tagID + " class already defined");

            Class[] s = resolve(supertags);
            Class proxy = tagProxyBuilder.makeInterface(s).name("_" + tagID).make().load(cl).getLoaded();
            tagClasses.put(tagID, proxy);
            return proxy;
        }

        //.subclass(NObject.class).implement(s)
                /*.method(any())
                .intercept(MethodDelegation.to(MyInterceptor.class)
                        .andThen(SuperMethodCall.INSTANCE)
                        .defineField("myCustomField", Object.class, Visibility.PUBLIC)*/
                /*.make()
                .load(cl)
                .getLoaded();*/
    }

    public Stream<String> tags() {
        return inh.traversal().V().toStream().map(x -> (String)x.id());
    }

}
